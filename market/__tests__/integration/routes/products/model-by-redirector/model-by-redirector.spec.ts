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
const categoriesMatchMock = require('./__mocks__/categories-match.model-by-redirector');
const multiMatchMock = require('../__mocks__/multi-match-empty-response');
const multiMatchStringMock = require('../__mocks__/multi-match-string.empty-response');
const modelMock = require('./__mocks__/model.model-by-redirector');
const redirectorMock = require('./__mocks__/redirect.model-by-redirector');
const modelOfferstMock = require('./__mocks__/model-offers.model-by-redirector');
const modelOpinionsMock = require('../__mocks__/model-opinions.mock');

/* For Mock Implementations */
const { shops } = require('./__mocks__/shops.model-by-redirector');

/* Stubs */
const responseStub = require('./__stubs__/response');

const request = supertest(app);

let mockScopes = raiseMocks();

const body = {
    dds: '17aeb246a30822e74605fbfc23dddc88',
    v: '201910161211',
    isSelectorExists: true,
    transaction_id: 'k1zbslveglu6c3k7k455rosgi0m9zt5r',
    is_shop: true,
    settings: {
        applicationName: 'Яндекс.Советник',
        affId: '1008',
        clid: 2210590,
        sovetnikExtension: true,
        withButton: true,
        extensionStorage: true,
        installTime: '1571409249690',
        installId: '8cdde70d-6e64-4252-9343-4e5108cc9918',
        notificationStatus: 'chrome',
        notificationPermissionGranted: true,
    },
    adult: true,
    screen_size: { width: 1440, height: 644 },
    screen_resolution: { ratio: 1, width: 1440, height: 787 },
    viewport: { width: 1423, height: 2615 },
    notifications: 'chrome',
    ui_language: 'ru',
    is_debug_mode: false,
    url: 'https://www.dns-shop.ru/product/7aec9755895d3330/2-tb-zestkij-disk-seagate-barracuda-st2000dm008/',
    pictures_by_tg: [
        'https://c.dns-shop.ru/thumb/st1/fit/120/120/45a971b4eb03bf8c14aa0ed3a62586e3/388d249f2647a68a2a5281c68ff6df754843fcf3c433ba22ef257b228b715574.jpg',
    ],
    h1_by_df: '2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008]',
    price_by_df: 41992,
    price_by_md: 4199,
    name_by_md: '2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008]',
    name_by_sl: '2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008]',
    pictures_by_sl: [
        'https://c.dns-shop.ru/thumb/st1/fit/320/250/1849bb52eda04c92950a62e63a3594cb/388d249f2647a68a2a5281c68ff6df754843fcf3c433ba22ef257b228b715574.jpg',
        'https://c.dns-shop.ru/thumb/st1/fit/320/250/9e3bc70f475fb72f042672f5cede5e66/c6fa47a94cee600b53c058a0796acb9dbb47cf58b2ed2e435c086fb1a33809e3.jpg',
        'https://c.dns-shop.ru/thumb/st1/fit/320/250/d8bbea22b684d2fa7e74bedc5ab749dd/6cfadfa870dae6f3dd7616b3c202a350af48324b5f87e22aa5418382d8c8afe1.jpg',
        'https://c.dns-shop.ru/thumb/st1/fit/320/250/d465938e08419acd88fc8c9aec1a7291/b841b6ce35fd4ae6fb71f81b7e0369c3b248357fa6056392b28762ba830a691b.jpg',
        'https://c.dns-shop.ru/thumb/st1/fit/320/250/153f0484edd3aa88b02ca8231a47636e/365258a55de9dd837e985b1e424d69758b1a65d532d53d9dbcb1efb859185abb.jpg',
        'https://c.dns-shop.ru/thumb/st4/fit/320/250/03294bba9f3a22eb2595c9e48770be2b/f53889b3441efe3551e145d99fdc4e1b15cfcdfb8dfe5ec863b2bb643f628d5a.jpg',
    ],
    category_by_sl:
        'Каталог/Комплектующие, компьютеры и ноутбуки/Комплектующие для ПК/Жесткие диски/Жесткие диски 3.5"',
    price_by_sl: 4199,
    url_by_sh: 'https://www.dns-shop.ru/product/7aec9755895d3330/2-tb-zestkij-disk-seagate-barracuda-st2000dm008/',
    name_by_og:
        'Купить 2 ТБ Жесткий диск Seagate BarraCuda [ST2000DM008] в интернет магазине DNS. Характеристики, цена Seagate BarraCuda | 1270975',
    self_by_sd: false,
};

describe('Search pipeline: model-by-redirector', () => {
    beforeAll(() => {
        mockScopes = raiseMocks(
            laasMock,
            yabroFilterMock,
            categoriesMatchMock,
            multiMatchMock,
            multiMatchStringMock,
            redirectorMock,
            modelMock,
            { ...modelOfferstMock, times: 3 },
            modelOpinionsMock,
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
