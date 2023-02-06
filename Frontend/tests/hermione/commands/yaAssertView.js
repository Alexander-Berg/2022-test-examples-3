module.exports = function(state, selector, options) {
    return this
        .execute(function(s) {
            window.document.querySelector(s).scrollIntoView();
        }, selector)
        .then(function() {
            // FF не выполняет moveToObject, если до этого уже был moveToObject с этими же координатами,
            // при этом, курсор может быть уведен другими деействиями (клики, драгги и т.д)
            // Происходит это если в одном it'e несколько yaAssertView.
            if (this.desiredCapabilities.browserName === 'firefox') {
                return this.moveToObject(selector, 2, 2);
            }
        })
        .moveToObject(selector, 0, 0)
        .assertView(state, selector, options);
};
