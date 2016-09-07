// function updateBuildPanel(buildPanel, buildData) {
//   buildPanel.className = "panel " + buildData["state"];
//   buildPanel.getElementsByClassName("build-definition-name")[0].innerHTML = buildData["build-definition-name"];
//   buildPanel.getElementsByClassName("build-number")[0].innerHTML = buildData["build-number"];
//   buildPanel.getElementsByClassName("commit-message")[0].innerHTML = buildData["commit-message"];
// }
//
// function updateReleasePanel(releasePanel, releaseData) {
//   releasePanel.className = "panel " + releaseData["state"];
//   releasePanel.getElementsByClassName("release-definition-name")[0].innerHTML = releaseData["release-definition-name"];
//   releasePanel.getElementsByClassName("release-number")[0].innerHTML = releaseData["release-number"];
//   // releasePanel.getElementsByClassName("commit-message")[0].innerHTML = buildData["commit-message"];
// }
//
// function showRefreshIcon() {
//   document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon";
// }
//
// function hideRefreshIcon() {
//   document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon hidden";
// }
//
// function showErrorModal(errorMessage) {
//   document.getElementsByClassName("error-modal-text")[0].innerHTML = errorMessage;
//   document.getElementsByClassName("error-modal")[0].className = "error-modal";
// }
//
// function hideErrorModal() {
//   document.getElementsByClassName("error-modal")[0].className = "error-modal hidden";
// }
//
// function updateFavicon(refreshArray) {
//   var statesOrderedWorstFirst = ["failed", "in-progress-after-failed", "in-progress", "succeeded"];
//   var currentStates = refreshArray.slice();
//   currentStates.sort(function(a,b) {
//     return statesOrderedWorstFirst.indexOf(a) - statesOrderedWorstFirst.indexOf(b);
//   });
//   var worstCurrentState = currentStates[0];
//   var faviconPath = "/favicon_" + worstCurrentState + ".ico";
//   document.getElementById("favicon").setAttribute("href", faviconPath);
// }
//
// function refreshBuildPanel(buildDefinitionId, refreshArrayIndex, refreshArray) {
//   var xhttp = new XMLHttpRequest();
//   xhttp.onreadystatechange = function() {
//     if (xhttp.readyState == 4) {
//       if (xhttp.status == 200) {
//         hideErrorModal();
//         var buildData = JSON.parse(xhttp.responseText);
//         var buildPanelId = "build-definition-id-" + buildDefinitionId;
//         var buildPanel = document.getElementById(buildPanelId);
//         updateBuildPanel(buildPanel, buildData);
//       }
//       else if (xhttp.status == 404) {
//         showErrorModal("Error Retrieving Build Info From VSO");
//       }
//       else {
//         showErrorModal("Build Monitor Unreachable");
//       }
//       refreshArray[refreshArrayIndex] = buildData["state"];
//       if (refreshArray.every(function(value){return value;})) {
//         updateFavicon(refreshArray);
//         hideRefreshIcon();
//       }
//     }
//   };
//   xhttp.open("GET", "/ajax/build-definitions/" + buildDefinitionId, true);
//   xhttp.send();
// }
//
//
// function refreshReleasePanel(releaseDefinitionId, refreshArrayIndex, refreshArray) {
//   var xhttp = new XMLHttpRequest();
//   xhttp.onreadystatechange = function() {
//     if (xhttp.readyState == 4) {
//       if (xhttp.status == 200) {
//         hideErrorModal();
//         var releaseData = JSON.parse(xhttp.responseText);
//         console.log(releaseData);
//         var releasePanelId = "release-definition-id-" + releaseDefinitionId;
//         var releasePanel = document.getElementById(releasePanelId);
//         updateReleasePanel(releasePanel, releaseData);
//       }
//       else if (xhttp.status == 404) {
//         showErrorModal("Error Retrieving Build Info From VSO");
//       }
//       else {
//         showErrorModal("Project Monitor Unreachable");
//       }
//       refreshArray[refreshArrayIndex] = releaseData["state"];
//       if (refreshArray.every(function(value){return value;})) {
//         updateFavicon(refreshArray);
//         hideRefreshIcon();
//       }
//     }
//   };
//   xhttp.open("GET", "/ajax/release-definitions/" + releaseDefinitionId, true);
//   xhttp.send();
// }
//
// function refreshProject() {
//   showRefreshIcon();
//   var refreshBuildArray = window.buildDefinitionIds.map(function(){return false;});
//   for (i = 0; i < window.buildDefinitionIds.length; i++) {
//     refreshBuildPanel(window.buildDefinitionIds[i], i, refreshBuildArray);
//   }
//
//   var refreshReleaseArray = window.releaseDefinitionIds.map(function(){return false;});
//   for (i = 0; i < window.releaseDefinitionIds.length; i++) {
//     refreshReleasePanel(window.releaseDefinitionIds[i], i, refreshReleaseArray);
//   }
// }


window.onload=function(){
  var oneMinute = 60000;
  window.setInterval(window.location.reload.bind(window.location), oneMinute);
};
