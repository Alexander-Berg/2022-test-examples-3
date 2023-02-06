/* eslint-disable @typescript-eslint/camelcase */
import supertest from 'supertest';
import nock from 'nock';

import app from '../../../../../src/app';
import raiseMocks from '../../../helpers/raise-mocks';
import Mock from '../../../../types/mock';
import * as tvm from '../../../../../src/API/yandex/tvm';
import getShopsByDomain from '../../../../../src/helper/get-shops-by-domain';

jest.mock('../../../../../src/API/yandex/tvm');

interface TvmMock {
    ticket: () => Promise<string>;
}
const tvmMock = tvm as jest.Mocked<TvmMock>;
tvmMock.ticket.mockImplementation(() => new Promise((resolve) => resolve('ticket')));

jest.mock('../../../../../src/helper/get-shops-by-domain');
const getShopsByDomainMock = getShopsByDomain as jest.MockedFunction<typeof getShopsByDomain>;

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks 1 test case (with www) */
const yabroFilterMock: Mock = require('../../__mocks__/yabro-filter');
const laasMock = require('../../__mocks__/laas.mock');
const searchEmptyMock = require('../../__mocks__/empty-url-search-report');
const categoriesMatchMock = require('./__mocks__/categories-match.model-by-url-report');
const searchMock = require('./__mocks__/search.model-by-url-report');
const modelMock = require('./__mocks__/model.model-by-url-report');
const modelOffersPremiumMock = require('./__mocks__/model-offers-premium.model-by-url-report');
const modelOffersMock = require('./__mocks__/model-offers.model-by-url-report');
const modelOpinionsMock = require('./__mocks__/opinions.model-by-url-report');
const socialProfileMock = require('./__mocks__/social-profile.model-by-url-report');
const shopInfoMock = require('./__mocks__/shops-info.model-by-url-report');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.model-by-url-report');

/* Mocks 2 test case (no www) */
const searchNoWwwMock = require('./__mocks__/search.model-by-url-no-www-report');
const modelNoWwwMock = require('./__mocks__/model.model-by-url-no-www-report');
const modelOffersNoWwwPremiumMock = require('./__mocks__/model-offers-premium.model-by-url-no-www-report');
const modelOffersNoWwwMock = require('./__mocks__/model-offers.model-by-url-no-www-report');
const modelOffersNoWwwMockEmpty = require('./__mocks__/model-offers-premium.model-by-url-no-www-report-empty');

/* Stubs */
const responseStubWww = require('./__stubs__/response-www');
const responseStubNoWww = require('./__stubs__/response-without-www');

const request = supertest(app);

let mockScopes = raiseMocks();

