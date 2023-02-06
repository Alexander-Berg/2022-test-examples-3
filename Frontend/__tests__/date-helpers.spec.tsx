import { makeDateFormater, calendar, IDateTokens, ICalendar } from '../date-helpers';

const DATE = new Date('2018-09-29 11:22:33');

function getTestDate(dayCount: number, fromDate: Date): Date {
    const date: Date = new Date(fromDate);

    date.setDate(fromDate.getDate() + dayCount);

    return date;
}

describe('makeDateFormater', () => {
    it('Вернет функцию с одним аргументом', () => {
        const fn = makeDateFormater('');

        expect(fn.length).toBe(1);
    });
    it('Вернет строку соответственно формату', () => {
        const short = makeDateFormater('D MMM YYYY');
        const defaults = makeDateFormater('D MMMM YYYY');
        const custom = makeDateFormater('DD.MM.YY HH:mm');
        const withoutTokens = makeDateFormater('random string');

        expect(short(DATE)).toBe('29 сен 2018');
        expect(defaults(DATE)).toBe('29 сентября 2018');
        expect(custom(DATE)).toBe('29.09.18 11:22');
        expect(withoutTokens(DATE)).toBe('random string');
    });

    it('Вернет строку соответственно формату из переопределенных токенов', () => {
        const customTokens: IDateTokens = {
            MMMM(date: Date): string {
                return `month ${date.getMonth()}`;
            },
            YY(date: Date): string {
                return `year ${date.getFullYear()}`;
            },
        };
        const custom = makeDateFormater('месяц: MMMM. год: YY. несуществующий: mm', customTokens);

        expect(custom(DATE)).toBe('месяц: month 8. год: year 2018. несуществующий: mm');
    });
});

describe('calendar', () => {
    const formats: ICalendar = {
        lastWeek: () => ('предыдущая неделя: MMMM'),
        lastDay: 'вчера',
        sameDay: () => ('сегодня: DD'),
        nextDay: 'завтра',
        nextWeek: 'следующая неделя',
        sameElse: 'по умолчанию',
    };

    describe('По умолчанию', () => {
        it('должен возвращать отформатированную строку по ключу sameDay', () => {
            expect(calendar({ formats, date: getTestDate(0, DATE), referenceDate: DATE })).toBe(`сегодня: ${DATE.getDate()}`);
        });

        it('должен возвращать отформатированную строку по ключу nextDay', () => {
            expect(calendar({ formats, date: getTestDate(1, DATE), referenceDate: DATE })).toBe('завтра');
        });

        it('должен возвращать отформатированную строку по ключу nextWeek', () => {
            const expectedString = 'следующая неделя';

            expect(calendar({ formats, date: getTestDate(2, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(3, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(4, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(5, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(6, DATE), referenceDate: DATE })).toEqual(expectedString);
        });

        it('должен возвращать отформатированную строку по ключу sameElse', () => {
            expect(calendar({ formats, date: getTestDate(7, DATE), referenceDate: DATE })).toEqual('по умолчанию');
        });

        it('должен возвращать отформатированную строку по ключу lasDay', () => {
            expect(calendar({ formats, date: getTestDate(-1, DATE), referenceDate: DATE })).toEqual('вчера');
        });

        it('должен возвращать отформатированную строку по ключу lastWeek', () => {
            const expectedString = 'предыдущая неделя: сентября';

            expect(calendar({ formats, date: getTestDate(-2, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-3, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-4, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-5, DATE), referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-6, DATE), referenceDate: DATE })).toEqual(expectedString);
        });
    });

    describe('С кастомными токенами', () => {
        const customMap: IDateTokens = {
            MMMM: () => ('{MMMM}'),
            DD: () => ('{DD}'),
        };

        it('должен возвращать отформатированную строку по ключу sameDay', () => {
            const NOW_DATE = new Date(1553086614359);
            const REF_DATE = new Date(1553086614360);

            expect(calendar({ formats, date: getTestDate(0, NOW_DATE), customMap, referenceDate: REF_DATE }))
                .toEqual('сегодня: {DD}');
        });

        it('должен возвращать отформатированную строку по ключу nextDay', () => {
            expect(calendar({ formats, date: getTestDate(1, DATE), customMap, referenceDate: DATE })).toEqual('завтра');
        });

        it('должен возвращать отформатированную строку по ключу nextWeek', () => {
            const expectedString = 'следующая неделя';

            expect(calendar({ formats, date: getTestDate(2, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(3, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(4, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(5, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(6, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
        });

        it('должен возвращать отформатированную строку по ключу sameElse', () => {
            expect(calendar({ formats, date: getTestDate(7, DATE), customMap, referenceDate: DATE })).toEqual('по умолчанию');
        });

        it('должен возвращать отформатированную строку по ключу lasDay', () => {
            expect(calendar({ formats, date: getTestDate(-1, DATE), customMap, referenceDate: DATE })).toEqual('вчера');
        });

        it('должен возвращать отформатированную строку по ключу lastWeek', () => {
            const expectedString = 'предыдущая неделя: {MMMM}';

            expect(calendar({ formats, date: getTestDate(-2, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-3, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-4, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-5, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
            expect(calendar({ formats, date: getTestDate(-6, DATE), customMap, referenceDate: DATE })).toEqual(expectedString);
        });
    });
});
