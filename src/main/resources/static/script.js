const msgerForm = get(".msger-inputarea");
const msgerInput = get(".msger-input");
const msgerChat = get(".msger-chat");
const msgerClear = get(".msger-clear-btn")
const modelsSelect = get(".models")

// Icons made by Freepik from www.flaticon.com
const PERSON_NAME = "user";

var conversation = {messages:[]}
var converter = new showdown.Converter();

var msgText = ""

msgerForm.addEventListener("submit", event => {
  event.preventDefault();

  msgText = msgerInput.value;
  if (!msgText) return;

  model = modelsSelect.options[modelsSelect.selectedIndex].text;
  if (!model) return;

  appendMessage(PERSON_NAME, "fa-user", "right", msgText);
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

function appendMessage(name, img, side, text, usage) {
  var msgHTML = "";
  if (side === "left") {
    if (name !== "error (error)") {
      msgHTML = `
        <div class="clr-row left-msg">
          <div class="${name}">
            <div class="card">
              <div class="card-header">${name}</div>
              <div class="card-block">
                <div class="card-title">${formatDate(new Date())}</div>
                <div class="card-text">
                  ${text}
                </div>
              </div>
              <div class="card-footer">
                <i class="fa-solid fa-terminal"></i> ${usage.promptTokens}, 
                <i class="fa-solid fa-arrow-right-from-bracket"></i> ${usage.completionTokens}, 
                <i class="fa-solid fa-square-plus"></i> ${usage.totalTokens}, 
                <i class="fa-solid fa-clock"></i> ${usage.timeTaken}ms, 
                <i class="fa-solid fa-gauge-high"></i> ${usage.tokensPerSecond}
              </div>
            </div>
          </div>
        </div>
      `;
    } else {
      msgHTML = `
        <div class="clr-row left-msg">
          <div class="${name}">
            <div class="card">
              <div class="card-header">${name}</div>
              <div class="card-block">
                <div class="card-title">${formatDate(new Date())}</div>
                <div class="alert alert-warning alert-sm">
                  <div class="alert-items">
                    <div class="alert-item static">
                      <div class="alert-icon-wrapper">
                        <cds-icon class="alert-icon" shape="exclamation-triangle"></cds-icon>
                      </div>
                      <span class="alert-text">${text}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      `;
    }
  } else {
    msgHTML = `
      <div class="clr-row right-msg">
        <div class="${name}">
          <div class="card">
            <div class="card-header">${name}</div>
            <div class="card-block">
              <div class="card-title">${formatDate(new Date())}</div>
              <div class="card-text">
                ${text}
              </div>
            </div>
          </div>
        </div>
      </div>
  `;
  }

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
  if (myJson.messages[0].role !== "error") {
    conversation.messages.push({role: myJson.messages[0].role, message: myJson.messages[0].message})
  }

  var role = myJson.messages[0].role + " (" + myJson.model + ")"
  appendMessage(role,
      "fa-desktop",
      "left",
      converter.makeHtml(myJson.messages[0].message),
      myJson.usage);
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
  const s = "0" + date.getSeconds();

  return `${h.slice(-2)}:${m.slice(-2)}:${s.slice(-2)}`;
}

function clear() {
  conversation = {messages:[]}
  msgerChat.innerHTML = '&nbsp;'
}

