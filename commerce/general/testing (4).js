const fs = require('fs');

module.exports = {
    achievements: {
        shim: '3189/share_shim',
        hello: '3702/hello_share',
        direct: '3702/direct_cert_preview'
    },

    blackbox: {
        connection: {
            api: 'blackbox-mimino.yandex.net'
        }
    },

    mdsService: {
        write: {
            hostname: 'storage-int.mdst.yandex.net'
        },
        read: {
            hostname: 'storage.mdst.yandex.net'
        }
    },

    avatarsService: {
        write: {
            hostname: 'avatars-int.mdst.yandex.net'
        },
        read: {
            hostname: 'avatars.mdst.yandex.net'
        }
    },

    skipTokenCheck: true,

    postgres: {
        options: {
            dialectOptions: {
                ssl: {
                    ca: fs.readFileSync('/root/.postgresql/root.crt').toString(),
                    sslmode: 'verify-full'
                },
                keepAlive: true
            }
        }
    },

    yt: {
        path: '//home/expert/testing',
        maxPendingTrialAge: 1000 * 60 * 5 // 5 min
    },

    tvm: {
        src: 2001462,
        direct: {
            alias: 'direct-api-testing',
            dsts: 2000693
        }
    },

    sender: {
        host: 'https://test.sender.yandex-team.ru',
        mailIds: {
            correct: 'C9PNW103-QNT1',
            failed: '06NEX103-79I1',
            nullifyCert: '86AE9V43-29S',
            banCert: 'C3QB86B3-DT41'
        }
    },

    frontHostPrefix: 'https://l7test.yandex.',

    self: {
        host: 'expert-api-testing.commerce-int.yandex.ru'
    },

    banDuration: { unit: 'day', count: 1 },

    officeUrl: 'https://expert-office-testing.yandex-team.ru',

    certServices: [
        'market',
        'metrika',
        'direct_en',
        'direct_cn',
        'metrika_en',
        'rsya',
        'publisher',
        'direct_pro',
        'cpm',
        'zen',
        'msp'
    ]
};
