import { groupByDates, getFormattedForDateRange } from '../../../components/helpers/date';

describe('helperDate', () => {
    describe('groupByDates', () => {
        const getDate = (item) => item.date && new Date(item.date);

        const TODAY = new Date('Mon Apr 24 2017 17:25:47 GMT+0300 (MSK)');

        it('Корректно группирует по месяцам', () => {
            const items = [
                { date: 'Mon Dec 12 2016 18:28:48 GMT+0300 (MSK)' },
                { date: 'Mon Dec 13 2016 00:00:01 GMT+0300 (MSK)' },
                { date: 'Mon Dec 14 2016 00:00:01 GMT+0300 (MSK)' },
                { date: 'Sat Feb 28 2015 18:28:48 GMT+0300 (MSK)' },
                { date: 'Wed Feb 29 2012 18:28:48 GMT+0300 (MSK)' }
            ];

            expect(groupByDates(items, getDate, TODAY)).toEqual([
                {
                    month: 11,
                    year: 2016,
                    type: 'month',
                    key: '2016_11',
                    title: 'Декабрь 2016',
                    items: [
                        { date: 'Mon Dec 12 2016 18:28:48 GMT+0300 (MSK)' },
                        { date: 'Mon Dec 13 2016 00:00:01 GMT+0300 (MSK)' },
                        { date: 'Mon Dec 14 2016 00:00:01 GMT+0300 (MSK)' }
                    ]
                },
                {
                    month: 1,
                    year: 2015,
                    type: 'month',
                    key: '2015_1',
                    title: 'Февраль 2015',
                    items: [
                        { date: 'Sat Feb 28 2015 18:28:48 GMT+0300 (MSK)' }
                    ]
                }, {
                    month: 1,
                    year: 2012,
                    type: 'month',
                    key: '2012_1',
                    title: 'Февраль 2012',
                    items: [
                        { date: 'Wed Feb 29 2012 18:28:48 GMT+0300 (MSK)' }
                    ]
                }
            ]);
        });

        it('Элементы, созданные сегодня, вчера и за последнюю неделю должны корректно распределяться по стандартным группам', () => {
            const items = [
                { date: 'Mon Apr 24 2017 12:13:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 24 2017 19:55:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 23 2017 12:13:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 23 2017 19:55:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 21 2017 12:13:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 20 2017 16:55:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 17 2017 16:55:00 GMT+0300 (MSK)' },
                { date: 'Mon Apr 16 2017 16:55:00 GMT+0300 (MSK)' }
            ];

            expect(groupByDates(items, getDate, TODAY)).toEqual([
                {
                    type: 'today',
                    key: 'today',
                    title: 'Сегодня',
                    items: [
                        { date: 'Mon Apr 24 2017 12:13:00 GMT+0300 (MSK)' },
                        { date: 'Mon Apr 24 2017 19:55:00 GMT+0300 (MSK)' }
                    ]
                },
                {
                    type: 'yesterday',
                    key: 'yesterday',
                    title: 'Вчера',
                    items: [
                        { date: 'Mon Apr 23 2017 12:13:00 GMT+0300 (MSK)' },
                        { date: 'Mon Apr 23 2017 19:55:00 GMT+0300 (MSK)' }
                    ]
                },
                {
                    type: 'week',
                    key: 'week',
                    title: 'За последние 7 дней',
                    items: [
                        { date: 'Mon Apr 21 2017 12:13:00 GMT+0300 (MSK)' },
                        { date: 'Mon Apr 20 2017 16:55:00 GMT+0300 (MSK)' },
                        { date: 'Mon Apr 17 2017 16:55:00 GMT+0300 (MSK)' }
                    ]
                },
                {
                    type: 'month',
                    month: 3,
                    year: 2017,
                    key: '2017_3',
                    title: 'Апрель 2017',
                    items: [
                        { date: 'Mon Apr 16 2017 16:55:00 GMT+0300 (MSK)' }
                    ]
                }
            ]);
        });

        it('Для неопределённых дат не должно быть month и year', () => {
            const items = [
                { date: 'Mon Dec 12 2016 18:28:48 GMT+0300 (MSK)' },
                { date: null },
                { date: null }
            ];

            expect(groupByDates(items, getDate, TODAY)).toEqual([
                {
                    month: 11,
                    year: 2016,
                    type: 'month',
                    key: '2016_11',
                    title: 'Декабрь 2016',
                    items: [
                        { date: 'Mon Dec 12 2016 18:28:48 GMT+0300 (MSK)' }
                    ]
                },
                {
                    type: 'unknown',
                    key: 'unknown',
                    title: 'Без даты съемки',
                    items: [
                        { date: null },
                        { date: null }
                    ]
                }
            ]);
        });
    });

    describe('getFormattedForDateRange', () => {
        it('Разные года и месяцы', () => {
            expect(getFormattedForDateRange(
                (new Date(2014, 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date(2015, 3, 1, 0, 0, 0, 0)).getTime()
            )).toBe('1 мая 2014 - 1 апреля 2015');
        });

        it('Разные года, одинаковые месяцы', () => {
            expect(getFormattedForDateRange(
                (new Date(2014, 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date(2015, 4, 1, 0, 0, 0, 0)).getTime()
            )).toBe('1 мая 2014 - 1 мая 2015');
        });

        it('Одинаковые года, разные месяцы', () => {
            expect(getFormattedForDateRange(
                (new Date(2015, 3, 1, 0, 0, 0, 0)).getTime(),
                (new Date(2015, 4, 1, 0, 0, 0, 0)).getTime()
            )).toBe('1 апреля - 1 мая 2015');
        });

        it('Одинаковые года, одинаковые месяцы, разные дни', () => {
            expect(getFormattedForDateRange(
                (new Date(2015, 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date(2015, 4, 3, 0, 0, 0, 0)).getTime()
            )).toBe('1 - 3 мая 2015');
        });

        it('Одинаковые года, одинаковые месяцы, одинаковые дни', () => {
            expect(getFormattedForDateRange(
                (new Date(2015, 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date(2015, 4, 1, 0, 0, 0, 0)).getTime()
            )).toBe('1 мая 2015');
        });

        it('Текущий год, разные месяцы', () => {
            expect(getFormattedForDateRange(
                (new Date((new Date()).getFullYear(), 3, 1, 0, 0, 0, 0)).getTime(),
                (new Date((new Date()).getFullYear(), 4, 1, 0, 0, 0, 0)).getTime()
            )).toBe('1 апреля - 1 мая');
        });

        it('Текущий год, одинаковые месяцы, разные дни', () => {
            expect(getFormattedForDateRange(
                (new Date((new Date()).getFullYear(), 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date((new Date()).getFullYear(), 4, 2, 0, 0, 0, 0)).getTime()
            )).toBe('1 - 2 мая');
        });

        it('Текущий год, одинаковые месяцы, разные дни (но одинаковые дни недели)', () => {
            expect(getFormattedForDateRange(
                (new Date((new Date()).getFullYear(), 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date((new Date()).getFullYear(), 4, 8, 0, 0, 0, 0)).getTime()
            )).toBe('1 - 8 мая');
        });

        it('Текущий год, одинаковые месяцы, одинаковые дни', () => {
            expect(getFormattedForDateRange(
                (new Date((new Date()).getFullYear(), 4, 1, 0, 0, 0, 0)).getTime(),
                (new Date((new Date()).getFullYear(), 4, 1, 0, 0, 0, 0)).getTime()
            )).toBe('1 мая');
        });
    });
});
