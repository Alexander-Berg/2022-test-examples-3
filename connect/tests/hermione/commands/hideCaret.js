module.exports = function hideCaret() {
    return this.execute(() => {
        const style = document.createElement('style');

        style.setAttribute('nonce', window.ya.connect.initial.nonce);
        style.textContent = '* {caret-color: transparent !important;}';

        document.head.appendChild(style);
    });
};
