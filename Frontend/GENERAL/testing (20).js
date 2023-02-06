const { INLINE } = require('@yandex-int/express-yandex-csp');
const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'script-src': ['yastatic.net', 'mc.yandex.ru'],
            'img-src': ['yastatic.net', 'avatars.mds.yandex.net'],
            'connect-src': ['yastatic.net'],
            'style-src': [INLINE],
        },
        presets: testingCsp,
    },
};
