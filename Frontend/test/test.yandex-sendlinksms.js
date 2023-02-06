/* global it, describe */

let SendLinkSms = require('..');

describe('yandex-sendlinksms', function() {
    it('should send sms with app code and figure out hostname if not passed', function(done) {
        let sms = new SendLinkSms();

        let query = {
            app: 'maps',
            remote_ip: '87.250.248.242',
            phone_full: '+79090115050',
            lang: 'ru',
        };

        sms.send(query).then(function(status) {
            if (status === 'ok') {
                done();
            } else {
                done(status);
            }
        }, done);
    });

    it('return status error if bad phone', function(done) {
        let sms = new SendLinkSms();

        let query = {
            app: 'maps',
            remote_ip: '87.250.248.242',
            phone_full: '+7909',
            lang: 'ru',
        };

        sms.send(query).then(function(status) {
            if (status === 'failed') {
                done();
            } else {
                done(status);
            }
        }, done);
    });
});
