import {HelmetProps} from 'react-helmet';

import {VIEWPORT} from '../constants';

import IState from '../../../../interfaces/state/IState';
import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';
import Platform from '../../../../interfaces/Platform';
import StationSubtype from '../../../../interfaces/state/station/StationSubtype';
import StationEventList from '../../../../interfaces/state/station/StationEventList';
import StationType from '../../../../interfaces/state/station/StationType';

import stationMeta from '../station';

jest.mock('../../../../i18n/meta-default', () =>
    jest.fn((key: string): string => {
        switch (key) {
            case 'default-title':
                return 'Стандартный заголовок';
            case 'description':
                return 'Стандартное описание';
            case 'og:site_name':
                return 'Расписания';
        }

        return '';
    }),
);
jest.mock('../../../../i18n/meta-station', () =>
    jest.fn((key: string, params: Record<string, any>): string => {
        switch (key) {
            // В моках можно нельзя использовать переменные вне области видимости,
            // поэтому есть такого рода дублирование текста
            case 'title-train-arrival-popular':
                return `Популярный заголовок для прибытия поездов ${params.title}`;
            case 'title-train-arrival':
                return `Заголовок для прибытия поездов ${params.title}`;
            case 'title-train-departure-popular':
                return `Популярный заголовок для отправления поездов ${params.title}`;
            case 'title-train-departure':
                return `Заголовок для отправления поездов ${params.title}`;
            case 'title-suburban-popular':
                return `Популярный заголовок для электричек ${params.titleGenitive}`;
            case 'title-suburban':
                return `Заголовок для электричек ${params.titleGenitive}`;
            case 'title-tablo-arrival-popular':
                return `Популярный заголовок для прибытия табло ${params.title}`;
            case 'title-tablo-arrival':
                return `Заголовок для прибытия табло ${params.title}`;
            case 'title-tablo-departure-popular':
                return `Популярный заголовок для отправления табло ${params.title}`;
            case 'title-tablo-departure':
                return `Заголовок для отправления табло ${params.title}`;
            case 'default-title-train':
                return `Стандартный заголовок без подтипа ${params.titleDative}`;
            case 'description-train-popular':
                return `Популярное описание поезда ${params.title}`;
            case 'description-train':
                return `Описание поезда ${params.titleDative}`;
            case 'description-suburban':
                return `Описание электрички ${params.title}`;
            case 'description-tablo-popular':
                return `Популярное описание табло ${params.title}`;
            case 'description-tablo':
                return `Описание табло ${params.titleDative}`;
            case 'default-description-train':
                return `Стандартное описание без подтипа ${params.titleDative}`;
        }

        return '';
    }),
);
jest.mock('../../../url/stationUrl.ts', () => ({
    stationUrl: jest.fn(() => '/station/9607601/'),
}));

const titleWithoutDot = 'Кукухи-Пасс';
const state = {
    station: {
        id: 9607601,
        titleGenitive: 'Кукух-Пасс.',
        title: 'Кукухи-Пасс.',
        fullTitle: 'станция Кукухи-Пасс.',
        fullTitleDative: 'станции Кукухи-Пасс.',
        fullTitleGenitive: 'станции Кукух-Пасс.',
        terminals: [],
    },
    page: {
        originUrl: 'https://rasp.yandex.ru',
        location: {
            protocol: 'https:',
            pathname: '/station/9607601/',
        },
    },
    tld: Tld.ru,
    language: Lang.ru,
    searchForm: {time: {now: 1}},
    platform: Platform.desktop,
} as unknown as IState;

