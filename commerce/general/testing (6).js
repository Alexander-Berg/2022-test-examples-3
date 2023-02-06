'use strict';
module.exports = {
    api: {
        host: 'http://expert-api-testing.commerce-int.yandex.ru'
    },

    blackbox: {
        api: 'blackbox-mimino.yandex.net'
    },

    bunker: {
        api: 'http://bunker-api-dot.yandex.net/v1',
        version: 'latest',
        updateInterval: 30000
    },

    host: 'https://l7test.yandex.',

    tldForExamSlugs: {
        vip: 'ru',
        simple: 'ru',
        'hello-pro': 'ru',
        'simple-en': 'com',
        'simple-cn': 'com'
    },

    specificExamLangs: {
        'simple-cn': 'zh-Hans'
    },

    examSlugToServiceCode: {
        vip: 'direct',
        simple: 'direct',
        'hello-pro': 'direct',
        'simple-en': 'direct',
        'simple-cn': 'direct'
    },

    logger: {
        excludedServices: ['uatraits', 'blackbox', 'bunker', 'localhost']
    },

    proctoringIframeUrl: {
        host: 'proctoring-no-cookie.commerce-int.yandex.net'
    }
};
