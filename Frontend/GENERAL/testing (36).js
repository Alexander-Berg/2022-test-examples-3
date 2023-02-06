const testingCsp = require('./csp/testing');

const apiURL = new URL(process.env.API_URL);

module.exports = {
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    csp: {
        policies: {
            'img-src': [
                'data:',
                process.env.API_FILE_STORAGE,
            ],
            'font-src': [
                'data:',
            ],
            'media-src': [
                process.env.API_FILE_STORAGE,
            ],
            'frame-src': [
                'www.youtube.com',
            ],
            'connect-src': [
                apiURL.host,
            ],
        },
        presets: testingCsp,
    },
};
