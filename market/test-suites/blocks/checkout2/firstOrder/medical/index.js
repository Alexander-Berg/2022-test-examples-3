import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// Mocks
import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

// Scenarios
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';

// Utils
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// PageObjects
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import TouchPlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/PlacemarkMap/__pageObject';
import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.touch';
import {Button} from '@self/root/src/uikit/components/Button/__pageObject';
import PopupSlider from '@self/root/src/components/PopupSlider/__pageObject';
import CheckoutLayoutConfirmation
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject/index.touch';
import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';

// Suites
import expressAndFashion
    from '@self/platform/spec/hermione/test-suites/blocks/checkout2/firstOrder/medical/delivery/expressAndFashion';

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
    buildCheckouterBucket({
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
    }),
    buildCheckouterBucket({
        cartIndex: 1,
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
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
    }),
];

export default makeSuite('Покупка списком. Чекаут. Флоу первого заказа.', {
    id: 'marketfront-5899',
    issue: 'MARKETFRONT-81900',
    feature: 'Покупка списком. Чекаут. Флоу первого заказа',
    environment: 'kadavr',
    story: mergeSuites(
        {
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

                await this.browser.setState('report.data.search.shops', shopsWithOffers);

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        region: region['Москва'],
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                        queryParams: {
                            purchaseList: 1,
                        },
                    }
                );

                this.setPageObjects({
                    medicalCartPlacemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.medicalCartDeliveryEditor,
                    }),
                    popupSlider: () => this.createPageObject(PopupSlider, {
                        parent: this.medicalCartDeliveryEditor,
                    }),
                    confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                    editPopup: () => this.createPageObject(EditPopup),
                    addressBlocks: () => this.createPageObject(GroupedParcels, {
                        parent: this.confirmationPage,
                    }),
                    recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                        parent: this.confirmationPage,
                    }),
                    placemarkMap: () => this.createPageObject(TouchPlacemarkMap),
                    checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                        parent: this.confirmationPage,
                    }),
                    deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions, {
                        parent: this.medicalCartDeliveryEditor,
                    }),
                    medicalDeliveryButton: () => this.createPageObject(Button, {
                        parent: this.medicalCartDeliveryEditor,
                    }),
                });
            },
        },
        prepareSuite(expressAndFashion)
    ),
});
