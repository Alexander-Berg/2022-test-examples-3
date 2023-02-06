const testingCsp = require('./csp/testing');

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'img-src': ['avatars.mds.yandex.net'],
            'frame-src': ['https://www.youtube.com/'],
            'connect-src': ['api.profi.yandex.net'],
        },
        presets: testingCsp,
    },
};
