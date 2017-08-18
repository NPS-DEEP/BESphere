window.edu_nps_deep_beArtifactGui_d3_ForceDirected =
function() {
    // Get the component from Vaadin
	var myelement = this.getElement();
	
	// d3 looks for svg element
	myelement.innerHTML =
		"<svg/><style>.links line {stroke: #999;stroke-opacity: 0.6;}.nodes circle {stroke: #fff;stroke-width: 1.5px;}</style>";
    
    var mycomponent = new fdNameSpace.ForceDirected(this.getElement());

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