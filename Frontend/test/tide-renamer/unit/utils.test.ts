import 'mocha';
import { assert, expect } from 'chai';
import { getUpdates, toRegExp } from '../../../src/plugins/tide-renamer/utils';

describe('tide-renamer / utils', () => {
    describe('toRegExp', () => {
        it('should make a regexp out of string', () => {
            const str = 'Зеленая ссылка / Размер шрифта';
            const expectedRegExp =
                /^Зеленая ссылка \/ Размер шрифта$|Зеленая ссылка \/ Размер шрифта/;

            const actualRegExp = toRegExp(str);

            expect(actualRegExp).deep.equal(expectedRegExp);
        });

        it('should make a regexp out of string object', () => {
            const obj = {
                feature: 'Зеленая ссылка',
                experiment: 'Размер шрифта',
            };
            const expectedRegExpObject = {
                feature: /^Зеленая ссылка$|Зеленая ссылка/,
                experiment: /^Размер шрифта$|Размер шрифта/,
            };

            const actualRegExpObject = toRegExp(obj);

            expect(actualRegExpObject).deep.equal(expectedRegExpObject);
        });
    });

    describe('getUpdates', () => {
        const fromTitlePath = /abc/;
        const toTitlePath = 'something';

        it('should return old and new title unchanged for string and regexp', () => {
            const expected = { from: /abc/, to: 'something' };
            const actual = getUpdates(fromTitlePath, toTitlePath);

            assert.deepEqual(actual, expected);
        });

        it('should return an array of updates given array inputs', () => {
            const expected = [
                {
                    from: { feature: /some-feature/, type: /some-type/ },
                    to: { feature: 'new-feature', type: 'new-type' },
                },
                { from: /fragment-1/, to: 'another-fragment' },
                { from: /fragment-2/, to: 'yet-another-fragment' },
            ];
            const from = [
                { feature: /some-feature/, type: /some-type/ },
                /fragment-1/,
                /fragment-2/,
            ];
            const to = [
                { feature: 'new-feature', type: 'new-type' },
                'another-fragment',
                'yet-another-fragment',
            ];
            const actual = getUpdates(from, to);

            assert.deepEqual(expected, actual);
        });
    });
});
