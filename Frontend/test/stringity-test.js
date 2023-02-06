'use strict';
/* eslint func-names: "off" */

var test = require('tape');
var ycookie = require('../');

test('stringify not an object', function(t) {
    t.plan(4);

    t.notOk(ycookie.stringify(true), 'should return undefined');
    t.notOk(ycookie.stringify(1), 'should return undefined');
    t.notOk(ycookie.stringify('1'), 'should return undefined');
    t.notOk(ycookie.stringify(console.log), 'should return undefined'); // eslint-disable-line no-console
});

test('stringify YS', function(t) {
    t.plan(1);

    var ys = ycookie.stringify({ foo: 'bar', kaomoji: '¯＼_(ツ)_/¯' });

    t.equal(ys, 'foo.bar#kaomoji.%C2%AF%EF%BC%BC_(%E3%83%84)_%2F%C2%AF', 'should stringify YS correct');
});

test('stringify YP', function(t) {
    t.plan(1);

    var yp = ycookie.stringify({
        foo: {
            value: 'bar',
            expires: 9999999999
        },
        kaomoji: {
            value: '¯＼_(ツ)_/¯',
            expires: 8888888888
        }
    });

    t.equal(yp, '9999999999.foo.bar#8888888888.kaomoji.%C2%AF%EF%BC%BC_(%E3%83%84)_%2F%C2%AF', 'should stringify YP correct');
});
