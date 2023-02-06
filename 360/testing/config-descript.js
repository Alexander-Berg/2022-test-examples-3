// Переопределения для `testing`.
module.exports = {
    environment: 'testing',

    urlBlackbox: 'https://pass-test.yandex.ru/blackbox/?',

    // Аватарки
    hostAvatar: 'https://avatars.mdst.yandex.net',
    urlAvatar: 'https://avatars.mdst.yandex.net/get-yapic/',

    hostMPFS: 'https://mpfs-stable.dst.yandex.net',
    // для теста ксивы
    //hostMPFS: 'https://mpfs02d.dst.yandex.net',

    hostRatelimiter: 'http://ratelimiter.dst.yandex.net:1880',

    hostSMS: 'http://phone-passport-test.yandex.ru',

    hostInternalCloudAPI: 'https://api-stable.dst.yandex.net:8443/v1/',

    hostDirectory: 'https://api-internal-test.directory.ws.yandex.net',

    hostSearch: 'http://disksearch-test.n.yandex-team.ru:80',

    hostDjfsAlbums: 'https://djfs-albums-stable.qloud.dst.yandex.net',

    hostPsBilling: 'https://ps-billing-web.qloud.dst.yandex.net',

    pathNarod: '/lnarod',

    bunker: {
        api: 'http://bunker-api-dot.yandex.net',
        version: 'latest'
    },

    /**
     * API внутренней авторизации по токену
     *
     * @see https://wiki.yandex-team.ru/oauth/api
     */
    oauthInternalHost: 'oauth-test-internal.yandex.ru',

    domains: require('./domains.js')
};
