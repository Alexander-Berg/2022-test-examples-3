'use strict';

const BaseConfig = require('./base-config.js');

class Config extends BaseConfig {
    _getUrls() {
        return Object.assign({}, super._getUrls(), {
            webattach: 'https://retriever-test.mail.yandex.net/message_part_real/'
        });
    }
}

module.exports = Config;
