// @flow
// flowlint untyped-import:off
import {getByText, fireEvent, waitFor, screen} from '@testing-library/dom';
import {select} from 'reselector';

import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';
import {makeMirror} from '@self/platform/helpers/testament';
import {
    createProduct,
    mergeState,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import DeliveryInfo from '@self/platform/spec/page-objects/components/DeliveryInfo';
import OnDemandDeliveryInfo from '@self/platform/components/DeliveryInfo/OnDemandDeliveryInfo/__pageObject/index.js';
// import ShopNamePO from '@self/project/src/components/ShopName/__pageObject';
import PricePO from '@self/platform/components/Price/__pageObject';
import OfferPaymentTypes from '@self/project/src/components/OfferPaymentTypes';
import DeliveryPolicyStatus from '@self/root/src/components/DeliveryPolicyStatus/__pageObject';
import HintWithContent from '@self/root/src/components/HintWithContent/__pageObject';

import {
    createDelivery,
    createDeliveryOption,
    createDeliveryPickupOption,
} from '@self/project/src/entities/delivery/__mock__/delivery.mock';
import {createOffer} from '@self/project/src/entities/offer/__mock__/offer.mock';
import {mockIntersectionObserver} from '@self/root/src/helpers/testament/mock';
import {buildUrl} from '@self/root/src/utils/router';

// fixtures
import productWithExternalLinkInDO from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/fixtures/productWithExternalLinkInDO';
import productWithDirectDiscountInDO from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/fixtures/productWithDirectDiscountInDO';
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';
import {
    DELIVERY_SERVICE_ID,
    OFFER,
    OFFER_ID,
    PRODUCT,
    PRODUCT_ID,
} from './mocks';
// flowlint untyped-import:error
import {nPlusMPromo, promocodePromo, giftPromo, spreadDiscountCountPromo} from './promo.mock';

/** @type {Mirror} */
let mirror;
/** @type {MandrelLayer} */
let mandrelLayer;
/** @type {KadavrLayer} */
let kadavrLayer;
/** @type {ApiaryLayer} */
let apiaryLayer;
/** @type {JestLayer} */
let jestLayer;

type TestContext = {
    exps?: { [string]: boolean },
    // $FlowFixMe
    user?: { [string]: any },
};

async function makeContext({exps = {}, user = {}}: TestContext = {}) {
    const UID = '9876543210';
    const yandexuid = '1234567890';

    const cookie = {kadavr_session_id: await kadavrLayer.getSessionId()};

    return mandrelLayer.initContext({
        user: {
            UID,
            yandexuid,
            ...user,
        },
        request: {
            cookie,
            abt: {
                expFlags: exps || {},
            },
        },
    });
}

describe('Widget: DefaultOffer', () => {
    const widgetPath = require.resolve('@self/platform/widgets/content/DefaultOffer');
    const widgetOptions = {pp: 'OFFER_CARD', showMoreOffersLink: false, offerId: OFFER_ID, showTitle: true};

    const setReportState = async offerData => {
        // $FlowFixMe не видит вложенные сущности
        const offer = createOffer(offerData);

        const reportState = mergeState([
            createProduct(PRODUCT, PRODUCT_ID),
            createOfferForProduct(offer, PRODUCT_ID, OFFER_ID),
            {
                data: {
                    search: {
                        total: 1,
                    },
                },
            },
        ]);
        await kadavrLayer.setState('report', reportState);
    };

    beforeAll(async () => {
        mockIntersectionObserver();
        mirror = await makeMirror({
            jest: {
                testFilename: __filename,
                jestObject: jest,
            },
            kadavr: {
                asLibrary: true,
            },
        });
        mandrelLayer = mirror.getLayer('mandrel');
        kadavrLayer = mirror.getLayer('kadavr');
        apiaryLayer = mirror.getLayer('apiary');
        jestLayer = mirror.getLayer('jest');

        // $FlowFixMe
        jest.useFakeTimers('modern');
        // $FlowFixMe
        jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
        await jestLayer.doMock(
            require.resolve('@self/platform/widgets/content/FinancialProduct'),
            () => ({create: () => Promise.resolve(null)})
        );
    });

    afterAll(() => {
        mirror.destroy();
        jest.useRealTimers();
    });

    describe('Бейдж в ДО', () => {
        // SKIPPED MARKETFRONT-96354
        // eslint-disable-next-line jest/no-disabled-tests
        test.skip.each([
            [
                'N+M',
                'При покупке 9 ещё 1 в подарок(450 ₽/шт.)',
                nPlusMPromo,
            ],
            [
                'промокод',
                '−400 ₽ по промокоду tch7i22000255',
                promocodePromo,
            ],
        ])('на акции %s содержит: "%s"', async (name, expected, params) => {
            await makeContext();
            await setReportState({...OFFER, ...params});
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const text = container.textContent;
            expect(text).toContain(expected);
        });

        test.each([
            [
                'подарок',
                'Подарок за покупку',
                giftPromo,
            ],
            [
                'за количество',
                'от 2 шт. – по 8999 ₽/шт.от 5 шт. – по 7999 ₽/шт.',
                spreadDiscountCountPromo,
            ],
        ])('на акции %s содержит: "%s"', async (name, expected, params) => {
            await makeContext();
            await setReportState({...OFFER, ...params});
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            const text = container.textContent;
            expect(text).toContain(expected);
        });
    });

    describe('DeliveryInfo component', () => {
        const regionUser = {
            region: {
                id: 32323,
            },
        };

        test('Рендеринг ДО', async () => {
            await makeContext();
            await setReportState(OFFER);
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

            await step('ДО должен отрендериться', async () => {
                expect(container.querySelector(`[data-offer-id="${OFFER_ID}"]`)).not.toBeNull();
            });
        });

        describe('PickupInfo', () => {
            const compareText = async (container, expectedText) => {
                await step('Ищем компонент PickupInfo и сравниваем текст', async () => {
                    const text = container.querySelector(DeliveryInfo.pickupInfo).textContent;
                    expect(text).toEqual(expectedText);
                });
            };

            describe('Город  миллионник', () => {
                test('Самовывоз завтра + дата, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 1, dayTo: 1, groupCount: 1}
                        )),
                        createDeliveryPickupOption({dayTo: 6}),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 0}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз завтра, 22 мая — 30 ₽');
                });

                test('Самовывоз сегодня, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(1).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 0}
                        )),
                        createDeliveryPickupOption({dayTo: 6}),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 0}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — 30 ₽');
                });

                test('Самовывоз сегодня, бесплатно', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 1, price: {value: '0', currency: 'RUR'}}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — бесплатно');
                });

                test('Самовывоз сегодня, без цены', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(1).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 1, price: {value: '100000', currency: 'RUR'}}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 1}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня');
                });

                test('Самовывоз в определенную дату, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 3, dayTo: 3, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз в понедельник, 24 мая — 30 ₽');
                });

                test('Самовывоз с датой формата N-M месяц, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 3, dayTo: 5, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз, 24-26 мая — 30 ₽');
                });

                test('Самовывоз с датой формата N месяц-M месяц, с ценой', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 1, dayTo: 6, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз, 30 мая-4 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });

                test('Самовывоз сегодня из N пунктов, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(1).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 15, price: {value: '30', currency: 'RUR'}, serviceId: DELIVERY_SERVICE_ID}
                        )),
                        createDeliveryPickupOption({dayTo: 6}),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock,
                        hasPickup: true,
                        availableServices: [
                            {
                                serviceId: DELIVERY_SERVICE_ID,
                                serviceName: 'Собственная служба',
                            },
                        ]});
                    await setReportState({...OFFER, delivery: deliveryMock, outletsCount: 50});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — 30 ₽');
                });

                test('Самовывоз, день недели, дата из N пунктов, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(15).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 4, dayTo: 4, groupCount: 1, price: {value: '30', currency: 'RUR'}}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, outletsCount: 20});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз во вторник, 25 мая — 30 ₽');
                });

                test('Самовывоз до даты из N пунктов, с ценой', async () => {
                    await makeContext();

                    const pickupOptionsMock = [
                        ...new Array(15).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: undefined, dayTo: 4, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз до 25 мая — 30 ₽');
                });
            });

            describe('Региональный город', () => {
                test('Самовывоз завтра + дата, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 1, dayTo: 1, groupCount: 1}
                        )),
                        createDeliveryPickupOption({dayTo: 6}),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 0}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз завтра, 22 мая — 30 ₽');
                });

                test('Самовывоз сегодня, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(1).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 0}
                        )),
                        createDeliveryPickupOption({dayTo: 6}),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 0}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — 30 ₽');
                });

                test('Самовывоз сегодня, бесплатно', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 1, price: {value: '0', currency: 'RUR'}}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — бесплатно');
                });

                test('Самовывоз сегодня, без цены', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(1).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 1, price: {value: '100000', currency: 'RUR'}}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 1}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — 100 000 ₽');
                });

                test('Самовывоз в определенную дату, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 3, dayTo: 3, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз в понедельник, 24 мая — 30 ₽');
                });

                test('Самовывоз с датой формата N-M месяц, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 3, dayTo: 5, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз, 24-26 мая — 30 ₽');
                });

                test('Самовывоз с датой формата N месяц-M месяц, с ценой', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(5).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 1, dayTo: 6, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз, 30 мая-4 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });

                test('Самовывоз сегодня из N пунктов, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(1).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 0, dayTo: 0, groupCount: 15, price: {value: '30', currency: 'RUR'}, serviceId: DELIVERY_SERVICE_ID}
                        )),
                        createDeliveryPickupOption({dayTo: 6}),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock,
                        hasPickup: true,
                        availableServices: [
                            {
                                serviceId: DELIVERY_SERVICE_ID,
                                serviceName: 'Собственная служба',
                            },
                        ]});
                    await setReportState({...OFFER, delivery: deliveryMock, outletsCount: 50});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз сегодня — 30 ₽');
                });

                test('Самовывоз, день недели, дата из N пунктов, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(15).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: 4, dayTo: 4, groupCount: 1, price: {value: '30', currency: 'RUR'}}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, outletsCount: 20});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз во вторник, 25 мая — 30 ₽');
                });

                test('Самовывоз до даты из N пунктов, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const pickupOptionsMock = [
                        ...new Array(15).fill(null).map(() => createDeliveryPickupOption(
                            {dayFrom: undefined, dayTo: 4, groupCount: 1}
                        )),
                    ];
                    const deliveryMock = createDelivery({pickupOptions: pickupOptionsMock, hasPickup: true});
                    await setReportState({...OFFER, delivery: deliveryMock, shop: {...OFFER.shop, outletsCount: 15}});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Самовывоз до 25 мая — 30 ₽');
                });
            });
        });

        describe('OnDemandDeliveryInfo', () => {
            describe('Нет доставки по клику', () => {
                test('OnDemandDeliveryInfo не отображается', async () => {
                    await makeContext();

                    const deliveryMock = createDelivery({});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    const onDemandInfo = container.querySelector(OnDemandDeliveryInfo.root);

                    expect(onDemandInfo).toBeNull();
                });

                test('DeliveryDescription отображается', async () => {
                    await makeContext();

                    const deliveryMock = createDelivery({});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    const deliveryDescription = container.querySelector(DeliveryInfo.deliveryDescription);

                    expect(deliveryDescription).not.toBeNull();
                });
            });

            // describe('Есть доставка по клику', () => {
            //     test('OnDemandDeliveryInfo отображается', async () => {
            //         await makeContext();
            //
            //         const deliveryMock = createDelivery({
            //             onDemandStats: {
            //                 dayFrom: 1,
            //                 dayTo: 1,
            //                 price: {
            //                     value: '0',
            //                     currency: 'RUB',
            //                 },
            //             },
            //         });
            //
            //         await setReportState({...OFFER, delivery: deliveryMock});
            //
            //         const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            //
            //         const onDemandInfo = container.querySelector(OnDemandDeliveryInfo.root);
            //
            //         expect(onDemandInfo).not.toBeNull();
            //     });
            //
            //     test('DeliveryDescription не отображается', async () => {
            //         await makeContext();
            //
            //         const deliveryMock = createDelivery({
            //             onDemandStats: {
            //                 dayFrom: 1,
            //                 dayTo: 1,
            //                 price: {
            //                     value: '0',
            //                     currency: 'RUB',
            //                 },
            //             },
            //         });
            //
            //         await setReportState({...OFFER, delivery: deliveryMock});
            //
            //         const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            //
            //         const deliveryDescription = container.querySelector(DeliveryInfo.deliveryDescription);
            //
            //         expect(deliveryDescription).toBeNull();
            //     });
            // });
        });

        describe('DeliveryDescription', () => {
            const compareText = async (container, expectedText) => {
                await step('Ищем компонент DeliveryDescription и сравниваем текст', async () => {
                    const text = container.querySelector(DeliveryInfo.deliveryDescription).textContent;
                    expect(text).toEqual(expectedText);
                });
            };

            describe('Город миллионник', () => {
                test('Получение онлайн', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0});
                    const deliveryMock = createDelivery({isDownloadable: true, options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Получение онлайн');
                });

                test('Получение по почте', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0});
                    const deliveryMock = createDelivery({isDownloadable: true, options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock, cpa: 'real'});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Получение по электронной почте');
                });

                test('Доставка без дефолтной опции, есть доставка', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0, isDefault: false});
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером — есть доставка');
                });

                test('Доставка сегодня, с ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0, isDefault: true});
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня — 30 ₽');
                });

                test('Доставка сегодня, с примерной ценой ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 0, isDefault: true, partnerType: 'market_delivery_white',
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня — ≈ 30 ₽');
                });

                test('Доставка сегодня, бесплатно', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 0, isDefault: true, price: {value: '0', currency: 'RUR'},
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня — бесплатно');
                });

                test('Доставка завтра, дата, бесплатно', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 1, dayTo: 1, isDefault: true, price: {value: '0', currency: 'RUR'},
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером завтра, 22 мая — бесплатно');
                });

                test('Доставка сегодня-завтра, с ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 1, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня‑завтра — 30 ₽');
                });

                test('Доставка до даты, с ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 4, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером до 25 мая — 30 ₽');
                });

                test('Доставка, интервал дат, с ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 2, dayTo: 6, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером, 23-27 мая — 30 ₽');
                });

                test('Доставка, день недели, дата, с ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 3, dayTo: 3, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером в понедельник, 24 мая — 30 ₽');
                });

                test('Доставка, до 60 дней, с ценой', async () => {
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 5, dayTo: 3, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером, до 60 дней — 30 ₽');
                });


                test('Доставка, интервал дат разные месяца, с ценой', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 1, dayTo: 6, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером, 30 мая-4 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });

                test('Доставка с долгим сроком доставки (marketfront-5952)', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 10, isDefault: true, isEstimated: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером с 8 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });

                test('Доставка с признаком товара под заказ (marketfront-5837)', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 10, isDefault: true, isEstimated: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({
                        ...OFFER,
                        delivery: deliveryMock,
                        isUniqueOffer: true,
                        orderReturnPolicy: {
                            type: 'forbidden',
                            reason: 'unique-order',
                            description: 'Some description',
                        },
                        orderCancelPolicy: {
                            type: 'time-limit',
                            reason: 'unique-order',
                            daysForCancel: 10,
                            description: 'Some description',
                        },
                    });

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером с 8 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });
            });

            describe('Региональный город', () => {
                test('Получение онлайн', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0});
                    const deliveryMock = createDelivery({isDownloadable: true, options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Получение онлайн');
                });

                test('Получение по почте', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0});
                    const deliveryMock = createDelivery({isDownloadable: true, options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock, cpa: 'real'});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Получение по электронной почте');
                });

                test('Доставка без дефолтной опции, есть доставка', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0, isDefault: false});
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером — есть доставка');
                });

                test('Доставка сегодня, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({dayFrom: 0, dayTo: 0, isDefault: true});
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня — 30 ₽');
                });

                test('Доставка сегодня, с примерной ценой ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 0, isDefault: true, partnerType: 'market_delivery_white',
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня — ≈ 30 ₽');
                });

                test('Доставка сегодня, бесплатно', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 0, isDefault: true, price: {value: '0', currency: 'RUR'},
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня — бесплатно');
                });

                test('Доставка завтра, дата, бесплатно', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 1, dayTo: 1, isDefault: true, price: {value: '0', currency: 'RUR'},
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером завтра, 22 мая — бесплатно');
                });

                test('Доставка сегодня-завтра, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 1, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером сегодня‑завтра — 30 ₽');
                });

                test('Доставка до даты, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 0, dayTo: 4, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером до 25 мая — 30 ₽');
                });

                test('Доставка, интервал дат, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 2, dayTo: 6, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером, 23-27 мая — 30 ₽');
                });

                test('Доставка, день недели, дата, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 3, dayTo: 3, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером в понедельник, 24 мая — 30 ₽');
                });

                test('Доставка, до 60 дней, с ценой', async () => {
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 5, dayTo: 3, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером, до 60 дней — 30 ₽');
                });


                test('Доставка, интервал дат разные месяца, с ценой', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext({user: regionUser});

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 1, dayTo: 6, isDefault: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером, 30 мая-4 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });

                test('Доставка с долгим сроком доставки (marketfront-5952)', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 10, isDefault: true, isEstimated: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({...OFFER, delivery: deliveryMock});

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером с 8 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });

                test('Доставка с признаком товара под заказ (marketfront-5837)', async () => {
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-29T12:00:00.223z'));
                    await makeContext();

                    const deliveryOption = createDeliveryOption({
                        dayFrom: 10, isDefault: true, isEstimated: true,
                    });
                    const deliveryMock = createDelivery({options: [deliveryOption]});
                    await setReportState({
                        ...OFFER,
                        delivery: deliveryMock,
                        isUniqueOffer: true,
                        orderReturnPolicy: {
                            type: 'forbidden',
                            reason: 'unique-order',
                            description: 'Some description',
                        },
                        orderCancelPolicy: {
                            type: 'time-limit',
                            reason: 'unique-order',
                            daysForCancel: 10,
                            description: 'Some description',
                        },
                    });

                    const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);

                    await compareText(container, 'Курьером с 8 июня — 30 ₽');
                    // $FlowFixMe
                    jest.setSystemTime(new Date('2021-05-21T12:00:00.223z'));
                });
            });
        });
    });

    describe('Цена', () => {
        test('Содержит корректное значение из оффера', async () => {
            await kadavrLayer.setState('report', productWithExternalLinkInDO.state);
            await makeContext();

            const {container} = await apiaryLayer.mountWidget(
                widgetPath,
                {
                    ...widgetOptions,
                    offerId: productWithExternalLinkInDO.offerId,
                }
            );

            const price = container.querySelector(PricePO.price);

            expect(price).toBeTruthy();
            expect(price.textContent).toBe(`37 490${NBSP}₽/шт`);
        });
        test('Отображает бейдж Direct Discount из оффера (marketfront-4345)', async () => {
            await kadavrLayer.setState('report', productWithDirectDiscountInDO.state);
            await makeContext();

            const {container} = await apiaryLayer.mountWidget(
                widgetPath,
                {
                    ...widgetOptions,
                    offerId: productWithDirectDiscountInDO.offerId,
                }
            );

            const price = container.querySelector(PricePO.price);
            const discountBadge = container.querySelector(PricePO.discountBadge);
            const discountBadgeText = container.querySelector(PricePO.discountBadgeText);
            const discountPrice = container.querySelector(PricePO.discountPrice);

            expect(price).toBeTruthy();
            expect(price.textContent).toBe(`103${NBSP}₽/шт`);
            expect(discountBadge).toBeTruthy();
            expect(discountBadge.textContent).toBe('Скидка:‒24%');
            expect(discountBadgeText.textContent).toBe('‒24%');
            expect(discountPrice.textContent).toBe(`136${NBSP}₽`);
        });
    });

    describe('Кнопка "В магазин"', () => {
        test('Содержит ссылку на сайт магазина', async () => {
            await kadavrLayer.setState('report', productWithExternalLinkInDO.state);
            await makeContext();

            const {container} = await apiaryLayer.mountWidget(
                widgetPath,
                {
                    ...widgetOptions,
                    offerId: productWithExternalLinkInDO.offerId,
                }
            );

            const clickoutLink = getByText(container, 'В магазин');

            expect(clickoutLink).toBeTruthy();
            expect(clickoutLink.getAttribute('target')).toBe('_blank');
            expect(clickoutLink.getAttribute('href')).toBe(
                buildUrl('external:clickdaemon', {url: 'http://example.com', tld: 'ru'})
            );
        });
    });

    describe('Кнопка "В корзину"', () => {
        test('DSBS оффер. Кнопка "В корзину" отображается ', async () => {
            await makeContext();
            await setReportState({...offerDSBSMock});
            await apiaryLayer.mountWidget(widgetPath, widgetOptions);

            expect(screen.getByRole('button', {name: 'Добавить в корзину'})).toBeInTheDocument();
        });
    });

    /**
     * @expFlag dsk_km-do_trust-rev
     * @ticket MARKETFRONT-71593
     * @start
     */
    // ShopNamePO сейчас не используется (дефолтно). Оставляем, тк может понадобиться, после унификации иконок-лого
    // describe('Ссылка на магазин оффера', () => {
    //     test('Содержит ссылку на сайт магазина', async () => {
    //         await kadavrLayer.setState('report', productWithExternalLinkInDO.state);
    //         await makeContext();
    //
    //         const {container} = await apiaryLayer.mountWidget(
    //             widgetPath,
    //             {
    //                 ...widgetOptions,
    //                 offerId: productWithExternalLinkInDO.offerId,
    //             }
    //         );
    //
    //         const shopNameLink = container.querySelector(`${ShopNamePO.link} a`);
    //
    //         expect(shopNameLink).toBeTruthy();
    //         expect(shopNameLink.getAttribute('target')).toBe('_blank');
    //         expect(shopNameLink.getAttribute('href')).toBe(
    //             buildUrl('external:clickdaemon', {url: 'http://example.com', tld: 'ru'})
    //         );
    //     });
    // });
    /**
     * @expFlag dsk_km-do_trust-rev
     * @end
     */

    describe('Блок способов оплаты', () => {
        test('Отражает все доступные методы оплаты', async () => {
            await kadavrLayer.setState('report', productWithExternalLinkInDO.state);
            await makeContext();

            const {container} = await apiaryLayer.mountWidget(
                widgetPath,
                {
                    ...widgetOptions,
                    offerId: productWithExternalLinkInDO.offerId,
                }
            );

            const paymentTypes = container.querySelector(select`${OfferPaymentTypes}`);

            expect(paymentTypes).toBeTruthy();
            expect(paymentTypes.textContent).toBe('Картой онлайн/курьеру, наличными');
        });
    });

    describe('Блок с сообщением о возврате', () => {
        async function createState(isDigital: boolean, isUnique: boolean) {
            await setReportState({
                ...OFFER,
                delivery: createDelivery({
                    isDownloadable: isDigital,
                    options: [
                        createDeliveryOption({
                            dayFrom: 10,
                            isDefault: true,
                        }),
                    ],
                }),
                isUniqueOffer: isUnique,
                orderReturnPolicy: {
                    type: 'forbidden',
                    reason: 'unique-order',
                    description: 'Some description',
                },
                orderCancelPolicy: {
                    type: 'time-limit',
                    reason: 'unique-order',
                    daysForCancel: 10,
                    description: 'Some description',
                },
            });
        }

        beforeEach(async () => {
            await makeContext();
        });

        test('В общем случае не показывается (не уникальный, не цифровой)', async () => {
            await createState(false, false);
            const {container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
            expect(container.querySelector(DeliveryPolicyStatus.root)).toBeNull();
        });

        describe('Если цифровой товар (marketfront-5959)', () => {
            let container;

            beforeEach(async () => {
                await createState(true, false);
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                container = _container;
            });

            test('то показывается', async () => {
                expect(container.querySelector(DeliveryPolicyStatus.root)).not.toBeNull();
            });

            test('при наведении на вопросик показывает хинт с текстом', async () => {
                fireEvent.mouseOver(
                    container.querySelector(
                        DeliveryPolicyStatus.root
                        + ' '
                        + DeliveryPolicyStatus.questionIcon
                    )
                );

                await waitFor(() =>
                    expect(window.document.querySelector(HintWithContent.content)).not.toBeNull()
                );

                expect(window.document.querySelector(HintWithContent.content).textContent).toContain(
                    'Вернуть его нельзя, как и другие онлайн-подписки, карты оплаты и коды активации. Отменить заказ тоже не получится.'
                );
            });
        });

        describe('Если запрещено политикой возвратов (marketfront-5837)', () => {
            let container;

            beforeEach(async () => {
                await createState(false, true);
                const {container: _container} = await apiaryLayer.mountWidget(widgetPath, widgetOptions);
                container = _container;
            });

            test('то показывается', async () => {
                expect(container.querySelector(DeliveryPolicyStatus.root)).not.toBeNull();
            });

            test('при наведении на вопросик показывает хинт с текстом', async () => {
                fireEvent.mouseOver(
                    container.querySelector(
                        DeliveryPolicyStatus.root
                        + ' '
                        + DeliveryPolicyStatus.questionIcon
                    )
                );

                await waitFor(() =>
                    expect(window.document.querySelector(HintWithContent.content)).not.toBeNull()
                );

                expect(window.document.querySelector(HintWithContent.content).textContent).toContain(
                    'Вернуть такой товар не получится, если он надлежащего качества. А отменить заказ можно в течение 10 дней после оформления.'
                );
            });
        });
    });
});
