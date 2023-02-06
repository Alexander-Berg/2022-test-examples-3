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
import RecipientList
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';

import cashOnDelivery from './cashOnDelivery';
import withPrescription from './withPrescription';
import courier from './courier';

export default makeSuite('В корзине только фарма.', {
    id: 'marketfront-5845',
    issue: 'MARKETFRONT-81686',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    defaultParams: {
        isAuthWithPlugin: false,
    },
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
                    recipientEditableCard: () => this.createPageObject(EditableCard, {
                        root: `${CheckoutRecipient.root}${EditableCard.root}`,
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
                    recipientList: () => this.createPageObject(RecipientList),
                    recipientForm: () => this.createPageObject(RecipientForm),
                    recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                        parent: this.recipientForm,
                    }),
                    popupBase: () => this.createPageObject(PopupBase, {
                        root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
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

                await prepareCheckoutCartStateWithPharma.call(this, {delivery: medicalDelivery});
            },
        },
        prepareSuite(cashOnDelivery),
        prepareSuite(withPrescription),
        prepareSuite(courier)
    ),
});
