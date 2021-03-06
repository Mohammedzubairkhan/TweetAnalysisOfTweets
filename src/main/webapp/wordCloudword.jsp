<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<!DOCTYPE html>
<meta charset="utf-8">
<html>
<head>
<title>cloud</title>
</head>
<body>
<div align="center">
<script src="http://d3js.org/d3.v3.min.js"></script>
  <script src="https://rawgit.com/jasondavies/d3-cloud/master/build/d3.layout.cloud.js"></script>
<script>
  var fill = d3.scale.category20();
  var data = JSON.parse("<%= request.getAttribute("wordCloudword") %>");
 console.log(data);
  d3.layout.cloud().size([500, 500])
          .words(data.map(function(d) {
              return {text: d.word, size: (d.weight/20)};
          }))
          .padding(5)
         .rotate(function() { return ~~(Math.random() * 2) * 90; })
         .font("Impact")
         .fontSize(function(d) { return d.size; })
         .on("end", draw)
         .start();

  function draw(words) {
      d3.select("body").append("svg")
           .attr("width", 300)
          .attr("height", 300)
          .append("g")
          .attr("transform", "translate(150,150)")
          .selectAll("text")
          .data(words)
          .enter().append("text")
          .style("font-size", function(d) { return d.size + "px"; })
          .style("font-family", "Impact")
          .style("fill", function(d, i) { return fill(i); })
          .attr("text-anchor", "middle")
          .attr("transform", function(d) {
              return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
          })
         .text(function(d) { return d.text; });
   }

   </script>
   </div>
</body>
</html>