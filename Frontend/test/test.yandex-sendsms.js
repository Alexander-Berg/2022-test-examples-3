/* global describe, it */
var yasms = require('..');

describe('Send SMS', function() {
    // TODO: Restore this test
    it.skip('should send sms and figure out hostname if not passed', function(done) {
        var sms = yasms();

        var data = {
            sender: 'promopages',
            identity: 'mobile',
            text: 'yandex-sendsms тест',
            phone: '+79122022659',
        };

        sms.sendsms(data)
            .then(function() {
                done();
            }, done);
    });
});
