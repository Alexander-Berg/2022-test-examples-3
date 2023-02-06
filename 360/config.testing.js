const Config = require('./base.js');

class ConfigTesting extends Config {
    _getUrls() {
        const passportHost = `passport-test.${this.passportDomain}`;
        const passportUrl = `https://${passportHost}`;

        return {
            ...super._getUrls(),
            passport: passportUrl + '/passport',
            passportHost,
            passportPlain: passportUrl,
            webattach: 'https://retriever-test.mail.yandex.net/message_part_real/',
        };
    }
}

module.exports = ConfigTesting;
