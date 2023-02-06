/**
 * Эти тесты являются портированием из ramda
 * @see https://github.com/ramda/ramda/blob/master/test/clone.js
 */

/* eslint-disable no-unused-expressions */

import {clone, forEach} from '..';

const eq = (a, b) => expect(a).toEqual(b);
const assert = {
    notStrictEqual: (a, b) => expect(a).not.toStrictEqual(b),
    strictEqual: (a, b) => expect(a).toStrictEqual(b),
    notToBe: (a, b) => expect(a).not.toBe(b),
};

describe('deep clone integers, strings and booleans', () => {
    it('clones integers', () => {
        eq(clone(-4), -4);
        eq(clone(9007199254740991), 9007199254740991);
    });

    it('clones floats', () => {
        eq(clone(-4.5), -4.5);
        eq(clone(0.0), 0.0);
    });

    it('clones strings', () => {
        eq(clone('ramda'), 'ramda');
    });

    it('clones booleans', () => {
        eq(clone(true), true);
    });
});

describe('deep clone objects', () => {
    it('clones shallow object', () => {
        const obj = {
            a: 1,
            b: 'ramda',
            c: true,
            d: new Date(2013, 11, 25),
        };
        const cloned = clone(obj);
        obj.c = false;
        obj.d.setDate(31);
        eq(cloned, {
            a: 1,
            b: 'ramda',
            c: true,
            d: new Date(2013, 11, 25),
        });
    });

    it('clones deep object', () => {
        const obj = {a: {b: {c: 'ramda'}}};
        const cloned = clone(obj);
        obj.a.b.c = null;
        eq(cloned, {a: {b: {c: 'ramda'}}});
    });

    it('clones objects with circular references', () => {
        const x = {c: null};
        const y = {a: x};
        const z = {b: y};
        x.c = z;
        const cloned = clone(x);
        assert.notToBe(x, cloned);
        assert.notToBe(x.c, cloned.c);
        assert.notToBe(x.c.b, cloned.c.b);
        assert.notToBe(x.c.b.a, cloned.c.b.a);
        assert.notToBe(x.c.b.a.c, cloned.c.b.a.c);
        eq(Object.keys(cloned), Object.keys(x));
        eq(Object.keys(cloned.c), Object.keys(x.c));
        eq(Object.keys(cloned.c.b), Object.keys(x.c.b));
        eq(Object.keys(cloned.c.b.a), Object.keys(x.c.b.a));
        eq(Object.keys(cloned.c.b.a.c), Object.keys(x.c.b.a.c));

        x.c.b = 1;
        expect(cloned.c.b).not.toEqual(x.c.b);
    });

    it('clone instances', () => {
        const Obj = function (x) {
            this.x = x;
        };
        Obj.prototype.get = function () {
            return this.x;
        };
        Obj.prototype.set = function (x) {
            this.x = x;
        };

        const obj = new Obj(10);
        eq(obj.get(), 10);

        const cloned = clone(obj);
        eq(cloned.get(), 10);

        assert.notToBe(obj, clone);

        obj.set(11);
        eq(obj.get(), 11);
        eq(cloned.get(), 10);
    });
});

describe('deep clone arrays', () => {
    it('clones shallow arrays', () => {
        const list = [1, 2, 3];
        const cloned = clone(list);
        list.pop();
        eq(cloned, [1, 2, 3]);
    });

    it('clones deep arrays', () => {
        const list = [1, [1, 2, 3], [[[5]]]];
        const cloned = clone(list);

        assert.notToBe(list, cloned);
        assert.notToBe(list[2], cloned[2]);
        assert.notToBe(list[2][0], cloned[2][0]);

        eq(cloned, [1, [1, 2, 3], [[[5]]]]);
    });
});

describe('deep clone functions', () => {
    it('keep reference to function', () => {
        const fn = function (x) { return x + x; };
        const list = [{a: fn}];

        const cloned = clone(list);

        eq(cloned[0].a(10), 20);
        eq(list[0].a, cloned[0].a);
    });
});

describe('built-in types', () => {
    it('clones Date object', () => {
        const date = new Date(2014, 10, 14, 23, 59, 59, 999);

        const cloned = clone(date);

        assert.notToBe(date, cloned);
        eq(cloned, new Date(2014, 10, 14, 23, 59, 59, 999));

        eq(cloned.getDay(), 5); // friday
    });

    it('clones RegExp object', () => {
        forEach((pattern: RegExp) => {
            const cloned = clone(pattern);
            assert.notToBe(cloned, pattern);
            eq(cloned.constructor, RegExp);
            eq(cloned.source, pattern.source);
            eq(cloned.global, pattern.global);
            eq(cloned.ignoreCase, pattern.ignoreCase);
            eq(cloned.multiline, pattern.multiline);
        }, [/x/, /x/g, /x/i, /x/m, /x/gi, /x/gm, /x/im, /x/gim]);
    });
});

describe('deep clone deep nested mixed objects', () => {
    it('clones array with objects', () => {
        const list: Array<any> = [{a: {b: 1}}, [{c: {d: 1}}]];
        const cloned = clone(list);
        list[1][0] = null;
        eq(cloned, [{a: {b: 1}}, [{c: {d: 1}}]]);
    });

    it('clones array with arrays', () => {
        const list = [[1], [[3]]];
        const cloned = clone(list);
        list[1][0] = null;
        eq(cloned, [[1], [[3]]]);
    });

    it('clones array with mutual ref object', () => {
        const obj = {a: 1};
        const list = [{b: obj}, {b: obj}];
        const cloned = clone(list);

        assert.strictEqual(list[0].b, list[1].b);
        assert.strictEqual(cloned[0].b, cloned[1].b);
        assert.notToBe(cloned[0].b, list[0].b);
        assert.notToBe(cloned[1].b, list[1].b);

        eq(cloned[0].b, {a: 1});
        eq(cloned[1].b, {a: 1});

        obj.a = 2;
        eq(cloned[0].b, {a: 1});
        eq(cloned[1].b, {a: 1});
    });
});

describe('deep clone edge cases', () => {
    it('nulls, undefineds and empty objects and arrays', () => {
        eq(clone(null), null);
        eq(clone(undefined), undefined);
        assert.notToBe(clone(undefined), null);

        const obj = {};
        assert.notToBe(clone(obj), obj);

        const list = [];
        assert.notToBe(clone(list), list);
    });
});

describe('types test', () => {
    it('keeps previous type as is', () => {
        const source = {a: 1, b: 2};
        const cloned = clone(source);

        (cloned as {
            a: 1,
            b: 2
        });

        // @ts-expect-error
        (cloned as {
            b: 3,
            c: 4
        });
    });
});
