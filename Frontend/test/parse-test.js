'use strict';
/* eslint func-names: "off" */

var test = require('tape');
var ycookie = require('../');

test('parse non string value', function(t) {
    t.plan(5);

    t.deepEqual(ycookie.parse({}), {}, 'should return empty object');
    t.deepEqual(ycookie.parse(console.log), {}, 'should return empty object'); // eslint-disable-line no-console
    t.deepEqual(ycookie.parse(true), {}, 'should return empty object');
    t.deepEqual(ycookie.parse(1), {}, 'should return empty object');
    t.deepEqual(ycookie.parse(''), {}, 'should return empty object');
});

test('parse cookie with decodeURIComponent fail', function(t) {
    t.plan(2);

    var ys = ycookie.parse('foo.ba%Hr#kaomoji.%C2%AF%EF%BC%BC_(%E3%83%84)_%2F%C2%AF#litevbchrome.8.11.0');

    t.equal(ys.foo, 'ba%Hr', 'should contain value');
    t.equal(ys.kaomoji, '¯＼_(ツ)_/¯', 'should contain value with non latin symbols');
});

test('parse cookie with encoded #(%23) delimeter', function(t) {
    t.plan(3);

    var ys = ycookie.parse('9999999999.foo.bar%239999999999.kaomoji.%C2%AF%EF%BC%BC_(%E3%83%84)_%2F%C2%AF#9999999999.litevbchrome.8.11.0');

    t.equal(ys.foo.value, 'bar', 'should contain value');
    t.equal(ys.kaomoji.value, '¯＼_(ツ)_/¯', 'should contain value with non latin symbols');
    t.equal(ys.litevbchrome.value, '8.11.0', 'should contain value');
});

test('parse YS', function(t) {
    t.plan(3);

    var ys = ycookie.parse('foo.bar#kaomoji.%C2%AF%EF%BC%BC_(%E3%83%84)_%2F%C2%AF#litevbchrome.8.11.0');

    t.equal(ys.foo, 'bar', 'should contain value');
    t.equal(ys.kaomoji, '¯＼_(ツ)_/¯', 'should contain value with non latin symbols');
    t.equal(ys.litevbchrome, '8.11.0', 'should contain value with dots');
});

test('parse YP', function(t) {
    t.plan(5);

    var yp = ycookie.parse('9999999999.foo.bar#9999999999.foo2.bar1.bar2#8888888888.kaomoji.%C2%AF%EF%BC%BC_(%E3%83%84)_%2F%C2%AF#1111111111.x.y');

    t.equal(yp.foo.value, 'bar', 'should contain value');
    t.equal(yp.kaomoji.value, '¯＼_(ツ)_/¯', 'should contain value with non latin symbols');
    t.ok(yp.foo.expires instanceof Date, 'should contain Date object in expires field');
    t.equal(yp.foo2.value, 'bar1.bar2', 'should contain value');
    t.equal(yp.x, undefined, 'should not cantain key after expiration');
});
