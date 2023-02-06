import React from 'react';

import {searchArchivalH1} from '../searchTitle';

import insertMarkupIntoKey from '../../../i18n/_utils/insertMarkupIntoKey';

jest.mock('../../../i18n/_utils/insertMarkupIntoKey', () => jest.fn());

const context = {
    from: {
        titleGenitive: 'Москвы',
        preposition: 'в',
        key: 'c213',
        popularTitle: 'Москва',
        timezone: 'Europe/Moscow',
        settlement: {slug: 'moscow', title: 'Москва'},
        slug: 'moscow',
        shortTitle: 'Москва',
        title: 'Москва',
        country: {
            railwayTimezone: 'Europe/Moscow',
            code: 'RU',
            title: 'Россия',
        },
        region: {title: 'Москва и Московская область'},
        titleLocative: 'Москве',
        titleAccusative: 'Москву',
    },
    to: {
        titleGenitive: 'Санкт-Петербурга',
        preposition: 'в',
        key: 'c2',
        popularTitle: 'Санкт-Петербург',
        timezone: 'Europe/Moscow',
        settlement: {slug: 'saint-petersburg', title: 'Санкт-Петербург'},
        slug: 'saint-petersburg',
        shortTitle: 'Санкт-Петербург',
        title: 'Санкт-Петербург',
        country: {
            railwayTimezone: 'Europe/Moscow',
            code: 'RU',
            title: 'Россия',
        },
        region: {title: 'Санкт-Петербург и Ленинградская область'},
        titleLocative: 'Санкт-Петербурге',
        titleAccusative: 'Санкт-Петербург',
    },
    when: {
        text: 'на все дни',
        hint: 'на все дни',
        special: 'all-days',
        formatted: 'на все дни',
    },
    time: {now: 1588833824336},
};

const archivalData = {
    canonical: {
        pointFrom: 'moscow',
        pointTo: 'saint-petersburg',
        transportType: 'all',
    },
    transportTypes: ['bus', 'train'],
};

const keysetReturnValue = [
    'Расписание транспорта и билеты на поезд и автобус из Москвы в Санкт-Петербург',
];

const notFoundArchivalDataOnDate = {
    title: [
        <span key={1}>
            Расписание транспорта и билеты на поезд и автобус из Москвы в
            Санкт-Петербург
        </span>,
    ],
    subtitle: '8 november 2018, thursday',
};

describe('searchArchivalH1', () => {
    it('searchArchivalH1. Для поиска на все дни должна вернуть верные title без subtitle', () => {
        insertMarkupIntoKey.mockReturnValueOnce(keysetReturnValue);

        const {title, subtitle} = searchArchivalH1({context, archivalData});

        expect(title.children).toEqual(
            notFoundArchivalDataOnDate.title.children,
        );
        expect(subtitle).toEqual('');
    });

    it('searchArchivalH1. Для поиска на дату должна вернуть верные title и subtitle', () => {
        insertMarkupIntoKey.mockReturnValueOnce(keysetReturnValue);

        const {title, subtitle} = searchArchivalH1({
            context: {
                ...context,
                when: {
                    text: '8 ноября',
                    hint: '8 ноября',
                    date: '2018-11-08',
                    formatted: '8 ноября',
                    nextDate: '2018-11-09',
                },
            },
            archivalData,
        });

        expect(title.children).toEqual(
            notFoundArchivalDataOnDate.title.children,
        );
        expect(subtitle).toEqual(notFoundArchivalDataOnDate.subtitle);
    });
});
