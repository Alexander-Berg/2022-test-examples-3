const { SELF } = require('@yandex-int/express-yandex-csp');
const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'script-src': ['yastatic.net', 'mc.yandex.ru', 'www.youtube.com', 'youtube.com', 'broadcast.comdi.com'],
            'img-src': ['data:', 'yastatic.net', 'avatars.mds.yandex.net'],
            'media-src': [SELF, 'yastatic.net'],
            'frame-src': ['yastatic.net', 'youtube.com', 'www.youtube.com', 'broadcast.comdi.com'],
            'font-src': ['data:'],
            'connect-src': ['yastatic.net'],
        },
        presets: testingCsp,
    },
};
