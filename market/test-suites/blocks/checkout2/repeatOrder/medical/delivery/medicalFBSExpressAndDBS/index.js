import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

// Mocks
import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';

// Scenarios
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';

// Constants
import ADDRESSES from '@self/root/src/spec/hermione/test-suites/blocks/checkout/constants/addresses';
import CONTACTS from '@self/root/src/spec/hermione/test-suites/blocks/checkout/constants/contacts';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

// Utils
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {PAYMENT_METHOD, PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import firstStep from './firstStep';
import secondStep from './secondStep';

const medicalFBSCart = buildCheckouterBucket({
    cartIndex: 0,
    items: [{
        skuMock: pharma.skuMock,
        offerMock: pharma.offerMock,
        count: 1,
    }],
    shopId: pharma.offerMock.shopId,
    isMedicalParcel: true,
    deliveryOptions: [
        {
            ...deliveryPickupMock,
            paymentOptions: [
                paymentOptions.yandex,
            ],
            outlets: [
                {id: x5outletMock.id, regionId: 0},
                {id: pharma.outletMock.id, regionId: 0},
                {id: withTrying.id, regionId: 0},
            ],
        },
        deliveryDeliveryMock,
    ],
    outlets: [
        x5outletMock,
        pharma.outletMock,
        withTrying,
    ],
});

const offers = [
    {
        entity: 'offer',
        marketSku: '265149848',
        wareId: 'zq3kcdGwrZdWHzFxtRMJWA',
        stockStoreCount: 1,
        count: 1,
        price: {currency: 'RUR', value: '234'},
    },
];

const shopsWithOffers = [
    {
        entity: 'shop',
        id: 10268608,
        name: 'Яндекс.Маркет',
        hasBooking: false,
        offers,
        offersTotalPrice: {currency: 'RUR', value: '234'},
    },
];

const carts = [
    medicalFBSCart,
    buildCheckouterBucket({
        cartIndex: 1,
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

function makeDeliveredOrder() {
    let orderIndex = 0;
    return async address => {
        await this.browser.setState(`persAddress.address.${address.id}`, address);
        await this.browser.setState(`Checkouter.collections.order.${orderIndex++}`, {
            id: orderIndex,
            status: ORDER_STATUS.DELIVERED,
            region: region['Москва'],
            delivery: {
                regionId: address.regionId,
                buyerAddress: address,
                type: DELIVERY_TYPES.DELIVERY,
            },
            paymentType: PAYMENT_TYPE.PREPAID,
            paymentMethod: PAYMENT_METHOD.YANDEX,
        });
    };
}

export default makeSuite('Покупка списком. Чекаут. Флоу повторного заказа', {
    id: 'marketfront-5901',
    issue: 'MARKETFRONT-81924',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: mergeSuites({
        async beforeEach() {
            this.browser.allure.runStep('Плагин Auth: логин', async () => {
                const retpathPageId = PAGE_IDS_COMMON.ORDER_CONDITIONS;
                const retpathParams = {
                    lr: region['Москва'],
                };

                const fullRetpath = await this.browser.yaBuildFullUrl(retpathPageId, retpathParams);
                return this.browser.yaMdaTestLogin(null, null, fullRetpath);
            });

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            const addDeliveredOrderWithAddresses = makeDeliveredOrder.call(this);

            await addDeliveredOrderWithAddresses(ADDRESSES.MOSCOW_ADDRESS);
            await addDeliveredOrderWithAddresses(ADDRESSES.MOSCOW_LAST_ADDRESS);

            await this.browser.setState('report.data.search.shops', shopsWithOffers);
            await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);
            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    region: region['Москва'],
                    checkout2: true,
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    queryParams: {
                        purchaseList: 1,
                    },
                }
            );
        },
    },
    prepareSuite(firstStep),
    prepareSuite(secondStep)
    ),
});
