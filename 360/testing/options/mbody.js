'use strict';

module.exports = {
    url: 'https://mbody-test.mail.yandex.net:443',
    methods: {
        default: {
            dnsCache: true,
            timeout: 5000,
            retryOnTimeout: 1
        }
    }
};
