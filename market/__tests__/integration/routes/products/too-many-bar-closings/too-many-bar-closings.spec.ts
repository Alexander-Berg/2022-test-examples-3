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
const categoriesMatchMock: Mock = require('./__mocks__/categories-match.too-many');
const multiMatchMock: Mock = require('../__mocks__/multi-match-empty-response');
const multiMatchStringMock: Mock = require('../__mocks__/multi-match-string.empty-response');
const primeTextMock: Mock = require('./__mocks__/prime.too-many');
const primeCategoryText: Mock = require('./__mocks__/prime-category-text.too-many');
const shopInfoMock: Mock = require('./__mocks__/shop-info.too-many');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.too-many');


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

const bodyClient = {
    transaction_id: 'k41vl38rxc4js4y5k0brnxuzxgfd4tb5',
    interaction: 'pricebar_close',
    interaction_details: {
        time: 441066,
        popup_shown: 1,
        popup_closed: 0,
        popup_interaction: 0,
    },
    type_view: 'desktop',
    show_type: 'model',
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1575985777888',
        installId: '383f4593-1627-429a-83c8-07ae25c0b411',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    v: '201912041225',
    url: 'https://www.avito.ru/sankt-peterburg/bytovaya_tehnika/hlebopech_panasonic_sd-2501_1786340164',
};

describe('Rule: too-many-bar-closings', () => {
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

    test('should get too-many-bar-closings rule', async () => {
        let cookieClosePricebar = '';
        for (let i = 0; i < 3; i++) {
            const responseClose: any = await request
                .post('/client')
                .set('Cookie', cookieClosePricebar)
                .send(bodyClient);

            // @ts-ignore
            [cookieClosePricebar] = /svt-user=[a-z0-9]+/.exec(responseClose.headers['set-cookie'][0]);
        }

        const responseProduct: any = await request
            .post('/products')
            .set('X-HTTP-Method-Override', 'GET')
            .set('Cookie', cookieClosePricebar)
            .send(body);

        const { statusCode, text } = responseProduct;
        const responseProductBody = JSON.parse(text);
        expect(statusCode).toBe(200);
        expect(responseProductBody).toEqual(responseStub);

        mockScopes.forEach((scope) => scope.done());
    });
});
