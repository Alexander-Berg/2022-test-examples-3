import {VIEWPORT} from '../constants';

import Lang from '../../../../interfaces/Lang';
import Tld from '../../../../interfaces/Tld';
import CountryCode from '../../../../interfaces/state/transport/CountryCode';

import searchUrl, {getCanonicalUrl} from '../../../url/searchUrl';
import getDesktopUrl from '../../../url/getDesktopUrl';
import {getAlternateLanguageLinks} from '../../../url/altLinks';
import search from '../search';
import defaultMetaInformation from '../default';
import isResultEmpty from '../../../search/isResultEmpty';
import {searchWindowTitle} from '../../searchTitle';
import * as platforms from '../../../platforms';
import notFoundTitle from '../../notFoundTitle';
import getSearchDescription from '../../getSearchDescription';
import addNoIndexPageToMeta from '../../addNoIndexPageToMeta';
import {isIOS} from '../../../os';

import metaSearchKeyset from '../../../../i18n/meta-search-v2/ru';
import keyset from '../../../../i18n/meta-default';

const mockNotFoundTitle = 'Заголовок';
const mockNotFoundDescription = 'Описание';

jest.mock('../../notFoundTitle');
jest.mock('../../addNoIndexPageToMeta');
jest.mock('../../getSearchDescription');
jest.mock('../../../../i18n/meta-search-v2/ru');
jest.mock('../../../url/searchUrl');
jest.mock('../../../url/getDesktopUrl', () => jest.fn());
jest.mock('../default');
jest.mock('../../../search/isResultEmpty');
jest.mock('../../searchTitle');
jest.mock('../../../segments/gatherMinPriceData.js');
jest.mock('../../../url/crumble/getJsonLdCrumbsForSearchPage', () => () => ({
    meta: 'breadcrumbs meta',
}));
jest.mock('../../../os');
jest.mock('../../../url/altLinks', () => ({
    ...require.requireActual('../../../url/altLinks'),
    getAlternateLanguageLinks: jest.fn(),
}));
jest.mock('../../../../i18n/page', () => key => {
    if (key === 'title-index') {
        return mockNotFoundTitle;
    }

    if (key === 'description-home') {
        return mockNotFoundDescription;
    }

    return '';
});

const searchDescription = 'mockSearchDescription';
const searchLink =
    'https://rasp.yandex.ua/search/?fromName=Moscow&toName=Yekaterinburg&fromId=c213&toId=c54when=сегодня';
const searchCanonicalUrl =
    'https://rasp.yandex.ua/search/?fromName=Moscow&toName=Yekaterinburg&fromId=c213&toId=c54';
const languageAltLinks = [
    {
        host: 'https://rasp.yandex.ru',
        hreflang: 'ru',
        rel: 'alternate',
    },
    {
        host: 'https://rasp.yandex.ua',
        hreflang: 'uk',
        rel: 'alternate',
    },
];

getSearchDescription.mockReturnValue(searchDescription);
metaSearchKeyset.mockReturnValue(searchDescription);
searchUrl.mockReturnValue(searchLink);
getCanonicalUrl.mockReturnValue(searchCanonicalUrl);
getDesktopUrl.mockImplementation(url => url);
getAlternateLanguageLinks.mockReturnValue(languageAltLinks);
isIOS.mockImplementation(() => false);

const commonMetaInformation = {
    title: 'Яндекс Расписания',
    meta: [
        {name: 'description', content: 'Яндекс Расписания найдет всё и всех'},
        {property: 'og:title', content: 'Яндекс Расписания'},
        {property: 'og:description', content: 'Описание'},
        {name: 'viewport', content: VIEWPORT},
    ],
};

const language = Lang.ru;
const tld = Tld.ua;

