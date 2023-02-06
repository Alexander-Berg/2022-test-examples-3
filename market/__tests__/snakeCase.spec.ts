/* eslint-disable no-unused-expressions */

import {snakeCase} from '..';

describe('snakeCase', () => {
    it('returns snake cased string', () => {
        expect(snakeCase('fooBar')).toEqual('foo_bar');
        expect(snakeCase('Foo Bar')).toEqual('foo_bar');
        expect(snakeCase('--FOO-BAR--')).toEqual('foo_bar');
        expect(snakeCase()).toEqual('');
    });

    it('properly typed', () => {
        (snakeCase('foo') as string);
        // @ts-expect-error
        (snakeCase('foo') as number);

        function snakeCaseWithNumber() {
            // @ts-expect-error
            (snakeCase(42));
        }

        expect(snakeCaseWithNumber).toThrow();
    });
});
