module.exports = function(selector, expectedURL, params, query) {
    return this
        .getAttribute(selector, 'href')
        .then(url => this.yaCheckUrl(url, expectedURL, params, query));
};
