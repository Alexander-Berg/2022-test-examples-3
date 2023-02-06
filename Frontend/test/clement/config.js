const { join: pathJoin } = require('path');

const defaultConfig = require('@yandex-int/clement/default-config');

const cacheFilepath = require('./plugins/cache-filepath');
const cacheFilename = require('./plugins/cache-filename');
const cacheKey = require('./plugins/cache-key');
const stringifyQuery = require('./plugins/pre-data-fetch');
const paintImagesOver = require('./plugins/paint-images-over');

module.exports = {
    cachePath: pathJoin(__dirname, 'test-data'),

    plugins: [
        cacheFilepath,
        cacheFilename,
        cacheKey,
        paintImagesOver,
        ...defaultConfig.plugins,
        stringifyQuery,
    ],
    sources: {
        '3333': {
            url: 'https://abc-back.test.yandex-team.ru',
        },
        '3334': {
            url: 'https://dispenser.test.yandex-team.ru',
        },
        '3335': {
            url: 'https://api-stable.dst.yandex-team.ru',
        },
        '3336': {
            url: 'https://st.test.yandex-team.ru',
        },
        '3337': {
            url: 'https://test.bot.yandex-team.ru',
        },
        '3338': {
            url: 'https://staff.test.yandex-team.ru',
        },
        '3339': {
            url: 'https://search-back.test.yandex-team.ru',
        },
        '3340': {
            url: 'https://wfaas.yandex-team.ru',
        },
        '3341': {
            url: 'https://center.yandex-team.ru',
        },
        '3342': {
            url: 'https://d.test.yandex-team.ru',
        },
    },
};
