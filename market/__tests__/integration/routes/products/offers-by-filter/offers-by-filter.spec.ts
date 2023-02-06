import supertest from 'supertest';
import nock from 'nock';

import app from '../../../../../src/app';
import raiseMocks from '../../../helpers/raise-mocks';
import Mock from '../../../../types/mock';
import getShopsByDomain from '../../../../../src/helper/get-shops-by-domain';

jest.mock('../../../../../src/helper/get-shops-by-domain');
const getShopsByDomainMock = getShopsByDomain as jest.MockedFunction<typeof getShopsByDomain>;

/**
 * @see {@link https://jestjs.io/docs/en/manual-mocks#mocking-user-modules}
 */
if (!process.env.LOG) {
    jest.mock('../../../../../utils/logs');
}

/* Mocks */
const yabroFilterMock: Mock = require('../../__mocks__/yabro-filter');
const laasMock: Mock = require('../../__mocks__/laas.mock');
const categoriesMatchMock: Mock = require('./__mocks__/categories-match.offers-by-filter');
const multiMatchMock: Mock = require('../__mocks__/multi-match-empty-response');
const multiMatchStringMock: Mock = require('../__mocks__/multi-match-string.empty-response');
const primeTextMock: Mock = require('./__mocks__/prime.offers-by-filter');
const primeCategoryText: Mock = require('./__mocks__/prime-category-text.offers-by-filter');
const shopInfoMock: Mock = require('./__mocks__/shop-info.offers.by-filter');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.offers-by-filter');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: 'bb08d245e93c4a23bfd076c0f364395f',
    v: '202103241221',
    transaction_id: 'kmv259s8gs2dm4aoivp7wqtxcgg8bb80',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1614560234162',
        installId: '170bbd24-b47b-4a21-a182-12456cd36f70',
        notificationStatus: 'yandex',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: '{"width":1440,"height":732}',
    screen_resolution: '{"ratio":2,"width":1440,"height":900}',
    viewport: '{"width":1440,"height":3737}',
    notifications: 'yandex',
    ui_language: 'ru',
    is_debug_mode: true,
    url: 'https://otzovik.com/reviews/otdelochniy_material_gibkiy_kamen/',
    h1_by_df: 'Отделочный материал "Гибкий камень" - отзывы',
    name_by_md: 'Отделочный материал "Гибкий камень"',
    vendor_by_md: 'Гибкий камень',
    pictures_by_md: [
        '//i.otzovik.com/objects/b/900000/895608.png',
        '//i6.otzovik.com/2017/12/avatar/56276315.jpeg?48c6',
        '//i.otzovik.com/2015/12/avatar/668100.jpg',
        '/static/img/2018/icons/default_photo.svg',
        '/static/img/2018/icons/default_photo.svg',
        '/static/img/2018/icons/default_photo.svg',
        '//i7.otzovik.com/2019/11/avatar/86534194.jpeg?63d',
    ],
    name_by_sl: 'Отделочный материал "Гибкий камень"',
    category_by_sl: 'Разное/Гибкий камень',
    pictures_by_sl: ['//i.otzovik.com/objects/b/900000/895608.png'],
    url_by_sh: 'https://otzovik.com/reviews/otdelochniy_material_gibkiy_kamen/',
    self_by_sd: [],
    metrics_by_mt: ['ya', 'yan_good'],
    niid: 'GkUNaWjDoYca8nzmKojqlYYiqJHdLrROuSPskj6noXo',
};

describe('Search pipeline: offers-by-filter', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            primeTextMock,
            primeCategoryText,
            shopInfoMock,
            categoriesMatchMock,
            multiMatchMock,
            multiMatchStringMock,
        );

        getShopsByDomainMock.mockImplementation(() => Promise.resolve(shops));
    });

    afterAll(() => {
        nock.cleanAll();
    });

    test('should find offers', async () => {
        const response: any = await request
            .post('/products')
            .set('X-HTTP-Method-Override', 'GET')
            .send(body);

        const { statusCode, text } = response as { statusCode: number; text: string };
        const responseBody = JSON.parse(text);
        expect(statusCode).toBe(200);
        expect(responseBody).toEqual(responseStub);

        mockScopes.forEach((scope) => scope.done());
    });
});
