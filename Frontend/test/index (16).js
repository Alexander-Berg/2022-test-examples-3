/* global describe, it */

let assert = require('assert');
let TJSON = require('..');
let fixture = require('./utils').fixture;

describe('tjson', function() {
    it('should exports function', function() {
        assert.equal(typeof TJSON, 'function', 'tjson is not a function');
    });

    it('should not throw on valid tjson', function() {
        assert.doesNotThrow(function() {
            // eslint-disable-next-line
            new TJSON(fixture('simple'));
        });
    });
});

describe('tjson.get', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.get), 'function', 'tjson.get is not a function');
    });

    // https://st.yandex-team.ru/FRONTEND-664
    it.skip('should return undefined', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(tjson.get('action.cancel', undefined));
        assert.equal(tjson.lang('ru').get('action.cancel', undefined));
        assert.equal(tjson.keyset('index').get('action.cancel', undefined));
    });

    it('should return value on key', function() {
        let tjson = new TJSON(fixture('simple'));
        let value = tjson.lang('ru').keyset('index').get('action.cancel');
        assert.equal(value, 'Отмена');
    });

    it('should return value on plural key', function() {
        let tjson = new TJSON(fixture('plural'));
        let keyset = tjson.lang('ru').keyset('common');
        assert.equal(keyset.get('points-word'), 'баллов');
        assert.equal(keyset.get('points-word', 1), 'балл');
        assert.equal(keyset.get('points-word', 2), 'балла');
        assert.equal(keyset.get('points-word', 0), 'баллов');
        assert.equal(keyset.get('points-word', '0'), 'баллов');
        assert.equal(keyset.get('points-word', '1'), 'балл');
        assert.equal(keyset.get('points-word', '2'), 'балла');
    });
});

describe('tjson.keyset', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.keyset), 'function', 'tjson.keyset is not a function');
    });

    it('should return new TJSON instance', function() {
        let tjson = new TJSON(fixture('simple'));
        let instance = tjson.keyset('lol');
        assert.ok(instance instanceof TJSON);
        assert.ok(instance !== tjson);
    });

    it('should throw on invalid keyset name', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.throws(function() { tjson.keyset() }, /Keyset must be a string, not a undefined/);
        assert.throws(function() { tjson.keyset(1) }, /Keyset must be a string, not a 1/);
    });
});

describe('tjson.lang', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.lang), 'function', 'tjson.lang is not a function');
    });

    it('should return new TJSON instance', function() {
        let tjson = new TJSON(fixture('simple'));
        let instance = tjson.lang('lol');
        assert.ok(instance instanceof TJSON);
        assert.ok(instance !== tjson);
    });

    it('should throw on invalid lang name', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.throws(function() { tjson.lang() }, /Language must be a string, not a undefined/);
        assert.throws(function() { tjson.lang(1) }, /Language must be a string, not a 1/);
    });
});

describe('tjson.languages', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.languages), 'function', 'tjson.languages is not a function');
    });

    it('should return array of languages', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.deepEqual(tjson.languages(), ['ru']);
    });
});

describe('tjson.keysets', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.keysets), 'function', 'tjson.keysets is not a function');
    });

    it('should return array of keysets', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.deepEqual(tjson.keysets('ru'), ['index']);
        assert.deepEqual(tjson.keysets('wrong'), []);
    });

    it('should return array of keysets for selected language', function() {
        let tjson = new TJSON(fixture('simple')).lang('ru');
        assert.deepEqual(tjson.keysets(), ['index']);
    });
});

describe('tjson.keys', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.keys), 'function', 'tjson.keys is not a function');
    });

    it('should return array of keys', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.deepEqual(tjson.keys(), []);
        assert.deepEqual(tjson.keys('ru'), []);
        assert.deepEqual(tjson.keys('index', 'ru'), ['action.cancel']);
    });

    it('should return array of keys for selected language', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.deepEqual(tjson.lang('ru').keys(), []);
        assert.deepEqual(tjson.lang('ru').keys('index'), ['action.cancel']);
        assert.deepEqual(tjson.lang('ru').keyset('index').keys(), ['action.cancel']);
    });
});

describe('tjson.key', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.key), 'function', 'tjson.key is not a function');
    });

    it('should return internal key representation', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.deepEqual(tjson.key(), undefined);
        assert.deepEqual(tjson.key('ru'), undefined);
        assert.deepEqual(tjson.key('index', 'ru'), undefined);
        assert.deepEqual(tjson.key('action.cancel', 'index', 'ru'), {
            form: 'Отмена',
            isPlural: false,
            one: undefined,
            some: undefined,
            many: undefined,
            none: undefined,
        });

        assert.deepEqual(tjson.lang('ru').key('action.cancel', 'index'), {
            form: 'Отмена',
            isPlural: false,
            one: undefined,
            some: undefined,
            many: undefined,
            none: undefined,
        });
    });
});

describe('tjson.merge', function() {
    it('should be a function', function() {
        let tjson = new TJSON(fixture('simple'));
        assert.equal(typeof (tjson.merge), 'function', 'tjson.merge is not a function');
    });

    it('should return TJSON instance', function() {
        let tjson = new TJSON();
        assert.ok(tjson.merge(fixture('simple')) instanceof TJSON);
    });
});
