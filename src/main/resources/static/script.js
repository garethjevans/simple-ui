const msgerForm = get(".msger-inputarea");
const msgerInput = get(".msger-input");
const msgerChat = get(".msger-chat");
const msgerClear = get(".msger-clear-btn")
const modelsSelect = get(".models")

// Icons made by Freepik from www.flaticon.com
const BOT_IMG = "https://image.flaticon.com/icons/svg/327/327779.svg";
const PERSON_IMG = "https://image.flaticon.com/icons/svg/145/145867.svg";
const PERSON_NAME = "user";

var conversation = {messages:[]}

var msgText = ""

msgerForm.addEventListener("submit", event => {
  event.preventDefault();

  msgText = msgerInput.value;
  if (!msgText) return;

  model = modelsSelect.options[modelsSelect.selectedIndex].text;
  if (!model) return;

  appendMessage(PERSON_NAME, PERSON_IMG, "right", msgText);
  msgerInput.value = "";

  conversation.model = model
  conversation.messages.push({role:"user", message: msgText})

  userAction();
});

window.addEventListener("load", event => {
  modelsAction();
});

msgerClear.addEventListener("click", event => {
  event.preventDefault();
  clear();
});

function appendMessage(name, img, side, text) {
  //   Simple solution for small apps
  const msgHTML = `
    <div class="msg ${side}-msg">
      <div class="msg-img" style="background-image: url(${img})"></div>

      <div class="msg-bubble">
        <div class="msg-info">
          <div class="msg-info-name">${name}</div>
          <div class="msg-info-time">${formatDate(new Date())}</div>
        </div>

        <div class="msg-text">${text}</div>
      </div>
    </div>
  `;

  msgerChat.insertAdjacentHTML("beforeend", msgHTML);
  msgerChat.scrollTop += 500;
}

const userAction = async () => {
  const response = await fetch('/chat', {
    method: 'POST',
    body: JSON.stringify(conversation),
    headers: {
      'Content-Type': 'application/json'
    }
  });
  const myJson = await response.json();
  conversation.messages.push({role:myJson[0].role, message: myJson[0].message})
  appendMessage(myJson[0].role, BOT_IMG, "left", myJson[0].message);
}

const modelsAction = async () => {
  const response = await fetch('/models', {
    method: 'GET',
    headers: {
      'Accept': 'application/json'
    }
  });
  const myJson = await response.json();
  for (let i = 0; i < myJson.length; i++) {
    modelsSelect.options[modelsSelect.options.length] = new Option(myJson[i], myJson[i]);
  }
}

// Utils
function get(selector, root = document) {
  return root.querySelector(selector);
}

function formatDate(date) {
  const h = "0" + date.getHours();
  const m = "0" + date.getMinutes();

  return `${h.slice(-2)}:${m.slice(-2)}`;
}

function clear() {
  conversation = {messages:[]}
  msgerChat.innerHTML = '&nbsp;'
}

