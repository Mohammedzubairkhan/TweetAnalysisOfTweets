<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<html  xmlns:xlink="http://www.w3.org/1999/xlink">
  <head>
  <link rel="stylesheet" href="./style.css">
  <link rel="stylesheet" href="./style1.css">
    <meta charset="utf-8">

    <title>Tree Example</title>

    <style>
	
	.node {
		cursor: pointer;
	}

	.node circle {
	  fill: #fff;
	  stroke: steelblue;
	  stroke-width: 3px;
	}

	.node text {
	  font: 12px sans-serif;
	}

	.link {
	  fill: none;
	  stroke: #ccc;
	  stroke-width: 2px;
	}
	
	#nav {
    line-height:30px;
    background-color:#eeeeee;
    height:auto;
    width:auto;
    float:bottom;
    padding:5px;          
}
	#container {
    margin: auto;
}
#graph {
    float: left;
    height:auto;
    width:auto;
}
.found {
  fill: #ff4136;
  stroke: #ff4136;
}

.search {
  float: left;
  font: 10px sans-serif;
  width: 30%;
}

ul.select2-results {
 max-height: 100px;
}

.select2-container,
.select2-drop,
.select2-search,
.select2-search input {
  font: 10px sans-serif;
}

#block_container {
  display: inline;
}

.middle_header {
font-size: 50px;
color:		#FFFFFF;
margin-top: 8px;
margin-left: 3px;
  background-color:#00BFFF;
  text-align: center;
  width: 1260px;
  height: 378px;
}

.image-content {
display: grid;
grid-template-columns: 600px auto;
}

.topic {
margin-top: 60px;
margin-right: 200px;
}

.button-grid {
margin-top: -300px;
margin-left: 430px;
display: grid;
grid-template-columns: 620px 100px;
}

    </style>

  </head>

  <body>
  <div id="cssmenu">
  	<ul>
  	<li>
	 <c:url value="NamedEntitySecond" var="url">
					<c:param name="param" value="${topic}" />
				</c:url>
				<a href="${url}"><span>Named Entity Graph</span> </a>
	</li>
	<li>
	 <c:url value="WordCloudServlet" var="url">
					<c:param name="param" value="${topic}" />
				</c:url>
				<a href="${url}"><span>Named entity WordCloud </span></a>
	</li>
	<li>
	 <c:url value="WordCloudwordServlet" var="url">
					<c:param name="param" value="${topic}" />
				</c:url>
				<a href="${url}"><span>WordCloud</span> </a>
	</li>
	<li>
	 <c:url value="TweetPullingServlet" var="url">
					<c:param name="param" value="${topic}" />
				</c:url>
				<a href="${url}"><span>Tweets<span></a>
	</li>
	</ul>
  </div>
<!--  <div id ="graph">-->
				<div class="middle_header">
				<div class="image-content"><img src="images/twitter-clipart-10.jpg"  height="378px">
				<div class="topic">${topic}
				Named Entity Graph</div>
				</div>
				<div class="button-grid">
				 <button class="button" onclick="expandAll()">Expand</button>
    <button class="button gray" class="gray" onclick="collapseAll()">Collapse</button>
				</div>	
				</div>

<!-- load the d3.js library -->	
<script src="http://cdnjs.cloudflare.com/ajax/libs/d3/3.4.13/d3.min.js"></script>
<script src="http://cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js"></script>
<link rel="stylesheet" type="text/css" href="http://cdnjs.cloudflare.com/ajax/libs/select2/3.5.2/select2.min.css"></link>
<script type="text/javascript" src="http://cdnjs.cloudflare.com/ajax/libs/select2/3.5.2/select2.min.js"></script>
<script type="text/javascript" src="http://d3js.org/d3.v3.min.js"></script>
	<div id="block_container">
   <div id="searchName"></div>
</div>
<script type="text/javascript" >


var treeData = JSON.parse("<%= request.getAttribute("tree") %>");
var tag = "<%= request.getAttribute("topic") %>";
console.log(tag)


function select2DataCollectName(d) {
    if (d.children)
        d.children.forEach(select2DataCollectName);
    else if (d._children)
        d._children.forEach(select2DataCollectName);
    select2Data.push(d.name);
}

