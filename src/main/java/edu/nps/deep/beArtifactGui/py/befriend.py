#!/usr/bin/env python3

# Program: befriend.py
# Author: Michael McCarrin <mrmccarr@nps.edu>
# Description: Produce graphs from bulk_extractor features.

import networkx as nx
import sys
import os
import argparse
import timeit
import collections
from concurrent.futures import ProcessPoolExecutor, as_completed
from os.path import join, basename, normpath

def ingest_feature(be_dir, feature_file):
  list_dict = {}
  aliases = {}
  email_file = join(be_dir, feature_file)

  with open(email_file, "rb") as f:
    for l in f:
      try:
        line = l.decode("utf-8")
      except:
        print("Line contains invalid utf-8; skipping")

      if line[0] == "#":
        continue
     
      raw_offset, raw_alias, remainder = line.split("\t", 2)

      alias  = raw_alias.replace("\\x00","")
      aliases[alias] = {'context': remainder}

      if raw_offset.isdigit():
        root   = "top"
        offset = int(raw_offset)
      else:
        complex_offset = raw_offset.split("-")
        root   = "-".join(complex_offset[:-1])
        offset = int(complex_offset[-1])

      if root not in list_dict:
        list_dict[root] = [(offset, alias)]
      else:
        list_dict[root].append((offset,alias))

    for l in list_dict.values():
      l.sort()

  return aliases, list_dict

def nx_weighted_edge_list(edge_dict):
  w_edges = []
  for p, w in edge_dict.items():
    w_edges.append((p[0], p[1], {"weight":w}))
  return w_edges

def fixed_find_edges(feature_data, win_size=256):
  node_q = collections.deque()
  edges  = collections.defaultdict(int)

  for l in feature_data.values():
    node_q.clear()
    for offset, alias in l:
      while (node_q and (node_q[0][0] < offset - win_size)):
        node_q.popleft()

      for n in node_q:
        pair = tuple(sorted([alias, n[1]]))
        if pair[0] != pair[1]:
          edges[pair] += 1

      node_q.append((offset, alias))

  weighted_edges = nx_weighted_edge_list(edges)

  return edges, weighted_edges

def stretchy_find_edges(feature_data, win_size=256):
  node_q = collections.deque()
  edges  = collections.defaultdict(int)

  for feature_list in feature_data.values():
    node_q.clear()
    for offset, alias in feature_list:
      while (node_q and (node_q[-1][0] < offset - win_size)):
        node_q.popleft()

      for n in node_q:
        pair = tuple(sorted([alias, n[1]]))
        if pair[0] != pair[1]:
          edges[pair] += 1

      node_q.append((offset, alias))

  weighted_edges = nx_weighted_edge_list(edges)

  return edges, weighted_edges


def make_gephi(V, E):
  G = nx.Graph()
  G.add_nodes_from(V)
  G.add_edges_from(E)
  return G

def find_big_connected_components(G, num=5):
  n = nx.number_of_nodes(G)
  con_comps  = list(sorted(nx.connected_component_subgraphs(G), key=len, reverse=True))
  return con_comps[:num]

class Summary:
  def __init__(self, path):
    self.lines = []
    self.path  = path 
    
  def print_last_n(self, n=0):
    index = 0 if n == 0 else len(self.lines) - n
    for l in self.lines[index:]: print(l)

  def write(self):
    with open(self.path, "w") as f:
      for l in self.lines: print(l, file=f)

  def add(self, line):
    self.lines.append(line)

def get_dirs(dir_list):
  dirs = []
  with open(dir_list, "r") as d:
    for line in d:
      dirs.append(line.strip().rstrip(os.sep))
  return dirs

def process_many(in_dirs, num_jobs, outdir, args):
  out_dirs  = [join(outdir, basename(subdir)) for subdir in in_dirs]
  arguments = zip(in_dirs, out_dirs, [args]*num_jobs, [True]*num_jobs)
  with ProcessPoolExecutor() as executor:
    executor.map(process_one, arguments)
 

