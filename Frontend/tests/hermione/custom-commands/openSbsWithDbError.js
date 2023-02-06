const { withFakeLoginAndDbError } = require('./utils');

module.exports = function(url, login) {
    return this.url(withFakeLoginAndDbError(url, login));
};
