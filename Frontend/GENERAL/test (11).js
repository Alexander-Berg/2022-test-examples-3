/* global it */

'use strict';

var assert = require('assert');
var mock = require('./');

it('should return function', function() {
    assert.equal(typeof mock, 'function');
});

it('should append uatraits property', function(done) {
    var req = {};
    mock()(req, null, function() {
        assert.ok(req.uatraits);
        done();
    });
});

it('should set isBrowser to true by default', function(done) {
    var req = {};
    mock()(req, null, function() {
        assert.equal(req.uatraits.isBrowser, true);
        done();
    });
});

it('should support override argument', function(done) {
    var req = {};
    mock({ isBrowser: false })(req, null, function() {
        assert.equal(req.uatraits.isBrowser, false);
        done();
    });
});