const filtering = {filters: {}};
const state = {
    search: {
        context: {from: {}, to: {}},
        segments: [{}, {}, {}],
        seoMetaInfo: {},
        filtering,
        archivalData: null,
    },
    platform: platforms.DESKTOP,
    currencies: {},
    language,
    tld,
    page: {
        name: 'search',
        fetching: false,
        originUrl: '',
        fullUrl: searchLink,
    },
    user: {
        os: {},
        isBot: false,
    },
};
const touchAltLink = {
    href: 'https://t.rasp.yandex.ua/search/?fromName=Moscow&toName=Yekaterinburg&fromId=c213&toId=c54',
    media: 'only screen and (max-width: 640px)',
    rel: 'alternate',
};

const differentCountriesCityContext = {
    from: {country: {code: CountryCode.ru}, key: 'c1'},
    to: {country: {code: CountryCode.by}, key: 'c2'},
};

const differentCountriesStationContext = {
    from: {country: {code: CountryCode.ru}, key: 's1'},
    to: {country: {code: CountryCode.by}, key: 's2'},
};

describe('search', () => {
    it('Проверка что функция Search перезаписывает стандартную meta информацию и возвращает свои данные', () => {
        defaultMetaInformation.mockReturnValue(commonMetaInformation);
        isResultEmpty.mockReturnValue(false);
        searchWindowTitle.mockReturnValue({
            title: 'Расписание рейсов Москва - Санкт-Петербург',
        });

        expect(search(state)).toEqual({
            ...commonMetaInformation,
            title: 'Расписание рейсов Москва - Санкт-Петербург',
            meta: [
                {charset: 'utf-8'},
                {name: 'description', content: searchDescription},
                {
                    property: 'og:title',
                    content: 'Расписание рейсов Москва - Санкт-Петербург',
                },
                {property: 'og:description', content: searchDescription},
                {name: 'viewport', content: VIEWPORT},
                {property: 'og:type', content: 'website'},
                {property: 'og:site_name', content: keyset('og:site_name')},
                {property: 'og:url', content: searchCanonicalUrl},
            ],

            script: [
                {
                    innerHTML: '{"meta":"breadcrumbs meta"}',
                    type: 'application/ld+json',
                },
            ],

            link: [
                {rel: 'canonical', href: searchCanonicalUrl},
                touchAltLink,
                ...languageAltLinks,
            ],
        });

        expect(searchWindowTitle).toBeCalledWith({
            context: state.search.context,
            segments: [{}, {}, {}],
            currencies: {},
            filtering,
            archivalData: null,
        });

        expect(getCanonicalUrl).toBeCalledWith(
            state.search.context,
            tld,
            language,
            state.page.originUrl,
            filtering,
            null,
        );
    });

    it('Проверка что функция Search не возвращает ссылку на touch в адаптивной версии', () => {
        defaultMetaInformation.mockReturnValue(commonMetaInformation);
        isResultEmpty.mockReturnValue(false);
        searchWindowTitle.mockReturnValue({
            title: 'Расписание рейсов Москва - Санкт-Петербург',
        });

        const searchState = {
            ...state,
            platform: platforms.MOBILE,
        };

        expect(search(searchState).link).toEqual([
            {rel: 'canonical', href: searchCanonicalUrl},
            ...languageAltLinks,
        ]);
    });

    it(
        'Проверка что функция Search вызывает NotFound и возвращает мета информацию ' +
            'для ничего не найдено',
        () => {
            defaultMetaInformation.mockReturnValue(commonMetaInformation);
            notFoundTitle.mockReturnValue({
                title: 'Не найден рейс Москва - Абакан',
            });
            isResultEmpty.mockReturnValue(true);

            const notFoundState = {
                ...state,
                search: {
                    context: {from: {}, to: {}},
                    segments: [],
                    seoMetaInfo: {},
                    filtering,
                    archivalData: null,
                },
            };

            expect(search(notFoundState)).toEqual({
                ...commonMetaInformation,
                title: 'Не найден рейс Москва - Абакан',
                meta: [
                    {charset: 'utf-8'},
                    {name: 'description', content: searchDescription},
                    {property: 'og:title', content: mockNotFoundTitle},
                    {
                        property: 'og:description',
                        content: mockNotFoundDescription,
                    },
                    {name: 'viewport', content: VIEWPORT},
                    {property: 'og:type', content: 'website'},
                    {property: 'og:site_name', content: keyset('og:site_name')},
                    {property: 'og:url', content: searchCanonicalUrl},
                ],

                script: [
                    {
                        innerHTML: '{"meta":"breadcrumbs meta"}',
                        type: 'application/ld+json',
                    },
                ],

                link: [
                    {rel: 'canonical', href: searchCanonicalUrl},
                    touchAltLink,
                    ...languageAltLinks,
                ],
            });

            expect(notFoundTitle).toBeCalledWith({
                context: state.search.context,
            });

            expect(getCanonicalUrl).toBeCalledWith(
                state.search.context,
                tld,
                language,
                state.page.originUrl,
                filtering,
                null,
            );
        },
    );

    it('Если нет канонического урла в og:url возвращаем ссылку на морду', () => {
        defaultMetaInformation.mockReturnValue(commonMetaInformation);
        isResultEmpty.mockReturnValue(false);
        searchWindowTitle.mockReturnValue({
            title: 'Расписание рейсов Москва - Санкт-Петербург',
        });
        getCanonicalUrl.mockReturnValueOnce('');

        const {meta} = search(state);

        expect(meta).toEqual([
            {charset: 'utf-8'},
            {name: 'description', content: searchDescription},
            {
                property: 'og:title',
                content: 'Расписание рейсов Москва - Санкт-Петербург',
            },
            {property: 'og:description', content: searchDescription},
            {name: 'viewport', content: VIEWPORT},
            {property: 'og:type', content: 'website'},
            {property: 'og:site_name', content: keyset('og:site_name')},
            {property: 'og:url', content: `https://rasp.yandex.${tld}/`},
        ]);
    });

    it('Для реальных пользователей на iOS вместо канонического урла возвращаем ссылку на поиск', () => {
        isIOS.mockReturnValueOnce(true);
        isResultEmpty.mockReturnValue(false);
        defaultMetaInformation.mockReturnValue(commonMetaInformation);
        searchWindowTitle.mockReturnValue({
            title: 'Расписание рейсов Москва - Санкт-Петербург',
        });

        const metaLinks = search(state).link;

        expect(metaLinks[0]).toEqual({
            rel: 'canonical',
            href: searchLink,
        });
    });

    it('Для ботов на iOS отдаем канонический урл', () => {
        isIOS.mockReturnValueOnce(true);
        isResultEmpty.mockReturnValue(false);
        defaultMetaInformation.mockReturnValue(commonMetaInformation);
        searchWindowTitle.mockReturnValue({
            title: 'Расписание рейсов Москва - Санкт-Петербург',
        });

        const botUser = {
            ...state.user,
            isBot: true,
        };
        const botState = {
            ...state,
            user: botUser,
        };

        const metaLinks = search(botState).link;

        expect(metaLinks[0]).toEqual({
            rel: 'canonical',
            href: searchCanonicalUrl,
        });
    });

    it('Если нельзя построить canonical и города не находятся в разных странах, должен вызвать функцию addNoIndexPageToMeta', () => {
        getCanonicalUrl.mockReturnValueOnce('');

        search(state);

        expect(addNoIndexPageToMeta).toBeCalled();
    });

    it('Если нельзя построить canonical и города находятся в разных странах, не должен вызвать функцию addNoIndexPageToMeta', () => {
        getCanonicalUrl.mockReturnValueOnce('');

        search({
            ...state,
            search: {
                ...state.search,
                context: differentCountriesCityContext,
            },
        });

        expect(addNoIndexPageToMeta).not.toBeCalled();
    });

    it('Если нельзя построить canonical и станции находятся в разных странах, должен вызвать функцию addNoIndexPageToMeta', () => {
        getCanonicalUrl.mockReturnValueOnce('');

        search({
            ...state,
            search: {
                ...state.search,
                context: differentCountriesStationContext,
            },
        });

        expect(addNoIndexPageToMeta).toBeCalled();
    });
});
