import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import {prepareCheckoutCartStateWithPharma} from '@self/root/src/spec/hermione/fixtures/cart/pharmaCart';

import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject';
import PaymentOptions from '@self/root/src/components/PaymentOptionsList/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';

import {CONTACTS} from '../../../../constants';
import cashOnDelivery from './cashOnDelivery';
import pickupInfo from './pickupInfo';
import courier from './courier';

export default makeSuite('В корзине только фарма.', {
    id: 'marketfront-5859',
    issue: 'MARKETFRONT-81686',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    groupedParcels: () => this.createPageObject(GroupedParcels, {
                        parent: this.confirmationPage,
                    }),
                    recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                        parent: this.confirmationPage,
                    }),
                    medicalCartPlacemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                        parent: this.confirmationPage,
                    }),
                    editPopup: () => this.createPageObject(EditPopup),
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption),
                    paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.paymentOptionsBlock,
                    }),
                    paymentOptions: () => this.createPageObject(PaymentOptions),
                    geoSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                });

                const medicalDelivery = {
                    deliveryOptions: [
                        {
                            ...deliveryPickupMock,
                            paymentOptions: [
                                paymentOptions.yandex,
                                paymentOptions.cashOnDelivery,
                            ],
                            outlets: [
                                {id: x5outletMock.id, regionId: 0},
                                {id: pharma.outletMock.id, regionId: 0},
                            ],
                        },
                        deliveryDeliveryMock,
                    ],
                    outlets: [
                        x5outletMock,
                        pharma.outletMock,
                    ],
                };

                await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);
                await prepareCheckoutCartStateWithPharma.call(this, {delivery: medicalDelivery});
            },
            async afterEach() {
                await this.browser.yaLogout();
            },
        },
        prepareSuite(cashOnDelivery),
        prepareSuite(pickupInfo),
        prepareSuite(courier)
    ),
});
