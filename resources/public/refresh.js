function showRefreshIcon() {
  document.getElementsByClassName("refresh-icon")[0].className = "refresh-icon";
}

function refresh() {
  showRefreshIcon();
  window.location.reload();
}

window.onload=function(){
  var refreshTimeMillis = 30000;
  window.setInterval(refresh, refreshTimeMillis);
};