def process_one(arguments):   
  be_dir, out_dir, args, multi_mode = arguments
  bar      = "="*80
  win_size = args.win_size 
  loners = args.keep_loners
  big_subs = args.big_subgraphs
  feature_file = args.feature_file

  if args.fixed_window:
    find_edges = fixed_find_edges
  else:
    find_edges = stretchy_find_edges

  gname    = basename(normpath(be_dir)) + "_" + str(win_size) + ".gexf"
  sumfile  = basename(normpath(be_dir)) + "_" + str(win_size) + "_summary.txt"

  if not os.path.exists(out_dir):
    os.makedirs(out_dir)

  sumpath  = join(out_dir, sumfile)
  gpath    = join(out_dir, gname)
  summary  = Summary(sumpath)
  summary.add("BEtographer summary for command line invocation:")

  summary.add(" ".join(sys.argv))
  summary.add(bar)
  summary.add("Using Window Size: {}".format(win_size))
  summary.add("Writing graphs to: {}".format(gpath))
  summary.add(bar)
  if not multi_mode: summary.print_last_n()
  
  start             = timeit.default_timer()
  nodes, feature_data = ingest_feature(be_dir, feature_file)
  ingest_time       = timeit.default_timer()

  summary.add("Ingested in {} seconds".format(ingest_time - start))
  if not multi_mode: summary.print_last_n(1)

  edges, weighted_edges = find_edges(feature_data, win_size)
  edge_time             = timeit.default_timer()

  summary.add("All neighbors linked in {} seconds".format(edge_time - ingest_time))
  if not multi_mode: summary.print_last_n(1)
  if not loners:
    friends = {}
    for e in edges.keys():
      friends[e[0]] = nodes[e[0]]
      friends[e[1]] = nodes[e[1]]
    nodes = friends

  G = make_gephi(nodes.items(),weighted_edges)
  nx.write_gexf(G, gpath) 
  end = timeit.default_timer()

  summary.add("Finished in {} seconds".format(end - start))
  if not multi_mode: summary.print_last_n(1)
  summary.add(bar)
  summary.add("Summary Statistics for Whole Graph:")
  summary.add(bar)
  summary.add(nx.info(G))

  if big_subs:
    subgraph_start = timeit.default_timer()
    i = 1
    summary.add("Looking for the top {} subgraphs".format(big_subs))
    if not multi_mode: summary.print_last_n(1)

    major_subs = find_big_connected_components(G, big_subs)
    subgraph_end = timeit.default_timer()
    subgraph_time = subgraph_end - subgraph_start
    
    summary.add("Found {} big subgraphs in {} seconds.".format(len(major_subs), subgraph_time))
    if not multi_mode: summary.print_last_n(1)

    for g in major_subs:
      summary.add(bar)
      summary.add("Summary Statistics for subgraph {}:".format(i))
      summary.add(bar)
      summary.add(nx.info(g))
      gname = basename(normpath(be_dir)) + "_" + str(win_size) + "sub" + str(i) + ".gexf"
      out   = join(out_dir, gname)
      nx.write_gexf(g, out)
      i += 1

  summary.write()

  if multi_mode:
    print(".", end="")
    sys.stdout.flush()
  else:
    print("Check {} for a summary of the results.".format(sumpath))

if __name__=="__main__":
  parser = argparse.ArgumentParser(description='Discover networks in bulk'
                                               ' extractor email.txt output.')

  group = parser.add_mutually_exclusive_group(required=True)

  group.add_argument('be_results_path', nargs='?', 
                     help="Path to bulk_extractor output files. Required unless"
                          " running with the -p option.")

  parser.add_argument('-o', '--output-path', required=True,
                      help="Sets the directory where befriend should store its"
                           " output (required).")

  parser.add_argument('-w', '--win-size', type=int, default=128, 
                      help='Optionally sets the window size in bytes. Default is 128.')

  parser.add_argument('-f', '--feature-file', default="email.txt", 
                      help='Sets the bulk_extractor feature file to be used for '
                           'analysis. The default is email.txt. Any other value is '
                           'experimental.')

  parser.add_argument('-F', '--fixed_window', action='store_true', default=False,
                      help='Only link addresses found within the distance'
                           ' defined by win_size. This was the old default'
                           ' behavior. This is probably not what you want, so'
                           ' it is not recommended to use this option unless you'
                           ' want to reproduce legacy behavior.') 

  parser.add_argument('-k', '--keep-loners', action='store_true', default=False,
                      help='Keep all nodes, even those with no edges to other nodes.') 

  parser.add_argument('-m', '--big-subgraphs', metavar='N', type=int, default=10,
                      help='Save a separate gexf file for the top N largest'
                           ' connected components of the graph. Default is 10.')

  group.add_argument('-p', '--parallel', dest="dir_list", 
                      help='Specify name of a file containing a list of bulk'
                           ' extractor output directories and process these'
                           ' in parallel. An output directory will be created'
                           ' for each as a subdirectory of the output path.')

  args     = parser.parse_args()

  if args.dir_list:
    multi_start = timeit.default_timer()
    in_dirs   = get_dirs(args.dir_list) 
    num_jobs  = len(in_dirs)
    process_many(in_dirs, num_jobs, args.output_path, args)
    very_end = timeit.default_timer()
    print()
    print("Completed {} jobs in {} seconds.".format(num_jobs, very_end - multi_start))
  else:
    process_one((args.be_results_path, args.output_path, args, False))