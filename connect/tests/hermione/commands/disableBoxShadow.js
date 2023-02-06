module.exports = function disableBoxShadow() {
    return this.execute(selector => {
        const style = document.createElement('style');

        style.setAttribute('nonce', window.ya.connect.initial.nonce);
        style.textContent = `${selector}
        {
            box-shadow: none !important;
        }`;

        document.head.appendChild(style);
    }, ...arguments);
};
