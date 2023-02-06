/* global describe, it */

/**
 * Created by unikoid on 04.02.16.
 */
'use strict';
var expressYaFrameguard = require('./');

function mockReq() {
    return { geolocation: {} };
}

function mockRes() {
    return {
        headers: {},
        setHeader: function(name, val) {
            this.headers[name] = val;
        },
    };
}

describe('express-ya-frameguard', function() {
    it('should set x-frame-options to deny by default', function(done) {
        var req = mockReq();
        var res = mockRes();
        expressYaFrameguard()(req, res, function() {
            for (var h in res.headers) {
                if (h.toLowerCase() === 'x-frame-options' && res.headers[h].toLowerCase() === 'deny') {
                    return done();
                }
            }
            throw new Error('No correct header was set');
        });
    });

    it('should not set x-frame-options if yandex', function(done) {
        var req = mockReq();
        req.geolocation.is_yandex = true;
        var res = mockRes();
        expressYaFrameguard()(req, res, function() {
            for (var h in res.headers) {
                if (h.toLowerCase() === 'x-frame-options') {
                    throw new Error('Header must not be set');
                }
            }
            return done();
        });
    });

    it('should support allow-from', function(done) {
        var req = mockReq();
        var res = mockRes();
        var host = 'http://example.com';
        expressYaFrameguard({ header: 'allow-from', allow_from: host })(req, res, function() {
            for (var h in res.headers) {
                if (h.toLowerCase() === 'x-frame-options' &&
                    res.headers[h].toLowerCase().indexOf('allow-from') !== -1 &&
                    res.headers[h].toLowerCase().indexOf(host) !== -1) {
                    return done();
                }
            }
            throw new Error('Header was not set');
        });
    });
});
