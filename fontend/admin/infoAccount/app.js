var host = window.location.protocol + "//" + window.location.host;
var countPage = "countPage",
  rowClick = "rowClick";

localStorage.setItem(countPage, "0");
localStorage.setItem(rowClick, "-1");

getDataAccount();

document.querySelector("h2").addEventListener("click", (evn) => {
  localStorage.setItem(countPage, "0");
  localStorage.setItem(rowClick, "-1");
  getDataAccount();
});

document.querySelector(".close").addEventListener("click", (evn) => {
  document.querySelector("#myModal").style.display = "none";
});

document.querySelector(".aNex").addEventListener("click", (evn) => {
  let numLim = parseInt(localStorage.getItem("limtCountClient"));

  let numCount = parseInt(localStorage.getItem(countPage));
  numCount = numCount < numLim - 1 ? numCount + 1 : numCount;
  localStorage.setItem(countPage, numCount.toString());

  // console.log(`check numCount ${numCount}`);
  getDataAccount();
});

document.querySelector(".aPre").addEventListener("click", (evn) => {
  let numCount = parseInt(localStorage.getItem(countPage));
  numCount = numCount == 0 ? 0 : numCount - 1;
  localStorage.setItem(countPage, numCount.toString());

  // // console.log(`check numCount ${numCount}`);

  getDataAccount();
});

document.querySelector(".active").addEventListener("click", (evt) => {
  let id = document.querySelector(`.user`).innerHTML;
  let passAdmin = document.querySelector("#pass-admin").value;

  // // console.log(`check id active ${id}  passadmin ${passAdmin}`);

  document.querySelector(".loading").style.display = "inherit";

  let data = {
    id: id,
    passAdmin: passAdmin,
  };

  let option = {
    method: "POST",
    body: JSON.stringify(data),
  };

  let url = host.concat("/api/active/account/");
  fetch(url, option)
    .then((server) => server.json())
    .then((server) => {
      if (server.success == true) {
        // // console.log(`check success ${server.response}`);
        document.querySelector(".loading").style.display = "none";
        document.querySelector(".modal").style.display = "none";
        let countRow = parseInt(localStorage.getItem("rowClick"));
        // // console.log(`rowClick ${countRow}`);
        document.querySelector(`.row${countRow}colum6`).innerHTML = "Active";
      }
    });
});

document.querySelector(".delete").addEventListener("click", (evt) => {
  let url = host.concat("/delete/api/account/");

  let id = document.querySelector(`.user`).innerHTML;
  let passAdmin = document.querySelector("#pass-admin").value;

  // // console.log(`check id active ${id}  passadmin ${passAdmin}`);

  document.querySelector(".loading").style.display = "inherit";


  let data = {
    id: id,
    passAdmin: passAdmin,
  };

  let option = {
    method: "POST",
    body: JSON.stringify(data),
  };

  fetch(url,option)
  .then((server) => server.json())
  .then((server) => {
    if (server.success == true) {
      getDataAccount();
      document.querySelector(".loading").style.display = "none";
      document.querySelector("#myModal").style.display = "none";
    }
  });
});

document.querySelector(".search img").addEventListener("click", (evt) => {
  searchAccount();
});

function getDataAccount() {
  let url = host.concat("/api/info/account/");

  let data = {
    count: parseInt(localStorage.getItem(countPage)),
  };

  // console.log(`body ${JSON.stringify(data)}`);
  let option = {
    method: "POST",
    body: JSON.stringify(data),
  };

  fetch(url, option)
    .then((server) => server.json())
    .then((server) => {
        // console.log(`server getDataAccount ${JSON.stringify(server)}`);
      let parseServer = JSON.parse(server.value);

      let jsonAccounts = parseServer.valueAccount;
      let limtCountClient = parseServer.limtCountClient;

      localStorage.setItem("limtCountClient", limtCountClient);

      //   // console.log(`limtCountClient ${limtCountClient}`);

      document.querySelectorAll(".valTr").forEach((e) => e.remove());

      if (server.success == true) {
        for (let index = 0; index < jsonAccounts.length; index++) {
          const element = jsonAccounts[jsonAccounts.length - 1 - index];
          document
            .querySelector(".detailTable")
            .insertAdjacentHTML("beforeend", handleResponse(element, index));
        }
      }
    });
}

function handleResponse(server, count) {
  // // console.log(`json ${JSON.stringify(server)}`);

  let parseServer = JSON.parse(JSON.stringify(server));
  // console.log(`active ${parseServer.active == undefined}`);

  let date = new Date(server.time);
  let displayDate =
    date.toLocaleDateString() + " - " + date.toLocaleTimeString();

  let active =
    parseServer.active == undefined ? "" : parseServer.active.toString();

  let dateActive =
    parseServer.timeActive == undefined ? "" : parseServer.timeActive;
  if (parseServer.timeActive != undefined) {
    dateActive = new Date(parseServer.timeActive);
    dateActive =
      dateActive.toLocaleDateString() + " - " + dateActive.toLocaleTimeString();
  }

  let data = `<tr class="valTr">
    <td>${count + 1 + parseInt(localStorage.getItem(countPage)) * 20}</td>
    <td class="row${count}colum${2}" ">${parseServer.id}</td>
    <td class="row${count}colum${3}" ">${parseServer.username}</td>
    <td class="row${count}colum${4}" ">${parseServer.pass}</td>
    <td class="row${count}colum${5}" ">${displayDate.toString()}</td>
    <td class="row${count}colum${6}" ">${active}</td>
    <td class="row${count}colum${7}" ">${dateActive}</td>
    <td class="row${count}colum${8}"  onclick="clickCopy(${count},${7})"></td>
    </tr>`;

  return data;
}

function clickCopy(count, colum) {
  let id = document.querySelector(`.row${count}colum${2}`).innerHTML;
  let pass = document.querySelector(`.row${count}colum${3}`).innerHTML;
  let date = document.querySelector(`.row${count}colum${4}`).innerHTML;

  let row = count;
  localStorage.setItem(rowClick, row.toString());

  document.querySelector("#myModal").style.display = "inherit";

  document.querySelector(".user").innerHTML = id;
  document.querySelector(".pass").innerHTML = pass;
  document.querySelector(".date").innerHTML = date;
}

function searchAccount() {
  localStorage.setItem(countPage, "0");

  let url = host.concat("/search/api/result/");
  let search = document.querySelector("#search-account").value;

  let data = {
    valueSearch: search,
  };

  let option = {
    method: "POST",
    body: JSON.stringify(data),
  };

  fetch(url, option)
    .then((server) => server.json())
    .then((server) => {
      let parseServer = JSON.parse(server.value);

      let valueNames = parseServer.valueNames;
      document.querySelectorAll(".valTr").forEach((e) => {
        e.remove();
      });

      for (let index = 0; index < valueNames.length; index++) {
        const element = valueNames[index];
        document
          .querySelector(".detailTable")
          .insertAdjacentHTML("beforeend", handleResponse(element, index));
      }
    });
}
