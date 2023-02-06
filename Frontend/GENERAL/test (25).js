/* global it */

'use strict';

var assert = require('assert'),
    yandexConfig = require('./');

it('should extend configs with default', function() {
    assert.equal(yandexConfig({ env: 'production' }).env, 'production');
    assert.equal(yandexConfig({ env: 'stress' }).env, 'stress');
});
