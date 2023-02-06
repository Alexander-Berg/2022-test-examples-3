const { withFakeLogin } = require('./utils');

module.exports = function(url, login) {
    return this.url(withFakeLogin(url, login));
};
