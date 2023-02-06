import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import onDemandDeliveryFeature from '@self/root/src/spec/hermione/test-suites/blocks/checkout/confirmation/onDemandDeliveryFeature';

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
import CheckoutRecipient from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import GroupedParcels from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';

import singleCartState from './hschSaveDataWhenReturnInBucket/singleCartState';
import multiCartState from './hschSaveDataWhenReturnInBucket/multiCartState';
import userEditing from './userEditing';
import createCourierAddress from './createCourierAddress';
import choosePvzForDSBS from './choosePvzForDSBS';
import addressEditing from './addressEditing';
import unavailableDeliveryPreset from './unavailableDeliveryPreset';
import recipientFormValidation from './recipientFormValidation';
import deleteRecipient from './deleteRecipient';
import changeDeliveryAddress from './changeDeliveryAddress';
import medical from './medical';

export default makeSuite('Повторная покупка.', {
    id: 'm-touch-3645',
    issue: 'MARKETFRONT-50733',
    feature: 'Повторная покупка.',
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
                    recipientEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.recipientBlock,
                    }),
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                        parent: this.confirmationPage,
                    }),
                    paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.paymentOptionsBlock,
                    }),
                    paymentOptionsModal: () => this.createPageObject(Modal),
                });
            },
        },
        prepareSuite(singleCartState, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(multiCartState, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(userEditing, {
            params: {
                region: region['Москва'],
            },
        }),

        prepareSuite(createCourierAddress),

        prepareSuite(choosePvzForDSBS),

        prepareSuite(addressEditing),

        prepareSuite(unavailableDeliveryPreset),

        prepareSuite(recipientFormValidation),

        prepareSuite(deleteRecipient),

        prepareSuite(onDemandDeliveryFeature),

        prepareSuite(changeDeliveryAddress, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(medical)
    ),
});
