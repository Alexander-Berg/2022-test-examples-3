import {mockLocation} from '@self/root/src/helpers/testament/mock';
import {makeMirrorDesktop} from '@self/root/src/helpers/testament/mirror';

import {
    showDirectDiscountBadge,
    showSnippetMainInfo,
} from './testCases';
import {productId} from './product';

/** @type {Mirror} */
let mirror;
/** @type {JestLayer} */
let jestLayer;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;

async function makeContext() {
    // Кука с ID сессии кадавра. Обязательная часть инициализации, если ваш виджет
    // вызывает какие-нибудь резолверы. Без этой куки хост запроса не будет подменяться
    // и приложение будет пытаться ходить в реальные бекенды.
    // const cookie = {kadavr_session_id: await (kadavrLayer.getSessionId())};

    return mandrelLayer.initContext({});
}

beforeAll(async () => {
    mockLocation();
    mirror = await makeMirrorDesktop({
        jest: {
            testFilename: __filename,
            jestObject: jest,
        },
        kadavr: {
            skipLayer: true,
        },
    });
    jestLayer = mirror.getLayer('jest');
    mandrelLayer = mirror.getLayer('mandrel');
    apiaryLayer = mirror.getLayer('apiary');


    await jestLayer.doMock(
        require.resolve('@self/project/src/resolvers/spvConfigs/resolveVendorsConfig'),
        () => ({
            __esModule: true,
            resolveVendorsConfigUnsafe: () => Promise.resolve({}),
        })
    );

    await jestLayer.doMock(
        require.resolve('@self/platform/resolvers/shops/info'),
        () => ({
            __esModule: true,
            resolveSupplierInfo: () => Promise.resolve({
                entities: {
                    supplierInfo: {},
                },
                result: [],
            }),
        })
    );

    await jestLayer.runCode(() => {
        jest.spyOn(require('@yandex-market/mandrel/resolvers/page'), 'resolvePageIdSync')
            .mockReturnValue('market:product-offers');

        // flowlint-next-line untyped-import: off
        require('@self/project/src/spec/unit/mocks/yandex-market/mandrel/resolver');
        const {unsafeResource} = require('@yandex-market/mandrel/resolver');
        const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
        const offer = require('./offer').default;
        const product = require('./product').default;

        // $FlowFixMe
        unsafeResource.mockImplementation(
            createUnsafeResourceMockImplementation({
                'report.getProductsInfo': () => Promise.resolve(product),
                'report.getProductOffers': () => Promise.resolve(offer),
            })
        );
    }, []);
});

afterAll(() => {
    mirror.destroy();
});

describe('ProductOffersResults.', () => {
    // SKIPPED MARKETFRONT-96354
    // eslint-disable-next-line jest/no-disabled-tests
    describe.skip('DirectDiscount sticker', () => {
        beforeEach(async () => {
            await makeContext();
        });

        test('отображается в списке офферов (marketfront-4342)', () => showDirectDiscountBadge(jestLayer, apiaryLayer, mandrelLayer, {productId}));
    });

    describe('Сниппет товара', () => {
        beforeEach(async () => {
            await makeContext();
        });

        describe('Оффер DSBS в ценах.', () => {
            beforeAll(async () => {
                await jestLayer.runCode(() => {
                    const {unsafeResource} = require('@yandex-market/mandrel/resolver');
                    const {createUnsafeResourceMockImplementation} = require('@self/root/src/spec/unit/mocks/resolvers/helpers');
                    const offerReportData = require('./offer').default;
                    const product = require('./product').default;

                    const [offerFromReportMock] = offerReportData.search.results;

                    const offerDSBS = {
                        ...offerReportData,
                        search: {
                            ...offerReportData.search,
                            results: [
                                {
                                    ...offerFromReportMock,
                                    delivery: {
                                        ...offerFromReportMock.delivery,
                                        deliveryPartnerTypes: ['SHOP'],
                                    },
                                    shop: {
                                        ...offerFromReportMock.shop,
                                        id: 2,
                                    },
                                    cpa: 'real',
                                    offerColor: 'white',
                                },
                            ],
                        },
                    };

                    delete offerDSBS.search.results[0].supplier;

                    // $FlowFixMe
                    unsafeResource.mockImplementation(
                        createUnsafeResourceMockImplementation({
                            'report.getProductsInfo': () => Promise.resolve(product),
                            'report.getProductOffers': () => Promise.resolve(offerDSBS),
                        })
                    );
                }, []);
            });

            test('Сниппет должен содержать основные данные', () => showSnippetMainInfo(jestLayer, apiaryLayer, mandrelLayer, {productId}));
        });
    });
});
