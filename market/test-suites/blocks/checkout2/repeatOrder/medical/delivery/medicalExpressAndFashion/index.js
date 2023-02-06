import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';
import {PAYMENT_METHOD, PAYMENT_TYPE} from '@self/root/src/entities/payment';
import CONTACTS from '@self/root/src/spec/hermione/test-suites/blocks/checkout/constants/contacts';

import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientInfo from '@self/root/src/widgets/content/checkout/layout/components/recipientInfo/__pageObject';
import CheckoutCourierMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPinMap/components/VectorMap/__pageObject';
import CourierSuggest
    from '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';

import firstStep from './firstStep';
import secondStep from './secondStep';
import thirdStep from './thirdStep';

export default makeSuite('Покупка списком. Чекаут. Флоу повторного заказа с фешн товаром.', {
    id: 'marketfront-5900',
    issue: 'MARKETFRONT-81908',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
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

                await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);
                await this.browser.setState('persAddress.lastState', {
                    paymentType: PAYMENT_TYPE.POSTPAID,
                    paymentMethod: PAYMENT_METHOD.YANDEX,
                    contactId: null,
                    parcelsInfo: null,
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

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

                await this.browser.setState('report.data.search.shops', shopsWithOffers);

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                        queryParams: {
                            purchaseList: 1,
                        },
                    }
                );

                this.setPageObjects({
                    addressForm: () => this.createPageObject(AddressForm, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    geoSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    courierSuggest: () => this.createPageObject(CourierSuggest),
                    courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    recipientForm: () => this.createPageObject(RecipientForm),
                    recipientList: () => this.createPageObject(RecipientList),
                    recipientInfo: () => this.createPageObject(RecipientInfo),
                    map: () => this.createPageObject(CheckoutCourierMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    medicalCartPlacemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),

                    fullAddressForm: () => this.createPageObject(FullAddressForm),
                    citySuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.fullAddressForm,
                    }),
                    streetSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: FullAddressForm.street,
                    }),
                });
            },
        },
        prepareSuite(firstStep),
        prepareSuite(secondStep),
        prepareSuite(thirdStep)
    ),
});
