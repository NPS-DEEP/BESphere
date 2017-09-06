
window.edu_nps_deep_beArtifactGui_d3_fruchRein_FruchtermanReingold =
function() {
    // Get the component from Vaadin
	var myelement = this.getElement();
	
	// d3 looks for svg element
	myelement.innerHTML =
		//"<svg/><style>.links line {stroke: #999;stroke-opacity: 0.6;}.nodes circle {stroke: #fff;stroke-width: 1.5px;}</style>";
		//"<div id='dialog'><svg id='path'></svg><div id='info'><p>Description</p><table><tr><td>Cost</td><td>1</td><td>2</td><td>3</td><td>4</td></tr></table></div></div>"+
	    "<div id='container'><svg id='chart'></svg></div>";
	myelement.style.height = "100%";
	myelement.style.width = "100%";
    var mycomponent = new fdNameSpace.FruchtermanReingold(this.getElement());

    // Handle changes from the server-side
    this.onStateChange = function()
    {
      mycomponent.setValue(this.getState().value);
    };

    // Pass user interaction to the server-side
    var self = this;
    mycomponent.click = function()
    {
      self.toJava(mycomponent.getValue());
    };
};