import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.touch.js';
import EditPaymentOption
    from '@self/root/src/components/EditPaymentOption/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import EditPopup from
    '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject/index.touch';
import CheckoutRecipient from
    '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import GroupedParcels from
    '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';

import smokeTest from './smokeTest';
import multiCartState from './hschSaveDataWhenReturnInBucket';
import stateTest from './state';
import express from './express';
import outletPointDeliveryDateExisting from './outletPointDeliveryDateExisting';
import selectOutlet from './selectOutlet';

export default makeSuite('Первая покупка', {
    feature: 'Первая покупка',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                    recipientForm: () => this.createPageObject(RecipientForm),
                    editPopup: () => this.createPageObject(EditPopup),
                    addressBlocks: () => this.createPageObject(GroupedParcels, {
                        parent: this.confirmationPage,
                    }),
                    addressBlock: () => this.createPageObject(GroupedParcel, {
                        parent: this.confirmationPage,
                    }),
                    addressEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.addressBlock,
                    }),
                    recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                        parent: this.confirmationPage,
                    }),
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                        parent: this.confirmationPage,
                    }),
                    paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.paymentOptionsBlock,
                    }),
                    paymentOptionsModal: () => this.createPageObject(Modal, {
                        root: `${Modal.root} [data-auto="editableCardPopup"]`,
                    }),
                });
            },
        },
        prepareSuite(multiCartState, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(smokeTest),
        prepareSuite(stateTest),
        prepareSuite(outletPointDeliveryDateExisting),
        prepareSuite(express, {
            params: {
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(selectOutlet, {
            params: {
                isAuthWithPlugin: false,
            },
        })
    ),
});
