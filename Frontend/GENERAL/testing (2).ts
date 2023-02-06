import { AppConfig } from 'yandex-cfg';

import testingCsp from './csp/testing';

const config: AppConfig = {
    blackbox: {
        api: 'blackbox-mimino.yandex.net'
    },<% if (shouldAddBunker) { %>

    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest'
    },<% } %>

    csp: {
        presets: testingCsp
    },

    httpGeobase: {
        server: 'http://geobase-test.qloud.yandex.ru'
    },

    httpLangdetect: {
        server: 'http://langdetect-test.qloud.yandex.ru'
    },

    httpUatraits: {
        server: 'http://uatraits-test.qloud.yandex.ru'
    },

    static: {
        baseUrl: '/static',
        frozenPath: '/_',
        version: ''
    },

    tvm: {
        serverUrl: 'http://localhost:1'
    }
};

module.exports = config;
