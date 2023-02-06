blocks['i-global__content'].push('i-global__config');

blocks['i-global__config'] = function(data) {
    const staticHost = 'borschik:include:../../../.static_host';

    const staticOrigin = staticHost.indexOf('serp-static-testing') !== -1 ?
        '//serp-static-testing.s3.yandex.net' :
        '//yastatic.net';

    data.config = {
        name: 'testing',
        staticVersion: 'web4',
        staticOrigin: staticOrigin,
        staticHost: staticHost,
        staticProxyPath: '/yastatic' + 'borschik:include:../../../.static_path'
    };
};
