import DateMock from 'utilities/testUtils/DateMock';

import {formatDateRange, setDateLocale, getDateLocale} from '../index';
import locales from '../locales';

describe('formatDateRange(from, to)', () => {
    const DEFAULT_LOCALE = getDateLocale();
    const MOCK_DATE = '2019-01-01';

    beforeEach(() => {
        setDateLocale(locales.RU);
        DateMock.mock(MOCK_DATE);
    });

    afterEach(() => {
        setDateLocale(DEFAULT_LOCALE);
        DateMock.restore();
    });

    test.each`
        from            | to              | expected
        ${'2018-10-10'} | ${undefined}    | ${'10 октября 2018'}
        ${'2019-10-10'} | ${undefined}    | ${'10 октября'}
        ${'2019-10-10'} | ${'2019-10-12'} | ${'10 окт — 12 окт'}
        ${'2019-10-10'} | ${'2019-11-12'} | ${'10 окт — 12 нояб'}
        ${'2019-10-10'} | ${'2020-01-12'} | ${'10 окт 2019 — 12 янв 2020'}
        ${'2018-10-10'} | ${'2019-01-12'} | ${'10 окт 2018 — 12 янв 2019'}
    `('returns $expected for from $from to $to,', ({from, to, expected}) => {
        expect(formatDateRange(from, to)).toBe(expected);
    });
});
