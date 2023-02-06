import {URLs} from 'constants/urls';
import {TLD} from 'constants/tld';

import {WHEN_SPECIAL_VALUE} from 'types/common/When';
import {ETimeOfDay} from 'utilities/dateUtils/types';

import {parseDate} from 'utilities/dateUtils';
import {getTrainsSearchUrl} from 'projects/trains/lib/urls/getTrainsSearchUrl';

describe('getTrainsSearchUrl', () => {
    test('Должен вернуть url для специального значения today', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: WHEN_SPECIAL_VALUE.TODAY,
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/?when=today`);
    });

    test('Должен вернуть url для специального значения tomorrow', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: WHEN_SPECIAL_VALUE.TOMORROW,
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/?when=tomorrow`);
    });

    test('Должен вернуть url для специального значения all-days', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: WHEN_SPECIAL_VALUE.ALL_DAYS,
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/`);
    });

    test('Должен вернуть url для даты, заданной строкой', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: '2018-12-20',
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/?when=2018-12-20`);
    });

    test('Должен вернуть url для даты, заданной как Date', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: new Date('2018-12-20'),
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/?when=2018-12-20`);
    });

    test('Должен вернуть url для даты, заданной как DateType', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: parseDate('2018-12-20'),
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/?when=2018-12-20`);
    });

    test('Должен вернуть url на все дни, если дата не задана', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/`);
    });

    test('Должен вернуть url на все дни, если дата является пустой строкой', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: '',
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/`);
    });

    test('Должен вернуть url на все дни, если дата является невалидным значением Date', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: new Date('invalid'),
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/`);
    });

    test('Должен вернуть url на все дни, если дата является невалидным значением DateType', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: parseDate('invalid'),
                },
            }),
        ).toBe(`${URLs.trains}/moscow--yekaterinburg/`);
    });

    test('Должен вернуть url с параметрами для фильтров', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: '2019-06-25',
                },
                filters: {
                    stationFrom: ['100'],
                    arrival: [ETimeOfDay.DAY, ETimeOfDay.NIGHT],
                },
            }),
        ).toBe(
            `${URLs.trains}/moscow--yekaterinburg/?arrival=day&arrival=night&stationFrom=100&when=2019-06-25`,
        );
    });

    test('Должен вернуть url дополнительными query-параметрами', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: '2019-06-25',
                },
                query: {
                    additional: 'data',
                },
            }),
        ).toBe(
            `${URLs.trains}/moscow--yekaterinburg/?additional=data&when=2019-06-25`,
        );
    });

    test('Должен вернуть url с параметрами для фильтров и дополнительными query-параметрами', () => {
        expect(
            getTrainsSearchUrl({
                context: {
                    from: 'moscow',
                    to: 'yekaterinburg',
                    when: '2019-06-25',
                },
                filters: {
                    arrival: [ETimeOfDay.DAY],
                },
                query: {
                    additional: 'data',
                },
            }),
        ).toBe(
            `${URLs.trains}/moscow--yekaterinburg/?additional=data&arrival=day&when=2019-06-25`,
        );
    });

    test('Должен вернуть url с origin', () => {
        expect(
            getTrainsSearchUrl(
                {
                    context: {
                        from: 'moscow',
                        to: 'yekaterinburg',
                        when: WHEN_SPECIAL_VALUE.ALL_DAYS,
                    },
                },
                {
                    withOrigin: true,
                    tld: TLD.RU,
                },
            ),
        ).toBe(`https://travel.yandex.ru${URLs.trains}/moscow--yekaterinburg/`);
    });
});
