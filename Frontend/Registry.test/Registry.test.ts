import { describe, it, beforeEach } from 'mocha';
import { assert } from 'chai';

import { Registry } from '../Registry';

class TestRegistry extends Registry<number> {
    dumpTypeStore() {
        return Array.from(this.typeStore.entries());
    }

    dumpSubtypeStore() {
        return Array.from(this.subtypeStore.entries());
    }
}

describe('Registry', () => {
    describe('add', () => {
        let registry: TestRegistry;

        beforeEach(() => {
            registry = new TestRegistry('numbers');
        });

        it('should not add element without type', () => {
            const key = { };

            registry.set(key, 8);

            const actual = registry.dumpTypeStore();

            assert.deepEqual(actual, []);
        });

        it('should add new element with type and subtype', () => {
            const key = { type: '1', subtype: '3' };

            registry.set(key, 13);

            const actual = registry.dumpSubtypeStore();
            const expected = [['1', new Map().set('3', 13)]];

            assert.deepEqual(actual, expected);
        });

        it('should add new element with type', () => {
            const key = { type: '8' };

            registry.set(key, 8);

            const actual = registry.dumpTypeStore();
            const expected = [['8', 8]];

            assert.deepEqual(actual, expected);
        });

        it('should add elements with same type but different subtype', () => {
            const firstKey = { type: '1', subtype: '2' };
            const secondKey = { type: '1', subtype: '3' };

            registry.set(firstKey, 12);
            registry.set(secondKey, 13);

            const actual = registry.dumpSubtypeStore();
            const subtypesMap = new Map().set('2', 12).set('3', 13);
            const expected = [['1', subtypesMap]];

            assert.deepEqual(actual, expected);
        });
    });

    describe('get', () => {
        let registry: TestRegistry;

        beforeEach(() => {
            registry = new TestRegistry('numbers');

            registry.set({ type: '1' }, 1);
            registry.set({ type: '2', subtype: '1' }, 21);
        });

        it('should return undefined without type', () => {
            const key = {};

            assert.isUndefined(registry.get(key));
        });

        it('should return value for existing type and subtype', () => {
            const key = { type: '2', subtype: '1' };

            assert.equal(registry.get(key), 21);
        });

        it('should return undefined if subtype does not exist', () => {
            const key = { type: '1', subtype: '0' };

            assert.isUndefined(registry.get(key));
        });

        it('should return value for existing type', () => {
            const key = { type: '1' };

            assert.equal(registry.get(key), 1);
        });
    });
});
