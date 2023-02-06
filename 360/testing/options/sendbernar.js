'use strict';

module.exports = {
    url: 'https://sendbernar-test.mail.yandex.net:443',
    methods: {
        default: {
            dnsCache: true,
            timeout: 30000,
            retryOnTimeout: 0,
            retryOnUnavailable: 0,
            logPostArgs: false
        },
        write_attachment: {
            dnsCache: true,
            timeout: 30000,
            retryOnTimeout: 1,
            retryOnUnavailable: 0,
            logPostArgs: false
        }
    }
};
