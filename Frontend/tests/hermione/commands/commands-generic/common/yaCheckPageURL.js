module.exports = function(expectedURL, message, options) {
    return this
        .getUrl()
        .then(url => {
            return this
                .yaCheckURL(url, `/searchapp${expectedURL}`, message, options);
        });
};
