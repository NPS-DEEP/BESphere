// Define the namespace
var fdNameSpace = fdNameSpace || {};

// This inits the component
fdNameSpace.FruchtermanReingold = function(element) {

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
	//setTimeout(function(){debugger;}, 6000);
	var svg = d3.select("svg");
	
	var width = value[0];
	var height= value[1];
	var mygraph = value[2];
	
	svg.attr("width",width);
	svg.attr("height",height);

	//var mycolors = d3.scaleOrdinal(d3.schemeCategory20);
   // var min_zoom = 0.1;
   // var max_zoom = 7;
   // var zoom = d3.zoom().scaleExtent([min_zoom,max_zoom]);

	//var simulation = d3.forceSimulation()
	//    .force("link", d3.forceLink()
	//    		.id(function(d) { return d.id; })
	//    		.distance(function(d){  return 100;/*d.value;*/})) //default=30
	//    .force("charge", d3.forceManyBody())
	//    .force("center", d3.forceCenter(width / 2, height / 2));

    var simulation = d3.layout.fruchtermanReingold()
        .autoArea(false)
        .area(width * height / 8)
        .gravity(0.75)
        .speed(0.1)
        .iterations(1500)
        .nodes(mygraph.nodes)
        .links(mygraph.links);

	var link = svg.append("g")  // group of lines
	    .attr("class", "links")
	    .selectAll("line")
	    .data(mygraph.links)
	    .enter().append("line")
	    .attr("stroke-width", function(d) { return Math.sqrt(d.value); });

	var circnodes = svg.append("g") // group of circles
	    .attr("class", "nodes")
	    .selectAll("circle")
	    .data(mygraph.nodes)
	    .enter().append("g").attr("class","nodegroups")
	     .on("click", nodeclick)
	      .on("dblclick", nodedblclick);
	
	var node = circnodes.append("circle")
	      .attr("r", function(d) {return d.diam;})
	      //.attr("fill", function(d) { return mycolors(d.group); })
	      ;/*.call(d3.drag()
	          .on("start", dragstarted)
	          .on("drag", dragged)
	          .on("end", dragended));*/
	
	node.append("title")
	    .text(function(d) { return d.id; });
	
	var labels = circnodes.append("svg:text")
		.attr("x",0)
		.attr("y",0)
		//.attr("class","shadow")
		.text(function(d) { return d.id; });
	/*			        
    simulation
        .nodes(mygraph.nodes)
	    .on("tick", tick);

    simulation.force("link")
         .links(mygraph.links);
    */
	function tick()
	{
      link
	    .attr("x1", function(d) { return d.source.x; })
	    .attr("y1", function(d) { return d.source.y; })
	    .attr("x2", function(d) { return d.target.x; })
	    .attr("y2", function(d) { return d.target.y; });

	  node
	    .attr("cx", function(d) { return d.x; })
	    .attr("cy", function(d) { return d.y; });
	  
	  labels
	    .attr("x", function(d) {	return d.x + 7 + (d.big?15:0);})   // move over if the circle is big
	    .attr("y", function(d) { return d.y + 4; });
	  
	}

	function dragstarted(d)
	{
      simulation.force("center",null);
	  simulation.stop();
	}
	function dragged(d)
	{
	  d.px += d3.event.dx;
      d.py += d3.event.dy;
      d.x += d3.event.dx;
      d.y += d3.event.dy; 
      tick();	
	}
	function dragended(d)
	{
	  d.fixed = true; // of course set the node to fixed so the force doesn't include the node in its auto positioning stuff
      tick();

      simulation.restart();	
	}

	// action to take on mouse click
   	function nodeclick() {
      // "this" = g element enclosing circle and text
      this.__data__.big = true;  //mark this data item as having a big circle
	    
      d3.select(this).select("text")
        .attr("font-size", "20px");
      d3.select(this).select("circle")
	    .attr("r", 16);
	}

	// action to take on mouse double click
	function nodedblclick() {
      this.__data__.big = false;
	  d3.select(this).select("text")
	    .attr("font-size", null);  // remove from element, use css to style
      d3.select(this).select("circle")
        .attr("r", 7);  //default
	}
  }
};

