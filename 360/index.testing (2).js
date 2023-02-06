const productionCfg = require('./index.production');

module.exports = Object.assign(productionCfg, {

    avatarsOrigin: 'https://avatars.mdst.yandex.net',
    yapicOrigin: 'https://yapic-test.yandex.ru',

    blackbox: {
        host: 'pass-test.yandex.ru',
        path: '/blackbox'
    },

    clicker: {
        protocol: 'https:',
        host: 'clck.deploy.dsp.yandex.net'
    },

    mpfs: {
        protocol: 'https:',
        // NOTE: хост по-умолчанию (обычно смотрит на mpfs-stable)
        host: 'mpfs.dst.yandex.net'
        // NOTE: стабильный тестовый хост
        // host: 'mpfs-stable.dst.yandex.net'
        // NOTE: близнец mpfs-stable https://c.yandex-team.ru/groups/disk_test_mpfs-stable
        // host: 'mpfs03f.dst.yandex.net'
        // NOTE: Самый свежий MPFS (может быть нестабильным)
        // host: 'mpfs-current.dst.yandex.net'
    },

    intapi: {
        host: 'api-stable.dst.yandex.net',
        port: 8443,
        protocol: 'https:'
    },

    directoryApi: {
        protocol: 'https:',
        host: 'api-internal-test.directory.ws.yandex.net'
    },

    iosAppMetrikaParams: {
        appId: '2216086',
        trackId: '458932812687381399'
    }
});
