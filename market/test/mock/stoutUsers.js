const User = require('nodules-user');

const mockConfig = require('./config');
const mockRequests = require('./requests');
const mockSettings = require('./settings');

function StoutUser(request, config, response, settings) {
    this.__request = request;
    this.response = response || {};
    this.config = config || mockConfig;
    this.user = new User(this.__request, this.response, this.config);
    this.settings = settings || mockSettings.DEFAULT;
    this.authHost = {
        replace() {},
    };
}

module.exports = {
    DEFAULT_USER: new StoutUser(mockRequests.DEFAULT),
    KIEV_LR_USER: new StoutUser(mockRequests.WITH_KIEV_LR),
    KIEV_CR_USER: new StoutUser(mockRequests.DEFAULT, undefined, undefined, mockSettings.KIEV_CR),
    UA_HOSTNAME_ONLY: new StoutUser(mockRequests.UA_HOSTNAME_ONLY),
    NON_KUBR: new StoutUser(mockRequests.NON_KUBR),
    UNEXISTING_LR: new StoutUser(mockRequests.UNEXISTING_LR),
    RU_LR_WITH_UA_TLD: new StoutUser(mockRequests.RU_LR_WITH_UA_TLD),
    USER_WITH_IP: new StoutUser(mockRequests.WITH_IP),
};
