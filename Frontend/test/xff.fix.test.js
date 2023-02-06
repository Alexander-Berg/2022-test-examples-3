/* global it */

let assert = require('assert');
let fix = require('..');
let Netmask = require('netmask').Netmask;
let trash = require('./trash-examples.js');

describe('express-x-forwarded-for-fix', function() {
    it('should filter trash', function() {
        trash.forEach(function(ip) {
            let req = { headers: { 'x-forwarded-for': ip } };
            fix()(req, {}, function() {
                assert.ok(undefined === req.headers['x-forwarded-for']);
            });
        });
    });

    it('should filter joined trash', function() {
        let req = { headers: { 'x-forwarded-for': trash.join(',') } };
        fix()(req, {}, function() {
            assert.ok(undefined === req.headers['x-forwarded-for']);
        });
    });

    it('should set defaultIP, if all filtered', function() {
        let req = { headers: { 'x-forwarded-for': trash.join(',') } };
        fix({ defaultIP: '127.0.0.1' })(req, {}, function() {
            assert.equal(req.headers['x-forwarded-for'], '127.0.0.1');
        });
    });

    it('should not set defaultIP, if not all filtered', function() {
        let req = { headers: { 'x-forwarded-for': trash.join(',') + ',87.250.248.136' } };
        fix({ defaultIP: '127.0.0.1' })(req, {}, function() {
            assert.equal(req.headers['x-forwarded-for'], '87.250.248.136');
        });
    });

    it('should work without x-forwarded-for header', function() {
        fix()({}, {}, function() {});
    });

    it('should filter reserved IPv4 entries wrapped in IPv6', function() {
        let req = { headers: { 'x-forwarded-for': '::ffff:192.168.0.1, 77.88.21.11' } };
        fix({ defaultIP: '127.0.0.1' })(req, {}, function() {
            assert.equal(req.headers['x-forwarded-for'], '77.88.21.11');
        });
    });

    it('should replace x-forwarded-for with x-forwarded-for-y', function() {
        let req = { headers: { 'x-forwarded-for': '::ffff:192.168.0.1, 77.88.21.11', 'x-forwarded-for-y': '79.172.59.35' } };
        fix()(req, {}, function() {
            assert.equal(req.headers['x-forwarded-for'], '79.172.59.35');
        });
    });

    it('should allow IP from allowedNetMasks', function() {
        let req = { headers: { 'x-forwarded-for': trash.join(',') + ',198.18.11.22' } };
        fix({ allowedNetMasks: ['198.18.0.0/15'] })(req, {}, function() {
            assert.equal(req.headers['x-forwarded-for'], '198.18.11.22');
        });
    });

    it('should allow Netmask in allowedNetMasks', function() {
        let req = { headers: { 'x-forwarded-for': trash.join(',') + ',198.18.11.22' } };
        fix({ allowedNetMasks: [new Netmask('198.18.0.0/15')] })(req, {}, function() {
            assert.equal(req.headers['x-forwarded-for'], '198.18.11.22');
        });
    });

    it('should throw an error for wrong allowedNetMasks value', function() {
        let req = { headers: { 'x-forwarded-for': trash.join(',') } };
        assert.throws(
            function() {
                fix({ allowedNetMasks: ['not a mask'] })(req, {}, function() {});
            },
            function(error) {
                return error.message === 'Invalid net address: not a mask';
            }
        );
    });
});
