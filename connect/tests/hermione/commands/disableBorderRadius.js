module.exports = function disableBorderRadius() {
    return this.execute(selector => {
        const style = document.createElement('style');

        style.setAttribute('nonce', window.ya.connect.initial.nonce);
        style.textContent = `${selector}
        {
            border-radius: 0 !important;
        }`;

        document.head.appendChild(style);
    }, ...arguments);
};
