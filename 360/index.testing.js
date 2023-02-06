const config = require('./index.prestable');

module.exports = Object.assign({}, config, {
    blackbox: {
        host: 'pass-test.yandex.ru',
        path: '/blackbox'
    }
});
