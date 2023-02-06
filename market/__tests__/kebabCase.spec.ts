/* eslint-disable no-unused-expressions */

import {kebabCase} from '..';

describe('kebabCase', () => {
    it('returns snake cased string', () => {
        expect(kebabCase('Foo Bar')).toEqual('foo-bar');
        expect(kebabCase('fooBar')).toEqual('foo-bar');
        expect(kebabCase('__FOO_BAR__')).toEqual('foo-bar');
        expect(kebabCase()).toEqual('');
    });

    it('properly typed', () => {
        (kebabCase('foo') as string);
        // @ts-expect-error
        (kebabCase('foo') as number);

        function kebabCaseWithNumber() {
            // @ts-expect-error
            (kebabCase(42));
        }

        expect(kebabCaseWithNumber).toThrow();
    });
});