const title = {
    train: {
        arrival: {
            popular: `Популярный заголовок для прибытия поездов ${state.station.title}`,
            normal: `Заголовок для прибытия поездов ${state.station.title}`,
        },
        departure: {
            popular: `Популярный заголовок для отправления поездов ${state.station.title}`,
            normal: `Заголовок для отправления поездов ${state.station.title}`,
        },
    },
    suburban: {
        popular: `Популярный заголовок для электричек ${state.station.fullTitleGenitive}`,
        normal: `Заголовок для электричек ${titleWithoutDot}`,
    },
    tablo: {
        arrival: {
            popular: `Популярный заголовок для прибытия табло ${state.station.title}`,
            normal: `Заголовок для прибытия табло ${state.station.title}`,
        },
        departure: {
            popular: `Популярный заголовок для отправления табло ${state.station.title}`,
            normal: `Заголовок для отправления табло ${state.station.title}`,
        },
    },
    noSubtype: `Стандартный заголовок без подтипа ${state.station.fullTitleDative}`,
    default: 'Стандартный заголовок',
};
const description = {
    train: {
        popular: `Популярное описание поезда ${state.station.fullTitleGenitive}`,
        normal: `Описание поезда ${state.station.fullTitleDative}`,
    },
    suburban: `Описание электрички ${state.station.fullTitleDative}`,
    tablo: {
        popular: `Популярное описание табло ${state.station.fullTitleGenitive}`,
        normal: `Описание табло ${state.station.fullTitleDative}`,
    },
    noSubtype: `Стандартное описание без подтипа ${state.station.fullTitleDative}`,
    default: 'Стандартное описание',
};
const alternativeLanguageLinks = [
    {
        rel: 'alternate',
        href: 'https://rasp.yandex.ru/station/9607601/',
        hreflang: 'ru',
    },
    {
        rel: 'alternate',
        href: 'https://rasp.yandex.ua/station/9607601/',
        hreflang: 'uk',
    },
    {
        rel: 'alternate',
        href: 'https://rasp.yandex.ua/station/9607601/?lang=ru',
        hreflang: 'ru-UA',
    },
    {
        rel: 'alternate',
        href: 'https://rasp.yandex.by/station/9607601/',
        hreflang: 'ru-BY',
    },
    {
        rel: 'alternate',
        href: 'https://rasp.yandex.kz/station/9607601/',
        hreflang: 'ru-KZ',
    },
    {
        rel: 'alternate',
        href: 'https://rasp.yandex.uz/station/9607601/',
        hreflang: 'ru-UZ',
    },
];
const alternativeLanguageLinksTouch = alternativeLanguageLinks.map(link => {
    return {
        ...link,
        href: link.href.replace(/^https:\/\//, 'https://t.'),
    };
});
const baseMeta = [
    {charset: 'utf-8'},
    {name: 'viewport', content: VIEWPORT},
    {property: 'og:type', content: 'website'},
    {property: 'og:site_name', content: 'Расписания'},
    {property: 'og:url', content: 'https://rasp.yandex.ru/station/9607601/'},
];

function checkExpectedMeta(result: HelmetProps, expectedMeta: object[]): void {
    expect(result.meta).toEqual(expect.arrayContaining(expectedMeta));
    expect(result.meta?.length).toBe(expectedMeta.length);
}

describe('stationMeta', () => {
    it('Возвращает корректный title и мету для расписания поездов', () => {
        let result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.arrival,
                settlement: {
                    title: 'title',
                },
                hasPopularTitle: true,
            },
        } as IState);
        let expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.popular},
        ];

        expect(result.title).toBe(title.train.arrival.popular);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.arrival,
                settlement: {
                    title: 'title',
                },
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.normal},
        ];
        expect(result.title).toBe(title.train.arrival.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.arrival,
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.popular},
        ];
        expect(result.title).toBe(title.train.arrival.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.arrival,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.normal},
        ];
        expect(result.title).toBe(title.train.arrival.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.departure,
                settlement: {
                    title: 'title',
                },
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.popular},
        ];
        expect(result.title).toBe(title.train.departure.popular);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.departure,
                settlement: {
                    title: 'title',
                },
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.normal},
        ];
        expect(result.title).toBe(title.train.departure.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.departure,
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.popular},
        ];
        expect(result.title).toBe(title.train.departure.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.train,
                event: StationEventList.departure,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.train.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.train.normal},
        ];
        expect(result.title).toBe(title.train.departure.normal);
        checkExpectedMeta(result, expectedMeta);
    });

    it('Возвращает корректный title и мету для расписания электричек', () => {
        let result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.suburban,
                settlement: {
                    title: 'title',
                },
                hasPopularTitle: true,
            },
        } as IState);
        let expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.suburban},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.suburban},
        ];

        expect(result.title).toBe(title.suburban.popular);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.suburban,
                settlement: {
                    title: 'title',
                },
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.suburban},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.suburban},
        ];
        expect(result.title).toBe(title.suburban.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.suburban,
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.suburban},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.suburban},
        ];
        expect(result.title).toBe(title.suburban.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.suburban,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.suburban},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.suburban},
        ];
        expect(result.title).toBe(title.suburban.normal);
        checkExpectedMeta(result, expectedMeta);
    });

    it('Возвращает корректный title и мету для расписания табло', () => {
        let result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.arrival,
                settlement: {
                    title: 'title',
                },
                hasPopularTitle: true,
            },
        } as IState);
        let expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.popular},
        ];

        expect(result.title).toBe(title.tablo.arrival.popular);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.arrival,
                settlement: {
                    title: 'title',
                },
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.normal},
        ];
        expect(result.title).toBe(title.tablo.arrival.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.arrival,
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.popular},
        ];
        expect(result.title).toBe(title.tablo.arrival.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.arrival,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.normal},
        ];
        expect(result.title).toBe(title.tablo.arrival.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.departure,
                settlement: {
                    title: 'title',
                },
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.popular},
        ];
        expect(result.title).toBe(title.tablo.departure.popular);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.departure,
                settlement: {
                    title: 'title',
                },
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.normal},
        ];
        expect(result.title).toBe(title.tablo.departure.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.departure,
                hasPopularTitle: true,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.popular},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.popular},
        ];
        expect(result.title).toBe(title.tablo.departure.normal);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                currentSubtype: StationSubtype.tablo,
                event: StationEventList.departure,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.tablo.normal},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.tablo.normal},
        ];
        expect(result.title).toBe(title.tablo.departure.normal);
        checkExpectedMeta(result, expectedMeta);
    });

    it('Возвращает корректный title и мету для случаев необработанного подтипа или при его отсутствии', () => {
        let result = stationMeta({
            ...state,
            station: {
                ...state.station,
                type: StationType.railroad,
            },
        });
        let expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.noSubtype},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.noSubtype},
        ];

        expect(result.title).toBe(title.noSubtype);
        checkExpectedMeta(result, expectedMeta);

        result = stationMeta({
            ...state,
            station: {
                ...state.station,
                type: StationType.railroad,
                currentSubtype: 'another' as StationSubtype,
            },
        } as IState);
        expectedMeta = [
            ...baseMeta,
            {name: 'description', content: description.default},
            {property: 'og:title', content: result.title},
            {property: 'og:description', content: description.default},
        ];
        expect(result.title).toBe(title.default);
        checkExpectedMeta(result, expectedMeta);
    });

    it('Возвращает корректный массив ссылок', () => {
        expect(stationMeta(state).link).toEqual([
            {rel: 'canonical', href: 'https://rasp.yandex.ru/station/9607601/'},
            {
                media: 'only screen and (max-width: 640px)',
                rel: 'alternate',
                href: 'https://t.rasp.yandex.ru/station/9607601/',
            },
            ...alternativeLanguageLinks,
        ]);

        expect(
            stationMeta({
                ...state,
                platform: Platform.mobile,
            }).link,
        ).toEqual([
            {rel: 'canonical', href: 'https://rasp.yandex.ru/station/9607601/'},
            ...alternativeLanguageLinksTouch,
        ]);
    });
});
