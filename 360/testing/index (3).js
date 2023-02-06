const config = module.exports = require('../common');

Object.assign(config, {
    nodeName: config.nodeName + '@testing',
    _nodeName: config.nodeName,

    mpfs: {
        protocol: 'https:',
        host: 'mpfs-stable.dst.yandex.net',
        amount: config.mpfs.amount,
        yaDiskAPI: 'https://clck.qloud.dsp.yandex.net'
    },

    blackbox: 'pass-test.yandex.ru',

    passportTemplate: 'https://passport-test%s',
    avatarTemplate: 'https://avatars.mdst.yandex.net/get-yapic/%s/islands-middle'
});
