/* eslint-disable */
import test from 'ava';
import typograf from '../../../services/typograf';

test('typograf for "undefined" and "null" should return empty string', t => {
    const badValues = [undefined, null, '', 0] as string[];
    for (const val of badValues) {
        t.is(typograf(val), '');
    }
});
