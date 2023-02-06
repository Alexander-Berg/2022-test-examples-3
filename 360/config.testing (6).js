'use strict';

const mailLib = require('@ps-int/mail-lib');
const BaseConfig = require('./base.js');
const localServiceConfigs = mailLib.serviceConfigs.testing(require('./services/testing'));

class TestingConfig extends BaseConfig {
    constructor(core) {
        super(core);

        this.sids = {
            telemost: '121',
            promoteMail360: '122',
            corp: '669'
        };
        this.ENVIRONMENT_NAME = 'testing';
    }
}

TestingConfig.localServiceConfigs = localServiceConfigs;

module.exports = TestingConfig;
