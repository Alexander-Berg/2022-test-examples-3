blocks['i-global__content'].push('i-global__config');

blocks['i-global__config'] = function(data) {
    data.config = {};
    data.config.name = 'testing';
    data.config.staticVersion = '';

    var staticHost = 'borschik:include:../../../.static_host';

    // Для обратной совместимости и возможности переключения через genisys
    // Оторвать после закрытия FEI-10574
    if (staticHost.indexOf('serp-static-testing') !== -1) {
        data.config.staticOrigin = '//serp-static-testing.s3.yandex.net';
    } else {
        data.config.staticOrigin = '//yastatic.net';
    }

    data.config.staticHost = staticHost;
};
