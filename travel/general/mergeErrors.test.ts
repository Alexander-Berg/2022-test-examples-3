import {isEqual} from 'lodash';

import mergeErrors from './mergeErrors';

describe('From mergeErrors', () => {
    it('merges errors with undefined', () => {
        const errors = {
            foo: 1,
            bar: 'bar',
        };

        mergeErrors(errors);

        expect(
            isEqual(errors, {
                foo: 1,
                bar: 'bar',
            }),
        ).toBe(true);
    });

    it('merges errors with empty object', () => {
        const errors = {
            foo: 1,
            bar: 'bar',
        };

        mergeErrors(errors, {});

        expect(
            isEqual(errors, {
                foo: 1,
                bar: 'bar',
            }),
        ).toBe(true);
    });

    it('merges errors with object', () => {
        const errors = {
            foo: 1,
            bar: 'bar',
        };
        const errorsToMerge = {
            baz: true,
        };

        mergeErrors(errors, errorsToMerge);

        expect(
            isEqual(errors, {
                foo: 1,
                bar: 'bar',
                baz: true,
            }),
        ).toBe(true);

        expect(
            isEqual(errorsToMerge, {
                baz: true,
            }),
        ).toBe(true);
    });
});
