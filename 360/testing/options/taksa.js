'use strict';

module.exports = {
    url: 'http://taksa-test.search.yandex.net:80',
    methods: {
        default: {
            retryOnTimeout: 1,
            timeout: 200,
            dnsCache: false
        }
    }
};
