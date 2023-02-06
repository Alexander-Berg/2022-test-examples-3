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
const laasMock = require('../../__mocks__/laas.mock');
const categoriesMatchMock = require('./__mocks__/categories-match.model-by-filter');
const multiMatchMock = require('../__mocks__/multi-match-empty-response');
const multiMatchStringMock = require('../__mocks__/multi-match-string.empty-response');
const redirectMock = require('./__mocks__/redirect.model-by-filter');
const categoriesSearchMock = require('./__mocks__/categories-search.model-by-filter');
const modelMock = require('./__mocks__/model.model-by-filter');
const modelPremiumOffersMock = require('./__mocks__/model-premium-offers.model-by-filter');
const modelOffersMock = require('./__mocks__/model-offers.model-by-filter');
const modelOpinionsMock = require('../__mocks__/model-opinions.mock');
const modelProductOffersMock = require('./__mocks__/product-offers.model-by-filter');
const alsoviewedMock = require('./__mocks__/alsoviewed.model-by-filter');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.model-by-filter');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: 'd73fed3751cd363bff92bc58c902507f',
    v: '201910161211',
    isSelectorExists: true,
    transaction_id: 'k20vcemffp1pse0fj974qohbte60mqgp',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1571665586650',
        installId: '02993609-7aee-4e8a-9d15-a092c3704af0',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: { width: 1025, height: 644 },
    screen_resolution: { ratio: 1, width: 1440, height: 787 },
    viewport: { width: 1008, height: 644 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: true,
    url:
        'https://www.mvideo.ru/products/elektricheskaya-zubnaya-shhetka-braun-oral-b-vitality-d12-514k-frozen-20062551',
    pictures_by_tg: ['https://img.mvideo.ru/Pdb/20062551b.jpg', '//img.mvideo.ru/Pdb/20062551b.jpg'],
    h1_by_df: 'Электрическая зубная щетка Braun Oral-B Vitality D12.514K Frozen',
    price_by_md: 2990,
    name_by_md: 'Электрическая зубная щетка Braun Oral-B Vitality D12.514K Frozen',
    vendor_by_md: 'Braun',
    category_by_md: 'Электрические зубные щетки',
    pictures_by_md: ['https://img.mvideo.ru/Pdb/20062551b.jpg'],
    name_by_sl: 'Электрическая зубная щетка Braun Oral-B Vitality D12.514K Frozen',
    pictures_by_sl: [
        '//img.mvideo.ru/Pdb/20062551b.jpg',
        '//img.mvideo.ru/Pdb/small_pic/65/20062551s.jpg',
        '//img.mvideo.ru/Pdb/small_pic/65/20062551b1.jpg',
        '//img.mvideo.ru/Pdb/small_pic/65/20062551b2.jpg',
    ],
    category_by_sl: 'Товары для здоровья/Зубные щетки/Электрические зубные щетки/Braun',
    price_by_sl: 2990,
    url_by_sh:
        'https://www.mvideo.ru/products/elektricheskaya-zubnaya-shhetka-braun-oral-b-vitality-d12-514k-frozen-20062551',
    name_by_og: 'Электрическая зубная щетка Braun Oral-B Vitality D12.514K Frozen',
    self_by_sd: false,
};

describe('Search pipeline: model-if-by-filter', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            categoriesMatchMock,
            multiMatchMock,
            multiMatchStringMock,
            redirectMock,
            categoriesSearchMock,
            modelMock,
            modelPremiumOffersMock,
            modelOffersMock,
            modelOffersMock,
            modelOpinionsMock,
            modelProductOffersMock,
            modelProductOffersMock,
            alsoviewedMock,
        );

        getShopsByDomainMock.mockImplementation(() => Promise.resolve(shops));
    });

    afterAll(() => {
        nock.cleanAll();
    });

    test('should find model', async () => {
        const response: any = await request
            .post('/products')
            .set('X-HTTP-Method-Override', 'GET')
            .send(body);

        const { statusCode, text } = response;
        const responseBody = JSON.parse(text);
        expect(statusCode).toBe(200);
        expect(responseBody).toEqual(responseStub);

        mockScopes.forEach((scope) => scope.done());
    });
});
