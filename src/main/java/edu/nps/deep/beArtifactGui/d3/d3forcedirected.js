// Define the namespace
var fdNameSpace = fdNameSpace || {};

// This inits the component
fdNameSpace.ForceDirected = function(element) {

  // Getter and setter for the value property
  this.getValue = function()
  {
    return null; // not used
  };
  
  this.setValue = function(value)
  {
    this.doit(value)
  };

  // build the graph based on the argument: [3]:width,height,json
  this.doit = function(value)
  {
	var svg = d3.select("svg");
	
	var width = value[0];
	var height= value[1];
	var mygraph = value[2];
	
	svg.attr("width",width);
	svg.attr("height",height);

	var mycolors = d3.scaleOrdinal(d3.schemeCategory20);
    for(i=0;i<20;i++)
    	  console.log(mycolors(i));
	var simulation = d3.forceSimulation()
	    .force("link", d3.forceLink()
	    		.id(function(d) { return d.id; })
	    		.distance(function(d){  return 30;/*d.value;*/})) //default=30
	    .force("charge", d3.forceManyBody())
	    .force("center", d3.forceCenter(width / 2, height / 2));

	var link = svg.append("g")
	    .attr("class", "links")
	    .selectAll("line")
	    .data(mygraph.links)
	    .enter().append("line")
	    .attr("stroke-width", function(d) { return /*Math.sqrt(*/d.value/*)*/; });

	var node = svg.append("g")
	    .attr("class", "nodes")
	    .selectAll("circle")
	    .data(mygraph.nodes)
	    .enter().append("circle")
	      .attr("r", function(d) {return d.diam;})
	      .attr("fill", function(d) { return mycolors(d.group); })
	      .call(d3.drag()
	          .on("start", dragstarted)
	          .on("drag", dragged)
	          .on("end", dragended));

	node.append("title")
	    .text(function(d) { return d.id; });

    simulation
        .nodes(mygraph.nodes)
	    .on("tick", ticked);

    simulation.force("link")
         .links(mygraph.links);

	function ticked()
	{
      link
	    .attr("x1", function(d) { return d.source.x; })
	    .attr("y1", function(d) { return d.source.y; })
	    .attr("x2", function(d) { return d.target.x; })
	    .attr("y2", function(d) { return d.target.y; });

	  node
	    .attr("cx", function(d) { return d.x; })
	    .attr("cy", function(d) { return d.y; });
	}

	function dragstarted(d)
	{
	  if (!d3.event.active)
		simulation.alphaTarget(0.3).restart();
	  d.fx = d.x;
	  d.fy = d.y;
	}

	function dragged(d) {
	  d.fx = d3.event.x;
	  d.fy = d3.event.y;
	}

	function dragended(d) {
	  if (!d3.event.active) simulation.alphaTarget(0);
	  d.fx = null;
	  d.fy = null;
	}	
  }
};

