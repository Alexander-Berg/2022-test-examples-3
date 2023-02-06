/* global it */

let sms = require('..');
let assert = require('assert');

it('should return error without langdetect', function(done) {
    sms()({}, {}, function(err) {
        assert.ok(/express-langdetect/.test(err.message));
        done();
    });
});

it('should return error without ip', function(done) {
    sms()({ langdetect: 'wow' }, {}, function(err) {
        assert.ok(/trust proxy/.test(err.message));
        done();
    });
});

it('should add sendlinksms to req', function(done) {
    let req = { langdetect: { id: 'ru' }, ip: '87.250.248.242' };
    sms()(req, {}, function(err) {
        assert.ok(!err);
        assert.ok(req.sendlinksms);
        done();
    });
});
