module.exports = function ywAppendStyle(css) {
    return this
        .waitForVisible('#root', 5000)
        .executeAsync((css, done) => {
            const style = document.createElement('style');

            style.setAttribute('type', 'text/css');
            style.appendChild(document.createTextNode(css));

            document.head.appendChild(style);

            requestAnimationFrame(done);
        }, css);
};
