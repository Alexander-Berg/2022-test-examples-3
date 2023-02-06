window['hermione-jserrors'] = [];
window.addEventListener('error', function (e) {
    /* Приводим ошибки к общему виду */
    var message = e.message;
    var stack = e.error && e.error.stack || e.stack || '';

    window['hermione-jserrors'].push({ stack: stack, message: message });
});
