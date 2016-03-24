function updateBuildPanel(buildPanel, buildData) {
  buildPanel.className = "build-panel " + buildData["state"];
  buildPanel.getElementsByClassName("status")[0].innerHTML = buildData["status-text"];
  buildPanel.getElementsByClassName("build-definition-name")[0].innerHTML = buildData["build-definition-name"];
  buildPanel.getElementsByClassName("build-number")[0].innerHTML = buildData["build-number"];
  buildPanel.getElementsByClassName("commit-message")[0].innerHTML = buildData["commit-message"];
}

function showRefreshIcon() {
  document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon";
}

function hideRefreshIcon() {
  document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon hidden";
}

function showErrorModal(errorMessage) {
  document.getElementsByClassName("error-modal-text")[0].innerHTML = errorMessage;
  document.getElementsByClassName("error-modal")[0].className = "error-modal";
}

function hideErrorModal() {
  document.getElementsByClassName("error-modal")[0].className = "error-modal hidden";
}

function updateFavicon(refreshArray) {
  var statesOrderedWorstFirst = ["failed", "in-progress-after-failed", "in-progress", "succeeded"];
  var currentStates = refreshArray.slice();
  currentStates.sort(function(a,b) {
    return statesOrderedWorstFirst.indexOf(a) - statesOrderedWorstFirst.indexOf(b);
  });
  var worstCurrentState = currentStates[0];
  var faviconPath = "/favicon_" + worstCurrentState + ".ico";
  document.getElementById("favicon").setAttribute("href", faviconPath);
}

function refreshBuildPanel(buildDefinitionId, refreshArrayIndex, refreshArray) {
  var xhttp = new XMLHttpRequest();
  xhttp.onreadystatechange = function() {
    if (xhttp.readyState == 4) {
      if (xhttp.status == 200) {
        hideErrorModal();
        var buildData = JSON.parse(xhttp.responseText);
        var buildPanelId = "build-definition-id-" + buildDefinitionId;
        var buildPanel = document.getElementById(buildPanelId);
        updateBuildPanel(buildPanel, buildData);
      }
      else if (xhttp.status == 404) {
        showErrorModal("Error Retrieving Build Info From VSO");
      }
      else {
        showErrorModal("Build Monitor Unreachable");
      }
      refreshArray[refreshArrayIndex] = buildData["state"];
      if (refreshArray.every(function(value){return value;})) {
        updateFavicon(refreshArray);
        hideRefreshIcon();
        getElementsByTagName('body').className = 'panel-count-' + 
      }
    }
  };
  xhttp.open("GET", "/ajax/build-definitions/" + buildDefinitionId, true);
  xhttp.send();
}

function refreshBuild() {
  showRefreshIcon();
  var refreshArray = window.buildDefinitionIds.map(function(){return false;});
  for (i = 0; i < window.buildDefinitionIds.length; i++) {
    refreshBuildPanel(window.buildDefinitionIds[i], i, refreshArray);
  }
}

window.onload=function(){
  window.setInterval(refreshBuild, window.refreshSeconds * 1000);
};
