<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<html>
<head>
<title>List of trending topics</title>

<style>

body{
background-color: lightblue;
}
.list-type6 {
	width: 410px;
	margin: 0 auto;
}
/* LIST #8 */
.list-type6 {
	
}

.list-type6 ul {
	list-style: none;
}

.list-type6 ul li {
	font-size: 15px;
	font-family: 'Raleway', sans-serif;
}

.list-type6 ul li a {
	display: block;
	width: 390px;
	margin-left: -40px;
	height: 24px;
	background-color: #03B3CB;
	border-left: 5px solid #222;
	border-right: 5px solid #222;
	padding-left: 10px;
	padding-top: 7px;
	text-decoration: none;
	color: white;
	margin-bottom: 7px;
	transition: all .2s ease-in-out;
}

.list-type6 ul li a:hover {
	-moz-transform: rotate(-5deg);
	-moz-box-shadow: 10px 10px 20px #000000;
	-webkit-transform: rotate(-5deg);
	-webkit-box-shadow: 10px 10px 20px #000000;
	transform: rotate(-5deg);
	box-shadow: 10px 10px 20px #000000;
	align:left;
}
</style>
</head>
<body>


<div class="hero">
  <center><h1>Tweets Analysis</h1></center>
</div>
<div class="content-wrapper" align="left">
  <h1>Topics : </h1>
  </div>
	<div class="list-type6" align="left">
		<ul>
			<c:forEach items="${hashtags}" var="employ">
				<c:url value="NamedEntityFirst" var="url">
					<c:param name="param" value="${employ.desc}" />
				</c:url>
				<li><a href="${url}">${employ._id}) ${employ.desc}</a></li>
			</c:forEach>
		</ul>
	</div>
</body>
</html>