//===============================================
function searchTree(d) {
    if (d.children)
        d.children.forEach(searchTree);
    else if (d._children)
        d._children.forEach(searchTree);
    var searchFieldValue = eval(searchField);
    if (searchFieldValue && searchFieldValue.match(searchText)) {
            // Walk parent chain
            var ancestors = [];
            var parent = d;
            while (typeof(parent) !== "undefined") {
                ancestors.push(parent);
		//console.log(parent);
                parent.class = "found";
                parent = parent.parent;
            }
	    //console.log(ancestors);
    }
}

//===============================================
function clearAll(d) {
    d.class = "";
    if (d.children)
        d.children.forEach(clearAll);
    else if (d._children)
        d._children.forEach(clearAll);
}

function collapseAllNotFound(d) {
    if (d.children) {
    	if (d.class !== "found") {
        	d._children = d.children;
        	d._children.forEach(collapseAllNotFound);
        	d.children = null;
	} else 
        	d.children.forEach(collapseAllNotFound);
    }
}

$("#searchName").on("select2-selecting", function(e) {
    clearAll(root);
    expandAll(root);
    update(root);

    searchField = "d.name";
    searchText = e.object.text;
    searchTree(root);
    root.children.forEach(collapseAllNotFound);
    update(root);
});
// ************** Generate the tree diagram	 *****************
var margin = {top: 20, right: 120, bottom: 20, left: 120},
	width = 5000 - margin.right - margin.left,
	height = 2000 - margin.top - margin.bottom;
	
var i = 0,
	duration = 750,
	root;

var tree = d3.layout.tree()
	.size([height, width]);

var diagonal = d3.svg.diagonal()
	.projection(function(d) { return [d.y, d.x]; });

var svg = d3.select("body").append("svg")
	.attr("width", width + margin.right + margin.left)
	.attr("height", height + margin.top + margin.bottom)
  .append("g")
	.attr("transform", "translate(" + margin.left + "," + margin.top + ")");

root = treeData[0];
root.x0 = height / 2;
root.y0 = 0;

function expand(d){   
    var children = (d.children)?d.children:d._children;
    if (d._children) {        
        d.children = d._children;
        d._children = null;       
    }
    if(children)
      children.forEach(expand);
}

function expandAll(){
    expand(root); 
    update(root);
}

function collapseAll(){
    root.children.forEach(collapse);
   // collapse(root);
    update(root);
}

//Collapse the node and all it's children
function collapse(d) {
  if(d.children) {
    d._children = d.children;
    d._children.forEach(collapse);
    d.children = null;
  }
}

select2Data = [];
select2DataCollectName(root);
select2DataObject = [];
select2Data.sort(function(a, b) {
          if (a > b) return 1; // sort
          if (a < b) return -1;
          return 0;
      })
      .filter(function(item, i, ar) {
          return ar.indexOf(item) === i;
      }) // remove duplicate items
      .filter(function(item, i, ar) {
          select2DataObject.push({
              "id": i,
              "text": item
          });
      });
$("#searchName").select2({
      data: select2DataObject,
      containerCssClass: "search"
});
//root.children.forEach(collapse);
collapse(root);
update(root);
d3.select(self.frameElement).style("height", "500px");

