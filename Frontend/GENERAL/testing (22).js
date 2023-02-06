const { SELF, INLINE } = require('@yandex-int/express-yandex-csp');
const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'script-src': ['yastatic.net', 'mc.yandex.ru'],
            'frame-src': ['forms.yandex.com'],
            'connect-src': ['api.contest.yandex.net', 'storage.yandexcloud.net', 'neurips-dev.storage.yandexcloud.net', 'yr-shifts.storage.yandexcloud.net', 'yastatic.net', 'promohr.storage.yandexcloud.net'],
            'img-src': ['avatars.yandex.net'],
            'media-src': [SELF],
            'style-src': [INLINE],
        },
        presets: testingCsp,
    },
};
