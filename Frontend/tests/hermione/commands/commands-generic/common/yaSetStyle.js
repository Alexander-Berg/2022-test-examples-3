module.exports = function(selector, css) {
    return this.execute(function(selector, css) {
        const element = document.querySelector(selector);
        if (element) element.style = css;
    }, selector, css);
};
