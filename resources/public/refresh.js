refreshBody = function(){
  var xhttp = new XMLHttpRequest();
  xhttp.onreadystatechange = function() {
    if (xhttp.readyState == 4) {
      if (xhttp.status == 200) {
        var htmlResponse = xhttp.responseText;
        var htmlElement = document.createElement('html');
        htmlElement.innerHTML = htmlResponse;
        var updatedBodyElement = htmlElement.getElementsByTagName('body')[0];
        var currentBodyElement = document.getElementsByTagName('body')[0];
        currentBodyElement.parentNode.replaceChild(updatedBodyElement, currentBodyElement);
      }
      else {
        var updatedBodyElement = document.createElement('body');
        updatedBodyElement.innerHTML = "SERVER ERROR";
        var currentBodyElement = document.getElementsByTagName('body')[0];
        currentBodyElement.parentNode.replaceChild(updatedBodyElement, currentBodyElement);
      }
    }
  };
  xhttp.open("GET", "/", true);
  xhttp.send();
}

window.onload=function(){
  window.setInterval(refreshBody, window.refreshSeconds * 1000);
};
