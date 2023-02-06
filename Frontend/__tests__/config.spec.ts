import { get } from 'lodash';

import pages from '../../pages';
import fetchers from '../../fetchers';

import { getPage } from '../index';

const PAGES = [
    {
        page: 'КМ',
        url: '/product--smartfon-apple-iphone-7-32gb/14206636',
        params: {
            entityId: '14206636',
            slug: 'smartfon-apple-iphone-7-32gb',
            pageName: 'product',
        },
    },
    {
        page: 'offer',
        url: '/offer/uFeoMMiWPcLkdT7i4VSKXA?shopId=37758&hid=91491',
        params: {
            entityId: 'uFeoMMiWPcLkdT7i4VSKXA',
            pageName: 'offer',
            shopId: '37758',
            hid: '91491',
        },
    },
];

describe('Корректность описания страниц в конфиге', () => {
    PAGES.forEach(({ page, url, params }) => {
        describe(`Страница ${page}`, () => {
            it('Правильный парсинг ссылки', () => {
                const { parsedParams } = getPage(url);

                expect(parsedParams).toEqual(params);
            });

            it('Наличие ф-ии для layout', () => {
                const { pageData } = getPage(url);

                const layoutName = get(pageData, 'layout', 'unknown') as string;
                const layoutCreator = pages[layoutName];

                expect(typeof layoutCreator).toEqual('function');
            });

            it('Наличие ф-ии для fetcher', () => {
                const { pageData } = getPage(url);

                const fetcherName = get(pageData, 'fetcher', 'unknown') as string;
                const fetcher = fetchers[fetcherName];

                expect(typeof fetcher).toEqual('function');
            });
        });
    });
});
