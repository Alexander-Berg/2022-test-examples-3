module.exports = {
    ui: {
        serviceActions: {
            wiki: 'https://wiki.test.yandex.com/homepage/.create',
            forms: 'https://forms.test.yandex.com/admin/myforms/drafts',
            tracker: 'https://tracker.test.yandex.com/createTicket',
        },
        relatedServices: {
            calendar: 'https://calendar.yandex.com',
            contacts: 'https://mail.yandex.com/#contacts',
            disk: 'https://disk.yandex.com',
            mail: 'https://mail.yandex.com/?uid=${uid}',
            passport: 'https://passport-test.yandex.com',
            wiki: 'https://wiki.test.yandex.com',
            forms: 'https://forms.test.yandex.com/b2b/admin/',
            yamb: 'https://yamb.yandex.com',
        },
        privateYambChat: 'https://yamb.yandex.com/user/${nickname}',
        staff: 'https://team.test.yandex.com/${nickname}?org_id=${org_id}&uid=${uid}',
    },
    passport: {
        session: {
            login: 'https://passport-test.yandex.com/auth?retpath=${retpath}',
            logout: 'https://passport-test.yandex.com/passport?mode=embeddedauth&action=logout&uid=${uid}&yu=${yu}&retpath=${retpath}', // eslint-disable-line max-len
            change: 'https://passport-test.yandex.com/passport?mode=embeddedauth&action=change_default&uid=${uid}&yu=${yu}&retpath=${retpath}', // eslint-disable-line max-len
            add: 'https://passport-test.yandex.com/auth?mode=add-user&retpath=${retpath}',
            update: 'https://passport-test.yandex.com/auth/update/?retpath=${retpath}',
            passwordChange: 'https://passport-test.yandex.com/passport?mode=changepass&retpath=${retpath}',
            confirmEmail: 'https://passport-test.yandex.com/passport?mode=changeemails',
            approveUser: 'https://passport-test.yandex.com/passport?mode=userapprove&retpath=${retpath}',
        },
    },
    metrika: {
        default: 37738550,
    },
    csp: {
        extra: {
            'img-src': 'yapic-test.yandex.ru yapic-test.yandex.com avatars.mdst.yandex.net',
            'frame-ancestors': '*',
        },
    },
};
