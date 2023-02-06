'use strict';

const Config = require('./base.js');
const localServiceConfigs = require('@ps-int/mail-lib').serviceConfigs.testing();

class TestingConfig extends Config {
    constructor(core) {
        super(core);

        var IS_CORP = this.HOST.indexOf('yandex-team') > -1;

        this.IS_CORP = IS_CORP;
        this.IS_DEV = true;
        this.IS_PROD = !IS_CORP;

        this.IS_FRONTDEV_HOST = /\.mailfront\d*\.yandex\./.test(this.HOST);

        /*
         БУДЬ МУЖИКОМ!
         1) Пиши конфиги по алвавиту!!!
         2) Не ставь слеш в конце! Нагляднее писать config.services.wmi + '/api/request'
         */

        this.clientConfig['docviewer-frontend-host'] = 'http://docviewer-qa.yandex.ru';
        this.clientConfig['social-avatars-url'] = '//betastatic.yastatic.net/mail/socialavatars/socialavatars';
        this.clientConfig['mail-url'] = `https://${this.HOST}`;

        this.secrets = this.getSecrets('testing');
    }

    _getResources() {
        const staticHost = 'https://' + (this.IS_FRONTDEV_HOST ?
            this.HOST.replace(/\.yandex\..*?$/, '.yandex.net') : this.HOST);

        return {
            ...super._getResources(),
            staticHost,
            webattach: 'https://retriever-test.mail.yandex.net/message_part_real/'
        };
    }
}

// Переопределяем константы

Config.VERSION = {touch: 'node'};

TestingConfig.localServiceConfigs = localServiceConfigs;

module.exports = TestingConfig;
