import moment from 'moment';

import {formatDate, parseDate} from '../index';
import {ROBOT_DATETIME} from '../formats';

describe('formatDate(date, format)', () => {
    it('Дата передана строкой datetime', () => {
        expect(formatDate('2017-05-25 13:52:12', ROBOT_DATETIME)).toBe(
            '2017-05-25T13:52:12',
        );
    });

    it('Дата передана объектом Date', () => {
        expect(
            formatDate(new Date('2017-05-25 13:52:12'), ROBOT_DATETIME),
        ).toBe('2017-05-25T13:52:12');
    });

    it('Дата передана объектом Moment', () => {
        expect(formatDate(moment('2017-05-25 13:52:12'), ROBOT_DATETIME)).toBe(
            '2017-05-25T13:52:12',
        );
    });

    it('Дата передана объектом DateType', () => {
        expect(
            formatDate(parseDate('2017-05-25 13:52:12'), ROBOT_DATETIME),
        ).toBe('2017-05-25T13:52:12');
    });

    it('Дата передана невалидной строкой', () => {
        expect(formatDate('invalid', ROBOT_DATETIME)).toBe('Invalid date');
    });
});
