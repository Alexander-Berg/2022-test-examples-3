import moment from 'moment';

import {parseDate} from '../index';

describe('parseDate(date)', () => {
    test('Дата передана строкой', () => {
        expect(moment.isMoment(parseDate('2017-05-25'))).toBe(true);
    });

    test('Дата передана строкой datetime', () => {
        expect(moment.isMoment(parseDate('2017-05-25 13:52:12'))).toBe(true);
    });

    test('Дата передана числом', () => {
        expect(moment.isMoment(parseDate(10000))).toBe(true);
    });

    test('Дата передана объектом Date', () => {
        expect(moment.isMoment(parseDate(new Date('2017-05-25')))).toBe(true);
    });

    test('Дата передана объектом Moment', () => {
        expect(moment.isMoment(parseDate(moment('2017-05-25')))).toBe(true);
    });
});
