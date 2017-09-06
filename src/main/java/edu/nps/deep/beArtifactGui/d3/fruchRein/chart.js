// Define the namespace
var fdNameSpace = fdNameSpace || {};

/* do the following to allow time to enable JS debugger on spawned window
setTimeout(function(){
	debugger;
},6000);
*/

// This inits the component
fdNameSpace.FruchtermanReingold = function(element) {
  // component state set from server
  var componentValue;

  this.getValue = function()
  {
    return componentValue;
  };
  
  this.setValue = function(value)
  {
	componentValue = value;

	if(componentValue["command"]) {
	  if(!componentValue["data"] && Object.keys(data).length === 0 && data.constructor === Object) {
		  alert("No data to graph");
		  return;
	  }
	}
	else {
	  console.log("log no command");
	  return;  //no command
	}

	switch(componentValue["command"]) {
      case "layout":	    
        dataobj = componentValue["data"];

	    var len = Object.keys(dataobj.nodes).length;
	    if(len > 500) {
		  alert("Can't graph greater than 500 nodes.  This graph has "+len);
		  return;
	    }
	    oldDocReady(this);
	    break;
	    
	  case "layoutagain":
	  case "stoplayout":
	    //alert("not implemented!");
		  oldDocReady(this); //try it
	}
  };

  var outer_width  = $('#container').width();     //2200
  var outer_height = $('#container').height();    //2200
  var chart_width  = $('svg#chart').width();      //1386
  var chart_height = $('svg#chart').height();     //1386

  //***************************
  // Graph parameters, set to defaults
  // match to FruchtermanReingoldState.java
  var autoAreaP = false;
  var areaP = chart_width * chart_height / 8;   //240124.5
  var speedP = 0.1;
  var gravityP = 0.75;
  var kcoreP = 0;
  var data = {};
  //***************************
  
  function initNodes(locdata, type) {
    for (var key in locdata) {
      locdata[key].id = key;
      locdata[key].name = key.split('_').join(' ');
      locdata[key].type = type;
      if (!locdata[key].requires)
    	    locdata[key].requires = [];
      locdata[key].children = [];
      locdata[key].childrenweights = [];
      locdata[key].weight = 0;
      locdata[key].row = 1;
      locdata[key].depth = 0;
      locdata[key].size = 2;
      locdata[key].walked = -1;
      locdata[key].rowwalked = -1;
      locdata[key].linkweights = [];
    }
  }

  function arrangeNodes(locdata) {
    // create tree structure
    for (var key in locdata) {
    	  var dnuts = locdata[key]; //test
      for (var i = 0; i < locdata[key].requires.length; i++) {
        var parent = locdata[key].requires[i];
        if (locdata[parent]) {
          var pnuts = locdata[parent];
          locdata[parent].children.push(locdata[key]);
          locdata[parent].childrenweights.push(locdata[key].linkweights[i]);
        }
      }
    }

    var byWeight = function(l, r) {
        return l.weight - r.weight;
    };

    var roots = [];
    for (var key in locdata) {
        if (locdata[key].requires.length == 0) {
            roots.push(locdata[key]);
        }
    }
    // figure out the depth and weight of each node
    var depth = 0;
    var walk_depth_weight = function(node) {
        if(node.walked != -1) 
        	  return 0;
        else
        	  node.walked = 1;
        var weight = 1;
        if (node.depth < depth) {
            node.depth = depth;
        }
        depth++;
        for (var i = 0; i < node.children.length; i++) {
            weight += walk_depth_weight(node.children[i]);
        }
        depth--;
        node.weight = weight;
        return weight;
    }
    
    for (var i = 0; i < roots.length; i++) {
        depth = 0;
        walk_depth_weight(roots[i]);
    }
    // now figure out which row to place them
    roots.sort(byWeight);

    row = 2;
    var walk_row = function(node) {
    	   if(node.rowwalked != -1) 
      	  return;
      else
      	  node.rowwalked = 1;
    	   
        node.row = row;
        row++;
        node.children.sort(byWeight);
        for (var i = 0; i < node.children.length; i++) {
            walk_row(node.children[i]);
        }
    }
    for (var i = 0; i < roots.length; i++) {
        walk_row(roots[i]);
    }
    return roots;
  }

  var drawPath = function(d) {
    var svg = d3.select('svg#path');
    var lineage = {};
    lineage.nodes = [];
    var trace = function(node) {
        lineage.nodes.push(node);
        for (var parent in node.requires) {
            if (data[node.requires[parent]].depth == node.depth - 1) {
                trace(data[node.requires[parent]]);
                break;
            }
        }
    }
    trace(d);
    lineage.links = [];
    for (var i=0; i<lineage.nodes.length-1; i++)
    {
        lineage.links.push({
            source: lineage.nodes[i],
            target: lineage.nodes[i+1],
        });
    }

    var xOffset = 15;
    var yOffset = 15;
    var ySpacing = 50;
    svg.attr('width', '100%');
    svg.attr('height', ySpacing * d.depth + yOffset * 2);

    svg.selectAll(".link").remove();
    svg.selectAll(".link").data(lineage.links).enter().append("line")
        .attr("class", "link")
        .attr("x1", function(d) {
            return xOffset;
        })
        .attr("y1", function(d) {
            return d.source.depth * ySpacing + yOffset;
        })
        .attr("x2", function(d) {
            return xOffset;
        })
        .attr("y2", function(d) {
            return d.target.depth * ySpacing + yOffset;
        });

    svg.selectAll(".node").remove();
    svg.selectAll(".node").data(lineage.nodes).enter().append("circle")
        .attr('r', function(d) {
            return 10;
        })
        .attr("class", function(d) {
            return "node " + d.type + ' ' + (d.selected?'selected':'');
        })
        .attr("cx", function(d) {
            return xOffset;
        })
        .attr("cy", function(d) {
            return d.depth * ySpacing + yOffset;
        })
        .attr("id", function(d) {
            return d.id;
        });

    svg.selectAll(".text").remove();
    svg.selectAll(".text").data(lineage.nodes).enter().append("text")
        .attr("class", function(d) {
            return "text " + d.type + ' ' + (d.selected?'selected':'');
        })
        .text(function(d) {
            return d.name;
        })
        .attr("x", function(d) {
            return xOffset + 25;
        })
        .attr("y", function(d) {
            var bbox = this.getBBox();
            return d.depth * ySpacing + yOffset + 5;
        });
}

var showInfo = function(d) {
    $('#dialog').hide();
    $('#dialog').slideDown(duration = 200);
    // $('#info > h3').html(d.name);
    // $('#info > p').html(d.description);
    $('#info > table').html('');
    if (d.cost) {
        $('#info > table')
            .append(
                '<tr>\
                <th class="resource">Costs</th>\
                <th>R</th>\
                <th>V</th>\
                <th>C</th>\
                <th>L</th>\
                </tr>'
            );
    }
    for (var resource in d.cost) {
        var row = $('<tr></tr>');
        row.append('<td class="resource">' + resource.split('_').join(' ') + '</td>');
        for (var difficulty in d.cost[resource])
            row.append('<td>' + d.cost[resource][difficulty] + '</td>');
        row.appendTo('#info > table');
    }
    $('#info > p').html(d.description);
  }

  var onNodeClick = function(d) {
    d.selected = true;
    drawPath(d);
    showInfo(d);
    d.selected = false;
  }

  var darkR = 191;
  var darkG = 171;
  var darkB = 110;
  var lightR = 255;
  var lightG = 246;
  var lightB = 198;
  var spanR = lightR-darkR;
  var spanG = lightG-darkG;
  var spanB = lightB-darkB;
  
  var weightSum = 0;
  var weightNum = 0;
  var weightMax = 0;
  var weightMin = Number.MAX_SAFE_INTEGER;
  var weightSpan = 0;

  var maxLinkWidth = 7;
  var minLinkWidth = 2;
  var linkWidthSpan = maxLinkWidth-minLinkWidth;

  var weightScaler = function(w) {
	weightSum+=w;
	weightNum++;
	if(w > weightMax)
	  weightMax = w;
	if(w < weightMin)
	  weightMin = w;
  }
  
  var calcLinkWidth = function(w) {
	if(weightSpan == 0)
		return (maxLinkWidth-minLinkWidth) / 2;
	return minLinkWidth + (linkWidthSpan * (w-weightMin) / weightSpan);
  }
  
  var calcLinkColor = function(w) {
	 if(weightSpan == 0)
		return"rgb("+darkR+","+darkG+","+darkB+");";
	 
	 var wdiff = w-weightMin;
	 var rr= lightR - (spanR*wdiff/weightSpan);
     var gg= (lightG-(spanG * wdiff / weightSpan));
     var bb= (lightB-(spanB * wdiff / weightSpan));
     
     return "rgb("+rr+","+gg+","+bb+");";
  }
  
  //$(document).ready(function() {
  function oldDocReady(component) {
    var stateValue = component.getValue();
    // Here, set the working graph params (defined up top) to what has come across in the state object from the server 
    if(stateValue["speed"] != null)
      speedP = stateValue["speed"];
    if(stateValue["autoarea"] != null)
    	  autoAreaP = stateValue["autoarea"];
    	if(stateValue["area"] != null) 
    	  areaP = stateValue["area"];
    	if(stateValue["gravity"] != null)
    	  gravityP = stateValue["gravity"];
    	if(stateValue["kcore"] != null)
    	  kcoreP = stateValue["kcore"];
 /*   	
    console.log("log state speed: "+stateValue["speed"]);
    console.log("log autoarea: "+stateValue["autoarea"]);
    console.log("log area: "+ stateValue["area"]);
    console.log("log gravity: "+stateValue["gravity"]);
    console.log("log kcore: " +stateValue["kcore"]);
    console.log("log default area: "+chart_width * chart_height / 2);
*/      
	var value = stateValue["data"];
	
	initNodes(value.nodes,"befriend");
	
	//start over
	data = {}; //empty object
	$.extend(data,value.nodes);
	
	// jam the link info into the node object under the "requires" key
	for(var lkey in value.links) {
	  var mylnk = value.links[lkey];	  
	  data[mylnk.source].requires.push(mylnk.target);
	  data[mylnk.source].linkweights.push(mylnk.value); //weight
	  weightScaler(mylnk.value); //weight
    }
	weightSpan = (weightMax-weightMin)

    arrangeNodes(data);

    var graph = {
        xOffset: 0,
        yOffset: 0,
        xSpacing: 1,
        ySpacing: 1,
        //root: data['mission_gatecrasher'],
       // root: data['charlie@m57.biz'],
        xView: -400,
        yView: -400,
        wView: 1600,
        hView: 800,
    };

    graph.nodes = [];
    graph.links = [];
    
    // delete nodes that don't pass the filter
    for (var key in data) {
      if(data[key].children.length < kcoreP) {  // filter
  	    delete data[key];
      }
    }
    // save nodes and create links if nodes exist
    for (var key in data) {
      graph.nodes.push(data[key]);
      for (var i = 0; i < data[key].children.length; i++) { 
    	    if(data[data[key].children[i].id] == null ) {  // was it deleted
    	    	  continue;  // referenced node must have been deleted
    	    }
        graph.links.push({
          id: key + i,
          source: data[key],
          target: data[key].children[i],
          weight: data[key].childrenweights[i]
        });
      }
    }

    // normalize row and initialize node locations
    graph.nodes.sort(function(l, r) {
        return l.row - r.row;
    });
    for (var i = 0; i < graph.nodes.length; i++) {
        graph.nodes[i].row = i + 1;
        graph.nodes[i].x = graph.nodes[i].depth * graph.xSpacing + graph.xOffset;
        graph.nodes[i].y = graph.nodes[i].row * graph.ySpacing + graph.yOffset;
        graph.nodes[i].size = 8; //forget node sizing: 20 / (graph.nodes[i].depth + 1) + 2;
    }

    // uncomment the following to specify the center node of the graph
    //graph.root.fixed = true;
    //graph.root.x = 0;
    //graph.root.y = 0;

    // This somehow gets reset when we enter the 2nd time
    $('#container').parent().width("100%");  // v-widget element
    $('#container').parent().height("100%"); // "
    
    $('#container').scrollLeft((chart_width - outer_width) / 2);
    $('#container').scrollTop((chart_height - outer_height) / 2);
    
    var svg = d3.select('svg#chart');
    svg.selectAll("*").remove();
    
    svg.attr('viewBox', function(d) {
      return '' + -chart_width / 2 + ' ' + -chart_height / 2 + ' ' + chart_width + ' ' + chart_height + '';
    });
   /* 
    console.log("log graphing with autoarea = "+autoAreaP);
    console.log("log graphing with area = "+areaP);
    console.log("log graphing with gravity = "+gravityP);
    console.log("log graphing with speed = "+speedP);
    console.log("log graphing with kcore = "+kcoreP);
   */ 
    var force = d3.layout.fruchtermanReingold()
        .autoArea(autoAreaP) //false)
        .area(areaP) //chart_width * chart_height / 2)// 3) //4)//8)
        .gravity(gravityP) //1.5)//0.75)
        .speed(speedP) //0.1)
        .iterations(1500)
        .nodes(graph.nodes)
        .links(graph.links);

    var link = svg.selectAll(".link")
        .data(graph.links)
        .enter().append("line")
        .attr("style", function(d) {
           return "stroke-width:"+calcLinkWidth(d.weight)+
                  ";stroke:"+calcLinkColor(d.weight);
        })
        .attr("class", "link");

    var node = svg.selectAll(".node")
        .data(graph.nodes)
        .enter().append("circle")
        .attr('r', function(d) {
            return d.size;
        })
        .attr("class", function(d) {
            return "node " + d.type;
        })
        .attr("id", function(d) {
            return d.id;
        })
        .on('click', onNodeClick);

    var text = svg.selectAll(".text")
        .data(graph.nodes)
        .enter().append("text")
        .attr("class", function(d) {
            return "text " + d.type;
        })
        .text(function(d) {
            return d.name;
        })
        .on('click', onNodeClick);

    force.on("tick", function(e) {
        text.attr("x", function(d) {
                var bbox = this.getBBox();
                var x = d.x - bbox.width / 2;
                return x;
            })
            .attr("y", function(d) {

                if (d.y == 0) return 0;
                var bbox = this.getBBox();
                var y = d.y + bbox.height / 3;
                y += 15 * (d.y > 0 ? 1 : -1);
                return y;
            });

        node.attr("cx", function(d) {
                return d.x;
            })
            .attr("cy", function(d) {
                return d.y;
            });

        link
            .attr("x1", function(d) {
                return d.source.x;
            })
            .attr("y1", function(d) {
                return d.source.y;
            })
            .attr("x2", function(d) {
                return d.target.x;
            })
            .attr("y2", function(d) {
                return d.target.y;
            });
    });
    
    force.start();
  }
};
