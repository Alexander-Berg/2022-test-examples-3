/* eslint-disable no-unused-expressions */

import {noop} from '..';

describe('noop', () => {
    it('should return undefined', () => {
        const result = noop();

        expect(result).toEqual(undefined);

        (result as void);
        // @ts-expect-error
        (result as 'test');
    });

    it('should accept any type of args', () => {
        const result = noop(1, 'test', true, [], {});

        expect(result).toEqual(undefined);

        (result as void);
        // @ts-expect-error
        (result as 'test');
    });
});
