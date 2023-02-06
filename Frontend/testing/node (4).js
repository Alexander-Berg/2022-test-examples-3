var sitesearchViewerBackHost = 'sitesearch-viewer.test.n.yandex-team.ru',
    sitesearchViewerBackPort = 80,
    sitesearchSupportBackHost = 'sitesearch-support.test.n.yandex-team.ru',
    sitesearchSupportBackPort = 80,
    sitesearchViewerBackHandles = require('../sitesearchViewerBackHandles.js'),
    sitesearchSupportBackHandles = require('../sitesearchSupportBackHandles.js'),
    CoreConfig = require('../common'),
    userConfig = require('../commonUserConfig'),
    extend = require('extend'),
    config = {
        env: 'testing',

        debug: false,

        support: false,

        staticHost: {
            desktop: '//yastatic.net/s3/webmaster/sitesearch/{{DEBIAN_VERSION}}/common/',
            form: '//site.betastatic.yandex.net/',
            catalogResults: '//site.test.yandex.ru/search/site/catalog/'
        },

        passportInternalHost: 'passport-internal.yandex.ru',

        fullSupportedDomains: [ 'ru', 'ua' ],

        servant: {
            licenseText: {
                host: 'help.doccenter-test.yandex.ru'
            },

            'support/blackbox-userinfo': {
                handle: 'support/blackbox-userinfo',
                host: 'blackbox-mimino.yandex.net',
                tvm_dst: 239,
            },
            'blackbox/sessionId': { handle: 'blackbox/sessionId', host: 'blackbox-mimino.yandex.net', tvm_dst: 239 },
        },

        TVM: { CLIENT_ID: 2000286 },
    };

config.support = require('../support');

config.user = userConfig;

sitesearchViewerBackHandles.forEach(function(handle) {
    config.servant[handle] = {
        host: sitesearchViewerBackHost,
        port: sitesearchViewerBackPort,
        agent: false
    };
});

if (config.support) {
    sitesearchSupportBackHandles.forEach(function(handle) {
        config.servant[handle] = {
            host: sitesearchSupportBackHost,
            port: sitesearchSupportBackPort,
            agent: false
        };
    });

    config.staticHost.desktop = '//yastatic.net/s3/webmaster/sitesearch-support/{{DEBIAN_VERSION}}/common/';
}

module.exports = extend(true, CoreConfig, config);