const bodyWww = {
    dds: '7ee5bfe915fd4774ebb531eaad0bef88',
    v: '202012110221',
    isSelectorExists: true,
    transaction_id: 'kiq6fxylbof5k3u23nv96x0vsq3lt5su',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1400',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1607335854763',
        installId: '31961340-104f-4820-bd9b-7734d384f120',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: { width: 1073, height: 718 },
    screen_resolution: { ratio: 2, width: 1440, height: 900 },
    viewport: { width: 4410, height: 666 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: true,
    url: 'https://www.citilink.ru/catalog/computers_and_notebooks/parts/powersupply/1049262/',
    pictures_by_tg: ['https://items.s1.citilink.ru/1049262_v01_b.jpg'],
    h1_by_df: 'Блок питания AEROCOOL KCAS PLUS 500, 500Вт, 120мм, черный, retail [kcas-500 plus]',
    price_by_md: 3410,
    currency_by_md: 'RUB',
    name_by_md: 'Блок питания AEROCOOL KCAS PLUS 500,  500Вт,  черный',
    vendor_by_md: 'AEROCOOL',
    vendorCode_by_md: 'KCAS-500 PLUS',
    pictures_by_md: ['https://items.s1.citilink.ru/1049262_v01_b.jpg'],
    name_by_sl: 'Блок питания AEROCOOL KCAS PLUS 500, 500Вт, 120мм, черный, retail [kcas-500 plus]',
    price_by_sl: 3410,
    category_by_sl: 'Ноутбуки и компьютеры/Комплектующие для ПК/Блоки питания/AEROCOOL',
    currency_by_sl: 'RUB',
    pictures_by_sl: ['https://items.s1.citilink.ru/1049262_p03_b.jpg'],
    url_by_sh: 'https://www.citilink.ru/catalog/computers_and_notebooks/parts/powersupply/1049262/',
    name_by_og:
        // eslint-disable-next-line max-len
        'Купить Блок питания AEROCOOL KCAS PLUS 500,  500Вт,  черный в интернет-магазине СИТИЛИНК, цена на Блок питания AEROCOOL KCAS PLUS 500,  500Вт,  черный (1049262) - Москва',
    self_by_sd: [],
    metrics_by_mt: ['ya'],
    niid:
        // eslint-disable-next-line max-len
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGlkIjoyMjEwMzY0LCJhZmZJZCI6MTAwOCwiYnJvd3NlciI6IkNocm9tZSIsInlhbmRleHVpZCI6IjU4MjkzMzQyMTYwNzAwMDA0MiIsImlhdCI6MTYwNzMzNTg1NH0.P7rU5Bk3lktFeAHmu43yRDsj51uhZbuDRO3cByhRQsc',
};

const bodyNoWww = {
    dds: '87bfbda339972719414c94ee150692b2',
    v: '202012110221',
    isSelectorExists: true,
    transaction_id: 'unknown-kiqhsykaam80r3x9subffnk85scjr6fz',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1400',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1607335854763',
        installId: '31961340-104f-4820-bd9b-7734d384f120',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: '{"width":1073,"height":718}',
    screen_resolution: '{"ratio":2,"width":1440,"height":900}',
    viewport: '{"width":1073,"height":2153}',
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: false,
    url: 'https://topcomputer.ru/tovary/1585606/',
    h1_by_df: 'Корпус для компьютера PowerCase Alisio Mesh M White',
    price_by_md: 3050,
    name_by_md: 'Корпус для компьютера PowerCase Alisio Mesh M White',
    pictures_by_md: [
        '/upload/resize_cache/images/e1/400_300_140cd750bba9870f18aada2478b24840a/e18821b9a624b02c3401ebb61f57dbcb.jpg',
        '/upload/resize_cache/images/3f/400_300_140cd750bba9870f18aada2478b24840a/3f4765bb15f57c7e3f7507cb2d80c23f.jpg',
    ],
    name_by_sl: 'Корпус для компьютера PowerCase Alisio Mesh M White',
    price_by_sl: 3050,
    category_by_sl: 'Каталог/Комплектующие для ПК/Корпуса для компьютеров',
    pictures_by_sl: [
        '/upload/resize_cache/images/e1/400_300_140cd750bba9870f18aada2478b24840a/e18821b9a624b02c3401ebb61f57dbcb.jpg',
        '/upload/resize_cache/images/3f/400_300_140cd750bba9870f18aada2478b24840a/3f4765bb15f57c7e3f7507cb2d80c23f.jpg',
    ],
    url_by_sh: 'https://topcomputer.ru/tovary/1585606/',
    self_by_sd: [],
    metrics_by_mt: ['ya'],
    niid:
        // eslint-disable-next-line max-len
        'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjbGlkIjoyMjEwMzY0LCJhZmZJZCI6MTAwOCwiYnJvd3NlciI6IkNocm9tZSIsInlhbmRleHVpZCI6IjU4MjkzMzQyMTYwNzAwMDA0MiIsImlhdCI6MTYwNzMzNTg1NH0.P7rU5Bk3lktFeAHmu43yRDsj51uhZbuDRO3cByhRQsc',
};

describe('Search pipeline: model-by-url', () => {
    beforeAll(() => {
        getShopsByDomainMock.mockImplementation(() => Promise.resolve(shops));
    });

    afterAll(() => {
        nock.cleanAll();
    });

    test('should find model with www', async () => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            categoriesMatchMock,
            searchEmptyMock,
            searchMock,
            modelMock,
            modelOffersPremiumMock,
            modelOffersMock,
            modelOffersMock,
            modelOffersMock,
            modelOpinionsMock,
            shopInfoMock,
            socialProfileMock,
        );

        const response: any = await request
            .post('/products')
            .set('X-HTTP-Method-Override', 'GET')
            .send(bodyWww);
        const { statusCode, text } = response;
        const responseBody = JSON.parse(text);
        expect(statusCode).toBe(200);
        expect(responseBody).toEqual(responseStubWww);

        mockScopes.forEach((scope) => scope.done());
    });

    test('should find model without www', async () => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            categoriesMatchMock,
            searchEmptyMock,
            searchNoWwwMock,
            modelNoWwwMock,
            modelOffersNoWwwPremiumMock,
            modelOffersNoWwwMock,
            modelOffersNoWwwMock,
            modelOffersNoWwwMock,
            modelOffersNoWwwMockEmpty,
            modelOffersNoWwwMockEmpty,
            socialProfileMock,
            modelOpinionsMock,
        );

        const response: any = await request
            .post('/products')
            .set('X-HTTP-Method-Override', 'GET')
            .send(bodyNoWww);

        const { statusCode, text } = response;
        const responseBody = JSON.parse(text);
        expect(statusCode).toBe(200);
        expect(responseBody).toEqual(responseStubNoWww);

        mockScopes.forEach((scope) => scope.done());
    });
});
