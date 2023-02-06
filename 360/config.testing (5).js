'use strict';

const BaseConfig = require('./base.js');
const localServiceConfigs = require('@ps-int/mail-lib').serviceConfigs.testing();

class TestingConfig extends BaseConfig {
    _setEnvVariables() {
        super._setEnvVariables();

        this.IS_QA = true;
    }

    _getUrls() {
        return Object.assign(super._getUrls(), {
            webattach: 'https://retriever-test.mail.yandex.net/message_part_real/'
        });
    }
}

TestingConfig.localServiceConfigs = localServiceConfigs;

module.exports = TestingConfig;
