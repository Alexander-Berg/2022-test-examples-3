const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'script-src': ['yastatic.net', 'mc.yandex.ru'],
            'img-src': ['data:', 'i.ytimg.com'],
            'frame-src': ['youtube.com', 'www.youtube.com'],
            'font-src': ['data:'],
            'connect-src': ['yastatic.net'],
        },
        presets: testingCsp,
    },
};
