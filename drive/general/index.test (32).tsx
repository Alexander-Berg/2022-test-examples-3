/*eslint-disable*/
import { isObjectEqual } from './index';

const DEEP_OBJECT = {
    arr: [1,2,3,{}],
    obj: {
        a: 1,
        b: 2
    },
    str: 'string'
};

const SAME_DEEP_OBJECT = {
    arr: [1,2,3,{a: 'j'}],
    obj: {
        a: 1,
        b: 3
    },
    str: 'string'
};

describe('Equal objects', () => {
    it('should work with empty objects', () => {
        expect(isObjectEqual({}, {})).toBe(true);
    });

    it('should work with difference objects', () => {
        expect(isObjectEqual({a: 5}, {b: 6})).toBe(false);
    });

    it('should work with wrong data', () => {
        expect(isObjectEqual(undefined, {b: 6})).toBe(false);
    });

    it('should work with deep objects', () => {
        expect(isObjectEqual(DEEP_OBJECT, Object.assign({}, DEEP_OBJECT))).toBe(true);
    });

    it('should work with same objects', () => {
        expect(isObjectEqual(DEEP_OBJECT, SAME_DEEP_OBJECT)).toBe(false);
    });
});
