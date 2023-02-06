import {CHAR_EM_DASH, CHAR_NBSP} from '../../../../../common/lib/stringUtils';

import Tld from '../../../../interfaces/Tld';
import Lang from '../../../../interfaces/Lang';
import {FilterTransportType} from '../../../transportType';
import DateSpecialValue from '../../../../interfaces/date/DateSpecialValue';

import getJsonLdCrumbsForSearchPage from '../getJsonLdCrumbsForSearchPage';

const keyset = jest.fn((name, params) => {
    switch (name) {
        case 'bus':
            return 'автобусы';
        case 'main-page':
            return 'Главная';
        case 'on-date':
            return `на ${params.date}`;
        case 'on-all-days':
            return `Расписания и цены ${params.points}`;
        case 'plane':
            return 'самолёты';
        case 'suburban':
            return 'электрички';
        case 'train':
            return 'поезда';
        case 'trains':
            return 'Расписание и билеты на поезда';
        case 'trains-all-day':
            return `Расписание и билеты на поезда ${params.points}`;
    }
});

jest.setMock('../../../../i18n/breadcrumbs/ru', keyset);

const calendarKeyset = jest.fn(name => {
    if (name === 'all-days') {
        return 'на все дни';
    }
});

jest.setMock('../../../../i18n/calendar/ru', calendarKeyset);

const page = {
    originUrl: 'https://rasp.yandex.ru',
    fullUrl: 'https://rasp.yandex.ru/search/',
};

const tld = Tld.ru;
const language = Lang.ru;

describe('getJsonLdCrumbsForSearchPage', () => {
    it('Верный JSON-LD только с главной страницей', () => {
        const context = {
            language,
            transportType: FilterTransportType.all,
            when: {
                special: 'all-days',
            },
        };
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            page,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
            ],
        });
    });

    it('Верный JSON-LD с главной страницей и страницей города', () => {
        const context = {
            language,
            from: {
                key: 'c213',
                title: 'Москва',
            },
            transportType: FilterTransportType.all,
            when: {
                special: 'all-days',
            },
        };
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            page,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/city/213',
                        name: 'Москва',
                    },
                },
            ],
        });
    });

    it('Верный JSON-LD с главной страницей и страницей станции', () => {
        const context = {
            language,
            from: {
                key: 's9607449',
                title: 'Первоуральск',
            },
            transportType: FilterTransportType.all,
            when: {
                special: 'all-days',
            },
        };
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            page,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/9607449',
                        name: 'Первоуральск',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD с главной страницей,
        страницей города,
        страницей поиска на все дни по направлению`, () => {
        const context = {
            language,
            from: {
                key: 'c213',
                slug: 'moscow',
                title: 'Москва',
            },
            to: {
                key: 'c54',
                slug: 'yekaterinburg',
                title: 'Екатеринбург',
            },
            transportType: FilterTransportType.all,
            when: {
                text: 'на все дни',
            },
        };
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            page,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/city/213',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/all-transport/moscow--yekaterinburg',
                        name: `Расписание Москва${CHAR_NBSP}${CHAR_EM_DASH} Екатеринбург`,
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD с главной страницей,
        страницей города+транспорта,
        страницей станции,
        поиском электричками на сегодня,
        поиском "Ласточками"`, () => {
        const context = {
            language,
            from: {
                key: 's213312',
                slug: 'moscow-station',
                title: 'Станция Москва',
                settlement: {
                    slug: 'moscow',
                    title: 'Москва',
                },
            },
            to: {
                key: 's545454',
                slug: 'yekaterinburg-pass',
                title: 'Екатеринбург-Пасс',
            },
            transportType: FilterTransportType.suburban,
            when: {
                text: 'сегодня',
                special: DateSpecialValue.today,
            },
            canonical: {
                pointFrom: 'moscow-station',
                pointTo: 'yekaterinburg-pass',
            },
        };
        const pageLastochka = {
            ...page,
            fullUrl:
                'https://rasp.yandex.ru/lastochka/moscow-station--yekaterinburg-pass/',
        };
        const filteringWithLastochka = {filters: {lastochka: {value: true}}};
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            pageLastochka,
            filteringWithLastochka,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/suburban/moscow',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/station/213312',
                        name: 'Станция Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': 'https://rasp.yandex.ru/suburban/moscow-station--yekaterinburg-pass/today',
                        name: 'Расписание электричек Станция Москва — Екатеринбург-Пасс',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 5,
                    item: {
                        '@id': 'https://rasp.yandex.ru/lastochka/moscow-station--yekaterinburg-pass/',
                        name: '«Ласточка»',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD с главной страницей,
        страницей города,
        поиском по всем транспортам по направлению на все дни,
        поиском по направлению на все дни по транспорту`, () => {
        const context = {
            language,
            from: {
                key: 'c213',
                slug: 'moscow',
                title: 'Москва',
            },
            to: {
                key: 'c54',
                slug: 'yekaterinburg',
                title: 'Екатеринбург',
            },
            transportType: FilterTransportType.plane,
            when: {
                text: 'на все дни',
            },
        };
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            page,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/city/213',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/all-transport/moscow--yekaterinburg',
                        name: `Расписание Москва${CHAR_NBSP}${CHAR_EM_DASH} Екатеринбург`,
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane/moscow--yekaterinburg',
                        name: 'самолёты',
                    },
                },
            ],
        });
    });

    it(`Верный JSON-LD с главной страницей,
        страницей города,
        поиском по всем транспортам на все дни по направлению,
        поиском по транспорту по направлению на все дни,
        поиском по транспорту по направлению на дату`, () => {
        const context = {
            language,
            from: {
                key: 'c213',
                slug: 'moscow',
                title: 'Москва',
            },
            to: {
                key: 'c54',
                slug: 'yekaterinburg',
                title: 'Екатеринбург',
            },
            transportType: FilterTransportType.plane,
            when: {
                text: '1 января',
            },
        };
        const result = getJsonLdCrumbsForSearchPage(
            context,
            tld,
            language,
            page,
        );

        expect(result).toEqual({
            '@context': 'http://schema.org',
            '@type': 'BreadcrumbList',
            itemListElement: [
                {
                    '@type': 'ListItem',
                    position: 1,
                    item: {
                        '@id': 'https://rasp.yandex.ru',
                        name: 'Главная',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 2,
                    item: {
                        '@id': 'https://rasp.yandex.ru/city/213',
                        name: 'Москва',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 3,
                    item: {
                        '@id': 'https://rasp.yandex.ru/all-transport/moscow--yekaterinburg',
                        name: `Расписание Москва${CHAR_NBSP}${CHAR_EM_DASH} Екатеринбург`,
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 4,
                    item: {
                        '@id': 'https://rasp.yandex.ru/plane/moscow--yekaterinburg',
                        name: 'самолёты',
                    },
                },
                {
                    '@type': 'ListItem',
                    position: 5,
                    item: {
                        '@id': page.fullUrl,
                        name: 'на 1 января',
                    },
                },
            ],
        });
    });
});
