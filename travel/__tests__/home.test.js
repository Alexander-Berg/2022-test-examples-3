import {VIEWPORT} from '../constants';

import {TransportType} from '../../../transportType';

import homeMeta from '../home';
import defaultMetaInformation from '../default';

import keyset from '../../../../i18n/meta-default';

const city = 'Нур-Султан';
const cityGenitive = 'Нур-Султана';
const language = 'ru';

const stateSample = {
    tld: 'ru',
    language,
    stationsGroup: [
        {t_type: TransportType.train},
        {t_type: TransportType.plane},
    ],
    directions: [{code: 'xxx'}],
    page: {
        originUrl: 'https://rasp.yandex.ru',
        location: {
            protocol: 'https:',
            pathname: '/',
            path: '/',
        },
    },
    platform: 'desktop',
    currentSettlement: {
        title: city,
        title_genitive: cityGenitive,
    },
    home: {
        isCity: false,
    },
};

const defaultMeta = defaultMetaInformation(stateSample);
const canonical = 'https://rasp.yandex.ru/';
const canonicalUa = 'https://rasp.yandex.ua/?lang=ru';
const expectedTitle =
    'Расписание электричек, поездов, автобусов и самолётов — Яндекс Расписания';
const expectedDescription =
    'Актуальное расписание электричек, автобусов, поездов, самолётов, теплоходов и паромов; онлайн табло вокзалов и аэропортов. Сервис Яндекс Расписания поможет построить маршрут по всему миру, а также выбрать и купить билет онлайн на самолёт, поезд или автобус.';
const expectedMeta = [
    {charset: 'utf-8'},
    {name: 'description', content: expectedDescription},
    {property: 'og:title', content: expectedTitle},
    {property: 'og:description', content: expectedDescription},
    {name: 'viewport', content: VIEWPORT},
    {property: 'og:type', content: 'website'},
    {property: 'og:site_name', content: keyset('og:site_name')},
];
const expectedAlternateLanguageLinks = [
    {
        href: 'https://rasp.yandex.ru/',
        hreflang: 'ru',
        rel: 'alternate',
    },
    {
        href: 'https://rasp.yandex.ua/',
        hreflang: 'uk',
        rel: 'alternate',
    },
    {
        href: 'https://rasp.yandex.ua/?lang=ru',
        hreflang: 'ru-UA',
        rel: 'alternate',
    },
    {
        href: 'https://rasp.yandex.by/',
        hreflang: 'ru-BY',
        rel: 'alternate',
    },
    {
        href: 'https://rasp.yandex.kz/',
        hreflang: 'ru-KZ',
        rel: 'alternate',
    },
    {
        href: 'https://rasp.yandex.uz/',
        hreflang: 'ru-UZ',
        rel: 'alternate',
    },
];

const expectedAlternateLanguageLinksMobile = [
    {
        href: 'https://t.rasp.yandex.ru/',
        hreflang: 'ru',
        rel: 'alternate',
    },
    {
        href: 'https://t.rasp.yandex.ua/',
        hreflang: 'uk',
        rel: 'alternate',
    },
    {
        href: 'https://t.rasp.yandex.ua/?lang=ru',
        hreflang: 'ru-UA',
        rel: 'alternate',
    },
    {
        href: 'https://t.rasp.yandex.by/',
        hreflang: 'ru-BY',
        rel: 'alternate',
    },
    {
        href: 'https://t.rasp.yandex.kz/',
        hreflang: 'ru-KZ',
        rel: 'alternate',
    },
    {
        href: 'https://t.rasp.yandex.uz/',
        hreflang: 'ru-UZ',
        rel: 'alternate',
    },
];

