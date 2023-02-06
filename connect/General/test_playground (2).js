import dom from './util/dom';

function getContent(element) {
    if (element) return (element.textContent || '').trim();
}

function pickData(form) {
    let data = {};

    Array.from(form.querySelectorAll('[name]')).forEach(e => {
        if (e.name === 'api') {
            let optionElement = e.querySelector(`option[value="${e.value}"]`);

            data.api_url = getContent(optionElement);
            data.service_ticket = optionElement.dataset.serviceTicket;
        }

        data[e.name] = e.value;
    });

    try {
        data.headers = JSON.parse(data.headers);
    }
    catch(e) {
        data.headers = {};
    }

    return data;
}

function sendData(form) {
    let { method, api, path, headers, body } = pickData(form);
    let fetchHeaders;

    try {
        fetchHeaders = JSON.parse(form.dataset.headers);
    }
    catch(e) {}

    return fetch('/fetch', {
        method: 'POST',
        headers: fetchHeaders,
        body: JSON.stringify({ method, api, path, headers, body }),
        credentials: 'same-origin'
    }).then(res => res.text());
}

function toCurl(data) {
    let curlMethod = data.method && data.method !== 'GET' ? ' -X ' + data.method : '';
    let curlContent = `curl${curlMethod} '${data.api_url}${data.path}'`;

    Object.keys(data.headers).forEach(key => {
        curlContent += ` -H '${key}: ${data.headers[key]}'`;
    });

    if (data.body) {
        let body = data.body
            .replace(/\r/g, '\\r')
            .replace(/\n/g, '\\n')
            .replace(/\t/g, '\\t');
        curlContent += ` -d '${body}'`;
    }

    return curlContent;
}

function refreshHeaders(form) {
    let data = pickData(form);
    let { headers } = data;

    headers['X-Ya-User-Ticket'] = data.user_ticket;
    headers['X-Ya-Service-Ticket'] = data.service_ticket;

    form.querySelector('textarea[name="headers"]')
        .value = JSON.stringify(headers, null, 2);
}

function update(form) {
    form.querySelector('[data-id="curl"]')
        .textContent = toCurl(pickData(form));
}

const MODIFYING_METHODS = ['POST', 'PATCH', 'PUT', 'DELETE'];

function render(form) {
    if (!form || form.dataset.activated)
        return;

    let updateForm = () => update(form);

    form.querySelector('select[name="method"]')
        .addEventListener('change', ({ target: { value } }) => {
            dom.toggleAttribute(
                form.querySelector('[data-id="body"]'),
                'hidden',
                !MODIFYING_METHODS.includes(value)
            );
            updateForm();
        });

    form.querySelector('select[name="api"]')
        .addEventListener('change', () => {
            refreshHeaders(form);
            updateForm();
        });

    form.querySelector('input[name="path"]')
        .addEventListener('input', updateForm);

    form.querySelector('textarea[name="body"]')
        .addEventListener('input', updateForm);

    form.querySelector('textarea[name="headers"]')
        .addEventListener('input', updateForm);

    form.querySelector('[data-id="edit_headers"]')
        .addEventListener('click', () => {
            let headersElement = form.querySelector('[data-id="headers"]');

            dom.toggleAttribute(
                headersElement,
                'hidden',
                headersElement.getAttribute('hidden') === null
            );
        });

    form.addEventListener('submit', event => {
        event.preventDefault();

        let responseContainer = form.querySelector('[data-id="response"]');
        responseContainer.removeAttribute('hidden');

        let responseField = responseContainer.querySelector('textarea')
        responseField.value = 'Loading...';

        sendData(form)
            .then(data => {
                try { data = JSON.stringify(JSON.parse(data), null, 2); }
                catch(e) {};
                responseField.value = data;
            })
            .catch(() => {
                responseField.value = 'Failed';
            });
    });

    refreshHeaders(form);
    update(form);

    form.dataset.activated = true;
}

render(document.querySelector('form'));