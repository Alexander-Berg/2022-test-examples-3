/* eslint-disable no-unused-expressions */

import {has} from '..';

describe('function "has"', () => {
    it('has hasOwnProperty semantics', () => {
        const sample: {
            offers: number,
            woo: void,
            no?: number
        } = {offers: 30, woo: undefined};

        expect(has('offers', sample)).toEqual(true);
        expect(has('offers')(sample)).toEqual(true);

        expect(has('woo', sample)).toEqual(true);
        expect(has('woo')(sample)).toEqual(true);

        expect(has('no', sample)).toEqual(false);
        expect(has('no')(sample)).toEqual(false);
    });

    it('shows error if there is no such field for sure', () => {
        const sample = {};

        // @ts-expect-error
        expect(has('no', sample)).toEqual(false);
        // todo: given error is very unclear
        // @ts-expect-error
        expect(has<typeof sample, 'no'>('no')(sample)).toEqual(false);
    });

    it('supports not only strings but also numbers for arrays', () => {
        const sample: [1, 2] = [1, 2];

        expect(has(1, sample)).toEqual(true);
        expect(has(1)(sample)).toEqual(true);
    });

    it('throws exception if first argument is not string or number', () => {
        function hasWithWrongKeyType() {
            // @ts-expect-error
            has(null, {});
            // @ts-expect-error
            has(null)({});
        }

        expect(hasWithWrongKeyType).toThrow();
    });
});
