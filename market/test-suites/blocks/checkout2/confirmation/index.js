import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {
    prepareCheckoutPage,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';

// pageObjects
import ConfirmationPage
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import CheckoutSummary from '@self/root/src/components/CheckoutSummary/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';

// mocks
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import {deliveryDeliveryMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-prepaid';
import {
    skuMock as largeCargoTypeSkuMock,
    offerMock as largeCargoTypeOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';

// suites
import orderTotalSuite from './orderTotal';
import mainDataSuite from './mainData';
import liftingToFloorSuite from './liftingToFloor';


const dsbsCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: dsbs.skuPhoneMock,
            offerMock: dsbs.offerPhoneMock,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            deliveryPartnerType: 'SHOP',
        }],
    }),
];

const largeCargoCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: largeCargoTypeSkuMock,
            offerMock: largeCargoTypeOfferMock,
            cargoTypes: largeCargoTypeOfferMock.cargoTypes,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
            liftPrice: 2000,
        }],
    }),
];

export default makeSuite('Страница подтверждения заказа.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    preloader: () => this.createPageObject(Preloader),
                    summaryPlaceholder: () => this.createPageObject(SummaryPlaceholder),
                    confirmationPage: () => this.createPageObject(ConfirmationPage),
                    summary: () => this.createPageObject(CheckoutSummary, {
                        parent: this.confirmationPage,
                    }),
                    orderTotal: () => this.createPageObject(OrderTotal, {
                        parent: this.summary,
                    }),
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    this.params.carts
                );

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );

                await this.browser.yaScenario(this, goToConfirmationPage, {userFormData});
            },
        },
        {
            'Для dsbs офера.': mergeSuites(
                prepareSuite(orderTotalSuite, {
                    meta: {
                        id: 'marketfront-4353',
                        issue: 'MARKETFRONT-35405',
                    },
                    params: {
                        carts: dsbsCarts,
                        count: 1,
                        deliveryType: 'SHOP',
                        price: dsbs.offerPhoneMock.prices.value,
                        deliveryPrice: deliveryDeliveryMock.buyerPrice,
                    },
                }),
                prepareSuite(mainDataSuite, {
                    meta: {
                        id: 'marketfront-4353',
                        issue: 'MARKETFRONT-35405',
                    },
                    params: {
                        carts: dsbsCarts,
                        userInfo: userFormData.recipient,
                        pageTitle: 'Оформление',
                        parcelTitle: 'Доставка курьером',
                        parcelsTitle: 'Посылки',
                        selectedAddress: 'Москва, Красная площадь, д. 1',
                        shop: dsbs.offerPhoneMock.shop.name,
                    },
                })
            ),

            'Для КГТ офера с подъемом на этаж.': mergeSuites(
                prepareSuite(orderTotalSuite, {
                    params: {
                        carts: largeCargoCarts,
                        count: 1,
                        price: largeCargoTypeOfferMock.prices.value,
                        deliveryPrice: deliveryDeliveryMock.buyerPrice,
                        liftingPrice: 2000,
                    },
                }),
                prepareSuite(liftingToFloorSuite, {})
            ),
        }
    ),
});
