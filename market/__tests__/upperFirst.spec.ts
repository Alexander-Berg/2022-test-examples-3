/* eslint-disable no-unused-expressions */

import {upperFirst} from '..';

describe('upperFirst', () => {
    it('returns converted string', () => {
        expect(upperFirst('fRED')).toEqual('FRED');
        expect(upperFirst('fred')).toEqual('Fred');
        expect(upperFirst()).toEqual('');
    });

    it('properly typed', () => {
        (upperFirst('foo') as string);
        // @ts-expect-error
        (upperFirst('foo') as number);

        function upperFirstWithNumber() {
            // @ts-expect-error
            (upperFirst(42));
        }

        expect(upperFirstWithNumber).toThrow();
    });
});
