'use strict';

const BaseClientConfig = require('./base.js');

class TestingClientConfig extends BaseClientConfig {
    fillUrlsInfo() {
        super.fillUrlsInfo();
        Object.assign(this._config, {
            'social-host': 'https://social-test.yandex.ru',
            'tavern-url': 'https://tavern-testing.mail.yandex.ru'
        });
    }
}

module.exports = TestingClientConfig;
