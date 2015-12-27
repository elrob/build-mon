function updateBuildPanel(buildData) {
  document.querySelector("link[rel='shortcut icon'").setAttribute("href", buildData["favicon-path"])
  document.getElementsByClassName("build-panel")[0].className = "build-panel " + buildData["state"];
  document.getElementsByClassName("status")[0].innerHTML = buildData["status-text"];
  document.getElementsByClassName("build-definition-name")[0].innerHTML = buildData["build-definition-name"];
  document.getElementsByClassName("build-number")[0].innerHTML = buildData["build-number"];
  document.getElementsByClassName("commit-message")[0].innerHTML = buildData["commit-message"];
}

function refreshBuild(refreshPath) {
  return function() {
    var xhttp = new XMLHttpRequest();
    xhttp.onreadystatechange = function() {
      if (xhttp.readyState == 4) {
        if (xhttp.status == 200) {
          var buildData = JSON.parse(xhttp.responseText);
          updateBuildPanel(buildData);
        }
        else {
          var updatedBodyElement = document.createElement('body');
          updatedBodyElement.innerHTML = "BUILD MONITOR UNREACHABLE";
          var currentBodyElement = document.getElementsByTagName('body')[0];
          currentBodyElement.parentNode.replaceChild(updatedBodyElement, currentBodyElement);
        }
      }
    };
    xhttp.open("GET", refreshPath, true);
    xhttp.send();
  }
}

window.onload=function(){
  window.setInterval(refreshBuild(window.refreshPath), window.refreshSeconds * 1000);
};
