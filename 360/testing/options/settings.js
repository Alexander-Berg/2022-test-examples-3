'use strict';

module.exports = {
    url: 'https://settings-test.mail.yandex.net:443',
    methods: {
        'default': {
            timeout: 1000,
            retryOnTimeout: 1
        },
        '/get': {
            timeout: 500,
            retryOnTimeout: 1
        },
        '/get-validation': {
            timeout: 1000,
            retryOnTimeout: 1
        },
        '/update': {
            timeout: 2000,
            retryOnTimeout: 1
        }
    }
};
