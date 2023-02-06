import { assert } from 'chai';
import { formatDateList } from '.';

describe('AfishaEvent.utils', () => {
    describe('formatDateList', () => {
        it('should return empty string when input is empty', () => {
            assert.equal(formatDateList([]), '');
        });

        it('should return 2 dates of different months correctly', () => {
            assert.equal(formatDateList([['5', 'мая'], ['6', 'июня']]), '5\xa0мая, 6\xa0июня');
        });

        it('should collapse equal dates', () => {
            assert.equal(formatDateList([['5', 'мая'], ['5', 'мая']]), '5\xa0мая');
        });

        it('should group days of same months', () => {
            assert.equal(
                formatDateList([['5', 'мая'], ['6', 'мая'], ['5', 'июня'], ['6', 'июня']]),
                '5, 6\xa0мая, 5, 6\xa0июня',
            );
        });
    });
});
