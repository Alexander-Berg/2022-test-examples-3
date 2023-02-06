import moment from 'moment';

import {isValidDate} from '../index';

describe('isValidDate(date)', () => {
    test('Дата передана строкой вида YYYY-MM-DD', () => {
        expect(isValidDate('2017-12-25')).toBe(true);
    });

    test('Дата передана строкой вида YYYY-MM-DD HH:mm:ss', () => {
        expect(isValidDate('2017-12-25 18:48:50')).toBe(true);
    });

    test('Дата передана строкой в ISO8601', () => {
        expect(isValidDate('2014-09-08T08:02:17-05:00')).toBe(true);
    });

    test('Дата передана числом', () => {
        expect(isValidDate(10000)).toBe(true);
    });

    test('Дата передана невалидной строкой', () => {
        expect(isValidDate('invalid')).toBe(false);
    });

    test('Дата передана валидным объектом Date', () => {
        expect(isValidDate(new Date('2017-12-25'))).toBe(true);
    });

    test('Дата передана невалидным объектом Date', () => {
        expect(isValidDate(new Date('invalid'))).toBe(false);
    });

    test('Дата передана валидным объектом Moment', () => {
        expect(isValidDate(moment('2017-12-25'))).toBe(true);
    });

    test('Дата передана невалидным объектом Moment', () => {
        expect(isValidDate(moment('invalid'))).toBe(false);
    });
});
