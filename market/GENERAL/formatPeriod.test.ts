import formatPeriod from './formatPeriod';
import {DURATION_UNIT} from '../../../../constants/offer';

describe('formatPeriod', () => {
    it('должен приводить duration в часы, если срок годности/службы/гарантии указан в часах и других единицах', () => {
        const result = formatPeriod({
            years: 2,
            months: 5,
            days: 3,
            hours: 18,
        });
        const expectedValue = '20970';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.HOURS});
    });

    it('должен возвращать duration в часах, если срок годности/службы/гарантии указан только в часах', () => {
        const result = formatPeriod({
            hours: 72,
        });
        const expectedValue = '72';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.HOURS});
    });

    it('должен приводить duration в дни, если срок годности/службы/гарантии указан в днях и других единицах', () => {
        const result = formatPeriod({
            years: 1,
            months: 6,
            days: 10,
        });
        const expectedValue = '550';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.DAYS});
    });

    it('должен возвращать duration в днях, если срок годности/службы/гарантии указан только в днях', () => {
        const result = formatPeriod({
            days: 15,
        });
        const expectedValue = '15';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.DAYS});
    });

    it('должен приводить duration в месяцы, если срок годности/службы/гарантии указан в месяцах и других единицах', () => {
        const result = formatPeriod({
            years: 1,
            months: 3,
        });
        const expectedValue = '15';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.MONTHS});
    });

    it('должен возвращать duration в месяах, если срок годности/службы/гарантии указан только в месяцах', () => {
        const result = formatPeriod({
            months: 6,
        });
        const expectedValue = '6';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.MONTHS});
    });

    it('должен возвращать duration в годах, если срок годности/службы/гарантии указан только в годах', () => {
        const result = formatPeriod({
            years: 5,
        });
        const expectedValue = '5';

        expect(result).toStrictEqual({value: expectedValue, unit: DURATION_UNIT.YEARS});
    });

    it('должен по дефолту возвращать пустую строку в value и годы в unit, если срок годности/службы/гаранти не указан', () => {
        const result = formatPeriod(undefined);

        expect(result).toStrictEqual({value: '', unit: DURATION_UNIT.YEARS});
    });
});
