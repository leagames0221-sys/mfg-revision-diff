// Previous / next difference: moves between the rows that are not "same and in place".
(function () {
  "use strict";
  var rows = Array.prototype.slice.call(document.querySelectorAll('tr[data-diff="1"]'));
  var prev = document.getElementById("prev");
  var next = document.getElementById("next");
  var pos = document.getElementById("pos");
  if (!prev || !next || !pos) {
    return;
  }
  var cur = -1;

  function show() {
    rows.forEach(function (r) { r.classList.remove("current"); });
    if (cur >= 0) {
      rows[cur].classList.add("current");
      rows[cur].scrollIntoView({ block: "center" });
    }
    pos.textContent = (cur + 1) + " / " + rows.length;
    prev.disabled = cur <= 0;
    next.disabled = cur >= rows.length - 1;
  }

  prev.addEventListener("click", function () { if (cur > 0) { cur--; show(); } });
  next.addEventListener("click", function () { if (cur < rows.length - 1) { cur++; show(); } });
  rows.forEach(function (r, i) {
    r.addEventListener("click", function () { cur = i; show(); });
  });
  show();
})();
