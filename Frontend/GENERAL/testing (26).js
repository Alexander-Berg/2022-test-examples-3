const { SELF } = require('@yandex-int/express-yandex-csp');
const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'img-src': ['avatars.yandex.net'],
            'media-src': [SELF, 'yastatic.net'],
        },
        presets: testingCsp,
    },
};
