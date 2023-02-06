const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'script-src': ['yastatic.net', 'mc.yandex.ru', 'api-maps.yandex.ru', 'core-renderer-tiles.maps.yandex.net'],
            'img-src': ['data:', 'api-maps.yandex.ru', 'core-renderer-tiles.maps.yandex.net', 'yastatic.net'],
            'frame-src': ['yastatic.net', 'forms.yandex.ru', 'lyceum.s3.yandex.net', 'yandexdataschool.ru', 'academy.yandex.ru'],
            'font-src': ['data:'],
            'connect-src': ['yastatic.net'],
            'object-src': ['lyceum.s3.yandex.net'],
        },
        presets: testingCsp,
    },
};
