const productionCfg = require('./index.production');

module.exports = Object.assign(productionCfg, {
    blackbox: {
        host: 'pass-test.yandex.ru',
        path: '/blackbox'
    },
    intapi: {
        host: 'cloud-api.dst.yandex.net',
        protocol: 'https:',
        port: 8443,
        path: '/intapi'
    }
});
