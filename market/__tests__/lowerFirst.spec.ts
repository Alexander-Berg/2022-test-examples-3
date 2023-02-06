/* eslint-disable no-unused-expressions */

import {lowerFirst} from '..';

describe('lowerFirst', () => {
    it('returns converted string', () => {
        expect(lowerFirst('FRED')).toEqual('fRED');
        expect(lowerFirst('fred')).toEqual('fred');
        expect(lowerFirst()).toEqual('');
    });

    it('properly typed', () => {
        (lowerFirst('foo') as string);
        // @ts-expect-error
        (lowerFirst('foo') as number);

        function lowerFirstWithNumber() {
            // @ts-expect-error
            (lowerFirst(42));
        }

        expect(lowerFirstWithNumber).toThrow();
    });
});