function update(source) {

  // Compute the new tree layout.
  var nodes = tree.nodes(root).reverse(),
	  links = tree.links(nodes);

  // Normalize for fixed-depth.
  nodes.forEach(function(d) { d.y = d.depth * 180; });

  // Update the nodes…
  var node = svg.selectAll("g.node")
	  .data(nodes, function(d) { return d.id || (d.id = ++i); });

  // Enter any new nodes at the parent's previous position.
  var nodeEnter = node.enter().append("g")
	  .attr("class", "node")
	  .attr("transform", function(d) { return "translate(" + source.y0 + "," + source.x0 + ")"; })
	  .on("click", click);

  nodeEnter.append("circle")
	  .attr("r", 1e-6)
	  .style("fill", function(d) { return d._children ? "lightsteelblue" : "#fff"; });

  //new code for link
   nodeEnter.each(function(d){
    var thisNode = d3.select(this);
    if (!d.children) {
    	var uril = encodeURIComponent(d.name);
    	//console.log(uril);
        thisNode.append("a")
            .attr("xlink:href", function(d) { return "http://localhost:8087/last/TweetGraphServlet?topic=" + encodeURIComponent(tag) + "&tid=" + d.tid + "&tweet=" + encodeURIComponent(d.name); })
            .append("text")
                .attr("x", 8)
                .attr("dy", 3)
                .attr("text-anchor", "start")
                .text(function(d) { 
                	var name;
          		  console.log(d.children);
          		  console.log(d._children);

          		  if(d.children)
          			  name = d.name+' ['+d.children.length+']';
          		  else if(d._children)
          			  name = d.name+' ['+d._children.length+']';
          		  else 
          			  name = d.name;
          		  return name; 
                	});
    } else {
        thisNode.append("text")
            .attr("x", -8)
            .attr("dy", 3)
            .attr("text-anchor", "end")
            .text(function(d) { var name;
    		  console.log(d.children);
      		  console.log(d._children);

      		  if(d.children)
      			  name = d.name+' ['+d.children.length+']';
      		  else if(d._children)
      			  name = d.name+' ['+d._children.length+']';
      		  else 
      			  name = d.name;
      		  return name;  });      
    }
});

  
  
  
  
  /*
  nodeEnter.append("a")
.attr("xlink:href", function(d){return   "http://en.wikipedia.org/wiki/"+d.name;}).append("text")
  	  .attr("x", function(d) { return d.children || d._children ? -13 : 13; })
	  .attr("dy", ".35em")
	  .attr("text-anchor", function(d) { return d.children || d._children ? "end" : "start"; })
	  .text(function(d) { 
		  var name;
		  console.log(d.children);
		  if(d.children)
			  name = d.name+' ['+d.children.length+']';
		  else if(d._children)
			  name = d.name+' ['+d._children.length+']';
		  else 
			  name = d.name;
		  return name; })
	  .style("fill-opacity", 1e-6);
*/
  // Transition nodes to their new position.
  var nodeUpdate = node.transition()
	  .duration(duration)
	  .attr("transform", function(d) { return "translate(" + d.y + "," + d.x + ")"; });

  nodeUpdate.select("circle")
	  .attr("r", 10)
	  .style("fill", function(d) { return d._children ? "lightsteelblue" : "#fff"; });

  nodeUpdate.select("text")
	  .style("fill-opacity", 1);

  // Transition exiting nodes to the parent's new position.
  var nodeExit = node.exit().transition()
	  .duration(duration)
	  .attr("transform", function(d) { return "translate(" + source.y + "," + source.x + ")"; })
	  .remove();

  nodeExit.select("circle")
	  .attr("r", 1e-6);

  nodeExit.select("text")
	  .style("fill-opacity", 1e-6);

  // Update the links…
  var link = svg.selectAll("path.link")
	  .data(links, function(d) { return d.target.id; });

  // Enter any new links at the parent's previous position.
  link.enter().insert("path", "g")
	  .attr("class", "link")
	  .attr("d", function(d) {
		var o = {x: source.x0, y: source.y0};
		return diagonal({source: o, target: o});
	  });

  // Transition links to their new position.
  link.transition()
	  .duration(duration)
	  .attr("d", diagonal);

  // Transition exiting nodes to the parent's new position.
  link.exit().transition()
	  .duration(duration)
	  .attr("d", function(d) {
		var o = {x: source.x, y: source.y};
		return diagonal({source: o, target: o});
	  })
	  .remove();

  // Stash the old positions for transition.
  nodes.forEach(function(d) {
	d.x0 = d.x;
	d.y0 = d.y;
  });
}

// Toggle children on click.
function click(d) {
	var kk="";
  if (d.children) {
	d._children = d.children;
	d.children = null;
	for(i = 0; i < d._children.length; i++){	
		 kk += "<li>" + d._children[i].name + "</li>";
		 	  console.log(d._children[i].name)
		  }
	document.getElementById("results").innerHTML = kk;
	 
  } else {
	d.children = d._children;
	d._children = null;
	for(i = 0; i < d.children.length; i++){	
		 kk += "<li>" + d.children[i].name + "</li>";
		 	  console.log(d.children[i].name)
		  }
	document.getElementById("results").innerHTML = kk;
  }
  
  update(d);
  
}


</script>

<div id="nav">
<ul id="results">


        </ul>
</div>
  </body>
</html>