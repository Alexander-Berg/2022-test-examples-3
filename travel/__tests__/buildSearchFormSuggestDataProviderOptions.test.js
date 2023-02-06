jest.disableAutomock();

import buildSearchFormSuggestDataProviderOptions from '../buildSearchFormSuggestDataProviderOptions';

import {ALL_TYPE, BUS_TYPE} from '../../transportType';

jest.mock('../getSuggestsType', () => jest.fn(t => t));

const props = {
    field: 'from',
    suggests: {
        url: '//suggests.tst.rasp.yandex.ru/',
    },
    language: 'ru',
    from: {
        title: 'Москва',
        key: 'c213',
    },
    to: {
        title: 'Питер',
        key: 'c2',
    },
    transportType: BUS_TYPE,
    nationalVersion: 'ru',
    clientSettlement: {
        region_id: 11131,
        suburban_zone: {
            settlement: {
                id: 51,
            },
            id: 23,
        },
        title: 'Самара',
        timezone: 'Europe/Samara',
        title_with_type: 'г. Самара',
        blablacar_title: 'Самара, Самарская область, Россия',
        id: 51,
        ymap_url: 'https://maps.yandex.ru/51/samara/',
        geo_id: 51,
    },
};

const result = {
    url: '//suggests.tst.rasp.yandex.ru/',
    path: BUS_TYPE,
    query: {
        field: 'from',
        lang: 'ru',
        format: 'old',
        t_type_code: BUS_TYPE,
        other_point: 'c2',
        national_version: 'ru',
        client_city: 51,
    },
};

describe('buildSearchFormSuggestDataProviderOptions', () => {
    it('Должен вернуть правильно сформированный результат', () => {
        expect(buildSearchFormSuggestDataProviderOptions(props)).toEqual(
            result,
        );

        expect(
            buildSearchFormSuggestDataProviderOptions({...props, field: 'to'}),
        ).toEqual({
            ...result,
            query: {...result.query, field: 'to', other_point: 'c213'},
        });

        expect(
            buildSearchFormSuggestDataProviderOptions({
                ...props,
                transportType: ALL_TYPE,
            }),
        ).toEqual({
            ...result,
            path: ALL_TYPE,
            query: {...result.query, t_type_code: null},
        });
    });
});
