/* global it, beforeEach */
'use strict';

var fs = require('fs'),
    assert = require('assert');

beforeEach(function() {
    delete require.cache[require.resolve('./index.js')];
});

it('should get test environment from /etc/yandex/environment.type', function() {
    var env = require('./index.js');
    var envString = fs.readFileSync('/etc/yandex/environment.type', 'utf8').trim();
    if (fs.existsSync('/etc/yandex/environment.type')) {
        assert.equal(env, envString);
    } else {
        assert.ok(undefined === env);
    }
});

it('should return environment from process.env', function() {
    process.env.NODE_ENV = 'env';
    var env = require('./index.js');
    assert.equal(env, 'env');
});
