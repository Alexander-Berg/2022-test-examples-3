const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'font-src': ['data:'],
            'frame-src': ['forms.yandex.ru', 'youtube.com', 'www.youtube.com', 'frontend.vh.yandex.ru'],
            'script-src': ['yastatic.net'],
            'img-src': ['data:', 'avatars.mds.yandex.net'],
        },
        presets: testingCsp,
    },
};