describe('homeMeta', () => {
    it('Вернет десктопную мету', () => {
        const state = stateSample;

        expect(homeMeta(state)).toEqual({
            ...defaultMeta,
            meta: [...expectedMeta, {property: 'og:url', content: canonical}],
            title: expectedTitle,
            link: [
                ...expectedAlternateLanguageLinks,
                {
                    rel: 'canonical',
                    href: canonical,
                },
                {
                    media: 'only screen and (max-width: 640px)',
                    rel: 'alternate',
                    href: 'https://t.rasp.yandex.ru/',
                },
            ],
        });
    });

    it('Вернет мобильную мету', () => {
        const state = {
            ...stateSample,
            page: {
                ...stateSample.page,
                originUrl: 'https://t.rasp.yandex.ru',
            },
            platform: 'mobile',
        };

        expect(homeMeta(state)).toEqual({
            ...defaultMeta,
            meta: [...expectedMeta, {property: 'og:url', content: canonical}],
            title: expectedTitle,
            link: [
                ...expectedAlternateLanguageLinksMobile,
                {
                    rel: 'canonical',
                    href: canonical,
                },
            ],
        });
    });

    it('Вернет десктопную мету для tld = ua & lang = ru', () => {
        const state = {
            ...stateSample,
            tld: 'ua',
            page: {
                ...stateSample.page,
                originUrl: 'https://rasp.yandex.ua',
            },
        };

        expect(homeMeta(state)).toEqual({
            ...defaultMeta,
            meta: [...expectedMeta, {property: 'og:url', content: canonicalUa}],
            title: expectedTitle,
            link: [
                ...expectedAlternateLanguageLinks,
                {
                    rel: 'canonical',
                    href: canonicalUa,
                },
                {
                    media: 'only screen and (max-width: 640px)',
                    rel: 'alternate',
                    href: 'https://t.rasp.yandex.ua/?lang=ru',
                },
            ],
        });
    });

    it('Вернет десктопную мету для другого города', () => {
        const state = {
            ...stateSample,
            stationsGroup: [
                {t_type: TransportType.train},
                {t_type: TransportType.plane},
                {t_type: TransportType.bus},
                {t_type: TransportType.water},
            ],
            directions: [{code: 'xxx'}],
            page: {
                originUrl: 'https://rasp.yandex.ru',
                location: {
                    protocol: 'https:',
                    pathname: '/city/2',
                },
            },
            currentSettlement: {
                title: 'Санкт-Петербург',
                title_genitive: 'Санкт-Петербурга',
                id: 2,
            },
            home: {
                isCity: true,
            },
        };

        const alternateLanguageLinks = [
            {
                href: 'https://rasp.yandex.ru/city/2',
                hreflang: 'ru',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.ua/city/2',
                hreflang: 'uk',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.ua/city/2?lang=ru',
                hreflang: 'ru-UA',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.by/city/2',
                hreflang: 'ru-BY',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.kz/city/2',
                hreflang: 'ru-KZ',
                rel: 'alternate',
            },
            {
                href: 'https://rasp.yandex.uz/city/2',
                hreflang: 'ru-UZ',
                rel: 'alternate',
            },
        ];

        const title =
            'Расписание транспорта Санкт-Петербурга – Яндекс Расписания';
        const description =
            'Актуальное расписание электричек, автобусов, поездов, самолётов и теплоходов; онлайн табло вокзалов и аэропортов Санкт-Петербурга. Сервис Яндекс Расписания поможет построить маршрут по всему миру, а также выбрать и купить билет онлайн на самолёт, поезд или автобус.';
        const canonicalCity = 'https://rasp.yandex.ru/city/2';
        const canonicalCityUa = 'https://rasp.yandex.ua/city/2?lang=ru';
        const meta = [
            {charset: 'utf-8'},
            {name: 'description', content: description},
            {property: 'og:title', content: title},
            {property: 'og:description', content: description},
            {name: 'viewport', content: VIEWPORT},
            {property: 'og:type', content: 'website'},
            {property: 'og:site_name', content: keyset('og:site_name')},
        ];

        // для tld = ru
        expect(homeMeta(state)).toEqual({
            ...defaultMeta,
            meta: [...meta, {property: 'og:url', content: canonicalCity}],
            title,
            link: [
                ...alternateLanguageLinks,
                {
                    rel: 'canonical',
                    href: canonicalCity,
                },
                {
                    media: 'only screen and (max-width: 640px)',
                    rel: 'alternate',
                    href: 'https://t.rasp.yandex.ru/city/2',
                },
            ],
        });

        const uaState = {
            ...state,
            tld: 'ua',
            page: {
                ...state.page,
                originUrl: 'https://rasp.yandex.ua',
            },
        };

        // для tld = ua
        expect(homeMeta(uaState)).toEqual({
            ...defaultMeta,
            meta: [...meta, {property: 'og:url', content: canonicalCityUa}],
            title,
            link: [
                ...alternateLanguageLinks,
                {
                    rel: 'canonical',
                    href: canonicalCityUa,
                },
                {
                    media: 'only screen and (max-width: 640px)',
                    rel: 'alternate',
                    href: 'https://t.rasp.yandex.ua/city/2?lang=ru',
                },
            ],
        });
    });
});
