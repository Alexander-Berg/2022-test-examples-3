import DateMock from 'utilities/testUtils/DateMock';

import {getDateLocale, humanFormatDate, setDateLocale} from '../index';
import locales from '../locales';

describe('humanFormatDate(date)', () => {
    const DEFAULT_LOCALE = getDateLocale();

    beforeEach(() => {
        setDateLocale(locales.RU);
    });

    afterEach(() => {
        setDateLocale(DEFAULT_LOCALE);
    });

    test('Должен вернуть дату с годом', () => {
        expect(humanFormatDate('2017-12-25')).toBe('25 декабря 2017');
    });

    test('Должен вернуть дату без года, если год даты совпадает с теущим годом', () => {
        DateMock.mock('2017-12-29');
        expect(humanFormatDate('2017-12-25')).toBe('25 декабря');
        DateMock.restore();
    });
});
