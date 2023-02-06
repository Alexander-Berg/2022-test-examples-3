'use strict';

module.exports = {
    url: 'https://msearch-proxy-test.search.yandex.net:10431',
    methods: {
        default: {
            dnsCache: true,
            timeout: 10000,
            retryOnTimeout: 1
        },
        searchSuggest: {
            queryParamFast: 200,
            timeoutFast: 500,
            queryParamNormal: 4000,
            timeoutNormal: 5000
        }
    }
};
