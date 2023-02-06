'use strict';

const mockRequire = require('mock-require');

const secrets = {
    ckeyHmacKey: 'XXX',
    composeFieldHmacKey: 'XXX',
    mailAttachmentsSaveToDiskSalt: 'XXX',
    mailSignatureImagesSalt: 'XXX',
    cryproxPartnerToken: 'XXX',
    callbackJsonKey: [ 'key1', 'key2' ],
    encryptMetrikaExpsKey: 'XXX',
    xivaQuinnToken: {
        production: 'XXX',
        corp: 'XXX'
    },
    sender: {
        YandexMail: {
            maillistPromo: {
                id: 'XXX',
                key: 'XXX'
            }
        }
    }
};

mockRequire('../../secrets.js', secrets);
