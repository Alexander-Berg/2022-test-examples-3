'use strict';

const truncate = require('./truncate-attach-file-name.js');

const BEST_CASE_CHAR = '\u0066'; // encodeURIComponent('\u0066') -> 'f'
const WORST_CASE_CHAR = '\u10A0'; // encodeURIComponent('\u10A0') -> UTF-8: E1 82 A0 -> '%E1%82%A0'

describe('helper truncateAttachFileName', () => {
    it('должен вернуть name, если нет расширения и name не выходит за границы', () => {
        const name = new Array(truncate.MAX_LENGTH_OF_ENCODED_NAME).fill(BEST_CASE_CHAR).join('');
        expect(truncate(name)).toBe(name);

        const name2 = new Array(truncate.MAX_NAME_LENGTH_FOR_WORST_CASE).fill(WORST_CASE_CHAR).join('');
        expect(truncate(name2)).toBe(name2);
    });

    it('должен сократить name, если нет расширения и name выходит за границы', () => {
        const name = new Array(truncate.MAX_LENGTH_OF_ENCODED_NAME + 1).fill(BEST_CASE_CHAR).join('');
        expect(truncate(name)).toBe(name.substring(0, truncate.MAX_NAME_LENGTH_FOR_WORST_CASE));
    });

    it('должен сократить name и сохранить расширение, если name выходит за границы', () => {
        const ext = '.ext';

        const name = new Array(truncate.MAX_LENGTH_OF_ENCODED_NAME).fill(BEST_CASE_CHAR).join('') + ext;
        const expected = new Array(truncate.MAX_NAME_LENGTH_FOR_WORST_CASE - ext.length)
            .fill(BEST_CASE_CHAR).join('') + ext;
        expect(truncate(name)).toBe(expected);

        const name2 = new Array(truncate.MAX_NAME_LENGTH_FOR_WORST_CASE).fill(WORST_CASE_CHAR).join('') + ext;
        const expected2 = new Array(truncate.MAX_NAME_LENGTH_FOR_WORST_CASE - ext.length)
            .fill(WORST_CASE_CHAR).join('') + ext;
        expect(truncate(name2)).toBe(expected2);
    });

    it('должен сократить name, если name выходит за границы', () => {
        const fakeExt = '.fake ext';
        const name = new Array(truncate.MAX_LENGTH_OF_ENCODED_NAME).fill(BEST_CASE_CHAR).join('') + fakeExt;
        expect(truncate(name)).toBe(name.substring(0, truncate.MAX_NAME_LENGTH_FOR_WORST_CASE));
    });
});
