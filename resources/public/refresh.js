function showRefreshIcon() {
  document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon";
}

function hideRefreshIcon() {
  document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon hidden";
}

function refresh() {
  showRefreshIcon();
  window.location.reload();
}

window.onload=function(){
  var oneMinute = 60000;
  window.setInterval(refresh, oneMinute);
};
