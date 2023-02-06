'use strict';

/**
 * @description
 * Это testing конфиг для БП.
 *
 * @type {Object.<String>}
 */
module.exports = Object.assign({}, require('./urls.production.js'), {
    akita: 'https://akita-test.mail.yandex.net',
    furita: 'http://furita-test.mail.yandex.net',
    meta: 'https://meta-test.mail.yandex.net'
});
