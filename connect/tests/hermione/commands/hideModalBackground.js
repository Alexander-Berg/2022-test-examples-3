module.exports = function hideModalBackground() {
    return this.execute(() => {
        const style = document.createElement('style');

        style.setAttribute('nonce', window.ya.connect.initial.nonce);
        style.textContent = '.modal_theme_normal { background: #ddd !important; }';

        document.head.appendChild(style);
    }, ...arguments);
};
