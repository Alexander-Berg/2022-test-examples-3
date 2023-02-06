var CommonConfig = require('../common');

var Config = CommonConfig.create()
    .setEnv('testing')
    .setDebug(false)
    .setStaticHost('desktop', '//yastatic.net/s3/webmaster/catalog/{{DEBIAN_VERSION}}/desktop/')
    .setStaticHost('touch', '//yastatic.net/s3/webmaster/catalog/{{DEBIAN_VERSION}}/touch/')
    .setBackend('wmctest-back01g.search.yandex.net', 33585)
    .setServantHandles(
        { handle: 'catalog', host: 'hamster.yandex.ru' },
        { handle: 'help-api', host: 'help.doccenter-test.yandex.ru' },
        { handle: 'passport', host: 'blackbox-mimino.yandex.net', tvm_dst: 239 },
        { handle: 'blackbox/sessionId', host: 'blackbox-mimino.yandex.net', tvm_dst: 239 }
    )
    .set('TVM.CLIENT_ID', 2000286)
    .set('externalStaticHost', 'site.yandex.net');

module.exports = Config;
