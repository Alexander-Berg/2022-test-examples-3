module.exports = function disableAnimations() {
    return this.execute(selector => {
        const style = document.createElement('style');

        style.setAttribute('nonce', window.ya.connect.initial.nonce);
        style.textContent = `${selector}, ${selector}:before, ${selector}:after
        {
            transition-property: none !important;
            -webkit-transition-property: none !important;
            transition-duration: 1ms !important;
            transition-delay: 0s !important;
            animation-duration: 1ms !important;
        }`;

        document.head.appendChild(style);
    }, ...arguments);
};
