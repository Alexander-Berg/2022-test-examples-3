module.exports = function(expectedURL, params, query) {
    return this
        .getUrl()
        .then(url => this.yaCheckUrl(url, expectedURL, params, query));
};
