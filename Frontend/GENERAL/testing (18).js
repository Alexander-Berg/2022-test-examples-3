const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {},
        presets: testingCsp,
    },
};
