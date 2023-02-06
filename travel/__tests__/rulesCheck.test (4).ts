import {getNow} from 'utilities/dateUtils';

import maxDate from '../maxDate';
import minDate from '../minDate';
import maxDateFromToday from '../maxDateFromToday';
import minDateFromToday from '../minDateFromToday';

describe('Date valudation rules', () => {
    /* MAX DATE */

    test('maxDate для пустого значения возвращает true', () => {
        expect(maxDate('11.11.2011', undefined)).toBeTruthy();
    });

    test('maxDate возвращет true для валидного значения', () => {
        expect(maxDate('11.11.2011', '10.11.2011')).toBeTruthy();
        expect(maxDate('11.11.2011', new Date(2011, 10, 10))).toBeTruthy();
    });

    test('maxDate возвращет false для не валидного значения', () => {
        expect(maxDate('11.11.2011', '12.11.2011')).toBeFalsy();
        expect(maxDate('11.11.2011', new Date(2011, 10, 12))).toBeFalsy();
    });

    test('maxDate для не валидной даты возвращает false', () => {
        expect(maxDate('11.11.2011', '124114')).toBeFalsy();
    });

    /* MIN DATE */

    test('minDate для пустого значения возвращает true', () => {
        expect(minDate('11.11.2011', undefined)).toBeTruthy();
    });

    test('minDate возвращет true для валидного значения', () => {
        expect(minDate('11.11.2011', '12.11.2011')).toBeTruthy();
        expect(minDate('11.11.2011', new Date(2011, 10, 12))).toBeTruthy();
    });

    test('minDate возвращет false для не валидного значения', () => {
        expect(minDate('11.11.2011', '10.11.2011')).toBeFalsy();
        expect(minDate('11.11.2011', new Date(2011, 10, 10))).toBeFalsy();
    });

    test('minDate для не валидной даты возвращает false', () => {
        expect(minDate('11.11.2011', '124114')).toBeFalsy();
    });

    /* MAX DATE FROM TODAY */

    const now = new Date(getNow());

    const dayOffset = {offset: -1, scale: 'day' as 'day'};
    const monthOffset = {offset: -1, scale: 'month' as 'month'};
    const yearOffset = {offset: -1, scale: 'year' as 'year'};

    test('maxDateFromToday возвращет true для валидного сдвига в сутках', () => {
        const twoDaysAgo = new Date(now);

        twoDaysAgo.setDate(twoDaysAgo.getDate() - 2);
        expect(maxDateFromToday(dayOffset, twoDaysAgo)).toBeTruthy();
    });

    test('maxDateFromToday возвращет true для валидного сдвига в месяцах', () => {
        const twoMonthAgo = new Date(now);

        twoMonthAgo.setMonth(twoMonthAgo.getMonth() - 2);
        expect(maxDateFromToday(monthOffset, twoMonthAgo)).toBeTruthy();
    });

    test('maxDateFromToday возвращет true для валидного сдвига в годах', () => {
        const twoYearsAgo = new Date(now);

        twoYearsAgo.setFullYear(twoYearsAgo.getFullYear() - 2);
        expect(maxDateFromToday(yearOffset, twoYearsAgo)).toBeTruthy();
    });

    test('maxDateFromToday возвращет false для не валидного сдвига в сутках', () => {
        expect(maxDateFromToday(dayOffset, now)).toBeFalsy();
    });

    test('maxDateFromToday возвращет false для не валидного сдвига в месяцах', () => {
        expect(maxDateFromToday(monthOffset, now)).toBeFalsy();
    });

    test('maxDateFromToday возвращет false для не валидного сдвига в годах', () => {
        expect(maxDateFromToday(yearOffset, now)).toBeFalsy();
    });

    /* MIN DATE */

    test('minDateFromToday возвращет true для валидного сдвига в сутках', () => {
        expect(minDateFromToday(dayOffset, now)).toBeTruthy();
    });

    test('minDateFromToday возвращет true для валидного сдвига в месяцах', () => {
        expect(minDateFromToday(monthOffset, now)).toBeTruthy();
    });

    test('minDateFromToday возвращет true для валидного сдвига в годах', () => {
        expect(minDateFromToday(yearOffset, now)).toBeTruthy();
    });

    test('minDateFromToday возвращет false для не валидного сдвига в сутках', () => {
        const twoDaysAgo = new Date(now);

        twoDaysAgo.setDate(twoDaysAgo.getDate() - 2);
        expect(minDateFromToday(dayOffset, twoDaysAgo)).toBeFalsy();
    });

    test('minDateFromToday возвращет false для не валидного сдвига в месяцах', () => {
        const twoMonthAgo = new Date(now);

        twoMonthAgo.setMonth(twoMonthAgo.getMonth() - 2);
        expect(minDateFromToday(monthOffset, twoMonthAgo)).toBeFalsy();
    });

    test('minDateFromToday возвращет false для не валидного сдвига в годах', () => {
        const twoYearsAgo = new Date(now);

        twoYearsAgo.setFullYear(twoYearsAgo.getFullYear() - 2);
        expect(minDateFromToday(yearOffset, twoYearsAgo)).toBeFalsy();
    });
});
