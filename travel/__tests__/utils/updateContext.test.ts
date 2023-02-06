import {ITrainsFilledSearchContext} from 'reducers/trains/context/types';

import updateContext from 'projects/trains/lib/context/updateContext';

const searchContext = {
    from: {
        title: 'Москва',
        slug: 'moscow',
        key: 'c213',
    },
    to: {
        title: 'Екатеринбург',
        slug: 'ekaterinburg',
        key: 'c54',
    },
    when: '2019-01-31',
} as ITrainsFilledSearchContext;

const commonRaspSearchContext = {
    original: {
        nearest: true,
        pointFrom: {key: '', title: ''},
        pointTo: {key: '', title: ''},
    },
    search: {
        nearest: true,
        pointFrom: {key: '', title: ''},
        pointTo: {key: '', title: ''},
    },
    isChanged: false,
    latestDatetime: '2019-02-03T01:00:00+00:00',
    transportTypes: [],
};

describe('updateContext', () => {
    test('Расширяет поисковый диапазон необходимыми данными', () => {
        const raspSearchContext = {
            ...commonRaspSearchContext,
            isChanged: false,
            latestDatetime: '2019-02-03T01:00:00+00:00',
        };

        expect(updateContext(searchContext, raspSearchContext)).toEqual({
            ...searchContext,
            latestDatetime: '2019-02-03T01:00:00+00:00',
        });
    });

    test('Если произошло сужение/расширение поиска - изменит данные контекста', () => {
        const original = {
            nearest: true,
            pointFrom: {
                ...searchContext.from,
            },
            pointTo: {
                ...searchContext.to,
            },
        };
        const search = {
            nearest: true,
            pointFrom: {
                title: 'Казанский вокзал',
                key: 's2000003',
            },
            pointTo: {
                title: 'Екатеринбург Пассажирский',
                key: 's9607404',
            },
        };

        expect(
            updateContext(searchContext, {
                ...commonRaspSearchContext,
                original,
                search,
                isChanged: true,
                latestDatetime: '2019-02-03T01:00:00+00:00',
            }),
        ).toEqual({
            ...searchContext,
            isChanged: true,
            latestDatetime: '2019-02-03T01:00:00+00:00',
            from: {
                title: 'Казанский вокзал',
                key: 's2000003',
                slug: 'moscow',
            },
            to: {
                title: 'Екатеринбург Пассажирский',
                key: 's9607404',
                slug: 'ekaterinburg',
            },
            original,
        });
    });
});
