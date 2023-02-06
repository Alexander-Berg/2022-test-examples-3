module.exports = {
    blackbox: {
        host: 'pass-test.yandex.ru',
        path: '/blackbox',
    },
    services: {
        'ps-billing': 'https://ps-billing-web.qloud.dst.yandex.net',
        'cloud-api': 'https://cloud-api.dst.yandex.net:8443',
        mpfs: 'https://mpfs-stable.dst.yandex.net',
    },
    passport: (yandexDomain) => ({
        passportAuth: `https://passport-test.${yandexDomain}/auth?from=tuning`,
        passportAuthUpdate: `https://passport-test.${yandexDomain}/auth/update`,
        passportProfileUpgrade: `https://passport-test.${yandexDomain}/profile/upgrade`,
    }),
    getPassportAccountsUrl: (tld) =>
        `https://api.passport-test.yandex.${tld}/all_accounts`,
    avatarsOrigin: 'https://avatars.mdst.yandex.net',
    getWebApiOrigin: (tld) => `https://web-tst-stable.mail.yandex.${tld}`,
};
