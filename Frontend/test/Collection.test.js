'use strict';

const sinon = require('sinon');
const { assert } = require('chai');
const Collection = require('../helpers/Collection');

sinon.assert.expose(assert, { prefix: '' });

describe('githooks / Collection', () => {
    describe('filter', () => {
        it('should return new collection', () => {
            const collection = new Collection([]);
            const actual = collection.filter(Boolean);

            assert.instanceOf(actual, Collection);
        });

        it('should filter items', () => {
            const collection = new Collection(['a', '', 'b']);
            const actual = collection.filter(Boolean).items;
            const expected = ['a', 'b'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('map', () => {
        it('should return array', () => {
            const collection = new Collection([]);
            const actual = collection.map(Boolean);

            assert.isArray(actual);
        });

        it('should map items with callback', () => {
            const collection = new Collection(['a', 'b']);
            const actual = collection.map(x => x + 'z');
            const expected = ['az', 'bz'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('forEach', () => {
        it('should return undefined', () => {
            const collection = new Collection([]);
            const actual = collection.forEach(Boolean);

            assert.isUndefined(actual);
        });

        it('should call callback for each item', () => {
            const collection = new Collection(['foo', 'bar']);
            const callback = sinon.stub();

            collection.forEach(callback);

            assert.equal(callback.callCount, 2);

            assert.equal(callback.firstCall.args[0], 'foo');
            assert.equal(callback.secondCall.args[0], 'bar');
        });
    });

    describe('filterBy', () => {
        it('should return new collection', () => {
            const collection = new Collection([]);
            const actual = collection.filterBy(/xyz/);

            assert.instanceOf(actual, Collection);
        });

        it('should return matched items', () => {
            const collection = new Collection(['ab', 'ac', 'bb']);
            const actual = collection.filterBy(/^a/).items;
            const expected = ['ab', 'ac'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('rejectBy', () => {
        it('should return new collection', () => {
            const collection = new Collection([]);
            const actual = collection.rejectBy(/xyz/);

            assert.instanceOf(actual, Collection);
        });

        it('should return unmatched items', () => {
            const collection = new Collection(['ab', 'ac', 'bb']);
            const actual = collection.rejectBy(/c$/).items;
            const expected = ['ab', 'bb'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('endsWith', () => {
        it('should return new collection', () => {
            const collection = new Collection([]);
            const actual = collection.endsWith();

            assert.instanceOf(actual, Collection);
        });

        it('should return matched items', () => {
            const collection = new Collection(['ab', 'ac', 'bb']);
            const actual = collection.endsWith('b').items;
            const expected = ['ab', 'bb'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('startsWith', () => {
        it('should return new collection', () => {
            const collection = new Collection([]);
            const actual = collection.startsWith();

            assert.instanceOf(actual, Collection);
        });

        it('should return matched items', () => {
            const collection = new Collection(['ab', 'ac', 'bb']);
            const actual = collection.startsWith('a').items;
            const expected = ['ab', 'ac'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('includes', () => {
        it('should return true for contained', () => {
            const collection = new Collection(['a', 'b']);

            assert.isTrue(collection.includes('a'));
        });

        it('should return false for contained', () => {
            const collection = new Collection(['a', 'b']);

            assert.isFalse(collection.includes('c'));
        });
    });

    describe('concat', () => {
        it('should return new collection', () => {
            const collection = new Collection([]);
            const actual = collection.concat([]);

            assert.instanceOf(actual, Collection);
        });

        it('should return all items', () => {
            const collection = new Collection(['a', 'b']);
            const actual = collection.concat(['c', 'd']).items;
            const expected = ['a', 'b', 'c', 'd'];

            assert.deepEqual(actual, expected);
        });
    });

    describe('asArgs', () => {
        it('should return quoted items', () => {
            const collection = new Collection(['a', 'b']);
            const actual = collection.asArgs();
            const expected = '"a" "b"';

            assert.equal(actual, expected);
        });
    });

    describe('asLines', () => {
        it('should return items in separate lines', () => {
            const collection = new Collection(['a', 'b']);
            const actual = collection.asLines();
            const expected = 'a\nb';

            assert.equal(actual, expected);
        });
    });

    describe('isEmpty', () => {
        it('should return true if empty', () => {
            const collection = new Collection([]);

            assert.isTrue(collection.isEmpty);
        });

        it('should return false if not empty', () => {
            const collection = new Collection(['a']);

            assert.isFalse(collection.isEmpty);
        });
    });
});
