'use strict';

const {createConfig} = require('../ginny');

module.exports = createConfig({
    baseUrl: 'https://default.aflt-exp-testing.fslb.yandex.ru',

    httpTimeout: 30000,

    sessionRequestTimeout: 60000,
    sessionQuitTimeout: 5000,
});
