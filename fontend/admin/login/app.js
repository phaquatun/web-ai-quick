document.querySelector(".btnLogin").addEventListener("click", (evn) => {
  let id = document.getElementById("userName").value;
  let pass = document.getElementById("pass").value;

  let body = {
    id: id,
    pass: pass,
  };

  let options = {
    method: "POST",
    body: JSON.stringify(body),
  };

  let url = window.location.href.toString();
  fetch(url.replace("/admin/login/dung/", "/log/au"), options)
    .then((server) => server.json())
    .then((server) => {
      if (server.success == true) {
        window.location.href = url.replace("/admin/login/dung/", "/admin/info/account");
        return;
      } else {
        console.log(`run failed `);
        document.querySelector(".notice").textContent = "sai id hoặc mật khẩu";
      }
    });
});


function setCookie(cname, cvalue, exdays) {
  var d = new Date();
  d.setTime(d.getTime() + (exdays*24*60*60*1000));
  var expires = "expires="+d.toUTCString();
  document.cookie = cname + "=" + cvalue + "; " + expires;
}