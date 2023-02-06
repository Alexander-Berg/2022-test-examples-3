'use strict';

module.exports = {
    url: 'https://akita-test.mail.yandex.net:443',
    methods: {
        default: {
            dnsCache: true,
            timeout: 400,
            retryOnTimeout: 1
        }
    }
};
