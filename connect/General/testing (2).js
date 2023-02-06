module.exports = {
    //  https://l7test.yandex.ru
    app: {
        changeLanguage: 'https://l7test.yandex.ru/portal/set/lang/?intl=${intl}&retpath=${retpath}&sk=${sk}',
    },
    api: {
        blackbox: 'https://pass-test.yandex.ru/blackbox',
        directory: 'https://api-internal-test.directory.ws.yandex.net',
        gendarme: 'https://test.gendarme.mail.yandex.net',
        dns: 'https://test.dns-hosting.ws.yandex.net/api/v1',
        setter: 'https://test.setter.mail.yandex.net',
        fouras: 'https://test.fouras.mail.yandex.net',
        settings: 'https://settings-test.ws.yandex.ru',
        sender: 'https://test.sender.yandex-team.ru',
    },
    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
    },
    ui: {
        avatar: {
            host: 'https://avatars.mdst.yandex.net',
            url: 'https://avatars.mdst.yandex.net/get-yapic/${id}/islands-${size}',
        },
        serviceActions: {
            wiki: 'https://wiki.test.yandex.ru/homepage/.create',
            forms: 'https://forms.test.yandex.ru/admin/myforms/drafts',
            tracker: 'https://tracker.test.yandex.ru/createTicket',
            staff: 'https://team.test.yandex.ru/${nickname}?org_id=${org_id}&uid=${uid}',
        },
        serviceSettings: {
            disk: 'https://admin-testing.yandex.ru/disk?org_id=${org_id}&uid=${uid}',
        },
        relatedServices: {
            calendar: 'https://calendar.yandex.ru',
            contacts: 'https://mail.yandex.ru/#contacts',
            mail: 'https://mail.yandex.ru/?uid=${uid}',
            passport: 'https://passport-test.yandex.ru',
            wiki: 'https://wiki.test.yandex.ru',
            tracker: 'https://tracker.test.yandex.ru',
            disk: 'https://disk.yandex.ru',
            forms: 'https://forms.test.yandex.ru/b2b/admin/',
        },
        privateYambChat: 'https://yamb-test.ws.yandex.ru/user/${nickname}',
        staff: 'https://team.test.yandex.ru/${nickname}?org_id=${org_id}&uid=${uid}',
        promo: '/fakepromo',
    },
    passport: {
        host: 'https://passport-test.yandex.${tld}',
        accounts: 'https://api.passport-test.yandex.${tld}/all_accounts',
        session: {
            login: 'https://passport-test.yandex.ru/auth?retpath=${retpath}',
            logout: 'https://passport-test.yandex.ru/passport?mode=embeddedauth&action=logout&uid=${uid}&yu=${yu}&retpath=${retpath}', // eslint-disable-line max-len
            change: 'https://passport-test.yandex.ru/passport?mode=embeddedauth&action=change_default&uid=${uid}&yu=${yu}&retpath=${retpath}', // eslint-disable-line max-len
            add: 'https://passport-test.yandex.ru/auth?mode=add-user&retpath=${retpath}',
            update: 'https://passport-test.yandex.ru/auth/update/?retpath=${retpath}',
            passwordChange: 'https://passport-test.yandex.ru/passport?mode=changepass&retpath=${retpath}',
            confirmEmail: 'https://passport-test.yandex.ru/passport?mode=changeemails',
            approveUser: 'https://passport-test.yandex.ru/passport?mode=userapprove&retpath=${retpath}',
            upgradeUser: 'https://passport-test.yandex.${tld}/profile/upgrade?origin=connect&retpath=${retpath}',
        },
        org: {
            registration: 'https://passport-test.yandex.${tld}/registration/connect?origin=${origin}&retpath=${retpath}',
        },
    },
    org: {
        list: 'https://api-test.directory.yandex.${tld}/v6/organizations-by-session',
    },
    metrika: {
        default: 37738550,
        setup: 37738550,
        general: 49300198,
    },
    csp: {
        extra: {
            'img-src': 'yapic-test.yandex.ru avatars.mdst.yandex.net',
            'connect-src': [
                'connect-test.ws.yandex.ru',
                'connect-test.ws.yandex.com',
                'api.passport-test.yandex.ru',
                'api.passport-test.yandex.com',
                'api-test.directory.yandex.ru',
                'api-test.directory.yandex.com',
            ].join(' '),
            'frame-ancestors': '*',
        },
    },
    sender: {
        newsList: {
            service: 'ya.connect',
            listId: 'OYON5K23-63U1',
        },
    },
};
