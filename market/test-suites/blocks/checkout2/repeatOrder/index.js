import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import ConfirmationPage
    from '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';

import AddressCard from '@self/root/src/components/AddressCard/__pageObject/index.js';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.desktop.js';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
// eslint-disable-next-line max-len
import DeliveryActionButton from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';
import onDemandDeliveryFeature from '@self/root/src/spec/hermione/test-suites/blocks/checkout/confirmation/onDemandDeliveryFeature';
import CourierSuggest from
    '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';
import Tooltip from '@self/root/src/widgets/content/checkout/common/CheckoutPin/components/Tooltip/__pageObject';
import Text from '@self/root/src/uikit/components/Text/__pageObject';

import displayingUnavailableDeliveryMessage from './displayingUnavailableDeliveryMessage';
import selectUnavailableDelivery from './selectUnavailableDelivery';
import userWithoutPresets from './userWithoutPresets';
import typeNewAddress from './typeNewAddress';
import checkCoordinatesWhenManualInput from './checkCoordinatesWhenManualInput';
import alertAboutInvalidAddress from './alertAboutInvalidAddress';
import selectPresetFromLastOrder from './selectPresetFromLastOrder';
import waysToReturnAtDeliveryTypesPopup from './waysToReturnAtDeliveryTypesPopup';
import simpleState from './hschSaveDataWhenReturnInBucket/simpleState';
import miltiCartState from './hschSaveDataWhenReturnInBucket/miltiCartState';
import addressEditing from './addressEditing';
import userEditing from './userEditing';
import editDiveliryAddress from './editDiveliryAddress';
import displayingPresetsAtDeliveryTypesPopup from './displayingPresetsAtDeliveryTypesPopup';
import addNewRecipient from './addNewRecipient';
import validationRecipientFields from './validationRecipientFields';
import choosePvzForDSBS from './choosePvzForDSBS';
import waysToReturnAtRecipientPopup from './waysToReturnAtRecipientPopup';
import gettingDataFromPreviousOrder from './gettingDataFromPreviousOrder';
import deleteDeliveryAddress from './deleteDeliveryAddress';
import backToChangeAddressPopup from './backToChangeAddressPopup';
import deleteRecipient from './deleteRecipient';
import orderComposition from './orderComposition';
import waysToCreateAddress from './waysToCreateAddress';
import changeDeliveryAddress from './changeDeliveryAddress';

export default makeSuite('Повторная покупка.', {
    id: 'marketfront-4629',
    issue: 'MARKETFRONT-45593',
    feature: 'Повторная покупка.',
    params: {
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    confirmationPage: () => this.createPageObject(ConfirmationPage),
                    deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    addressForm: () => this.createPageObject(AddressForm, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    courierSuggest: () => this.createPageObject(CourierSuggest, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    street: () => this.createPageObject(GeoSuggest, {
                        parent: this.addressForm,
                    }),
                    courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                        parent: this.courierSuggest,
                    }),
                    addressBlocks: () => this.createPageObject(GroupedParcels, {
                        parent: this.confirmationPage,
                    }),
                    addressBlock: () => this.createPageObject(GroupedParcel, {
                        parent: this.addressBlocks,
                    }),
                    addressEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.addressBlock,
                    }),
                    recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                        parent: this.confirmationPage,
                    }),
                    recipientEditableCard: () => this.createPageObject(EditableCard, {
                        root: `${CheckoutRecipient.root}${EditableCard.root}`,
                        parent: this.confirmationPage,
                    }),
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                        parent: this.confirmationPage,
                    }),
                    paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                        parent: this.paymentOptionsBlock,
                    }),
                    deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                        parent: this.addressEditableCard,
                    }),
                    addressCard: () => this.createPageObject(AddressCard, {
                        parent: this.deliveryInfo,
                    }),
                    checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                        parent: this.confirmationPage,
                    }),
                    popupBase: () => this.createPageObject(PopupBase, {
                        root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                    }),
                    editPopup: () => this.createPageObject(EditPopup),
                    deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
                    tooltip: () => this.createPageObject(Tooltip, {
                        parent: this.pinMap,
                    }),
                    tooltipAddress: () => this.createPageObject(Text, {
                        parent: this.tooltip,
                        root: `${Text.root}[data-auto="address"]`,
                    }),
                });
            },
        },

        prepareSuite(displayingUnavailableDeliveryMessage, {
            params: {
                region: region['Москва'],
            },
        }),

        prepareSuite(selectUnavailableDelivery, {
            params: {
                region: region['Москва'],
            },
        }),

        prepareSuite(userWithoutPresets, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(typeNewAddress, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(checkCoordinatesWhenManualInput, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(alertAboutInvalidAddress, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(waysToReturnAtDeliveryTypesPopup),

        prepareSuite(simpleState, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(miltiCartState, {
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

        prepareSuite(addressEditing),

        prepareSuite(editDiveliryAddress, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(displayingPresetsAtDeliveryTypesPopup, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(addNewRecipient, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: false,
            },
        }),

        prepareSuite(selectPresetFromLastOrder),

        prepareSuite(choosePvzForDSBS),

        prepareSuite(validationRecipientFields),

        prepareSuite(gettingDataFromPreviousOrder),

        prepareSuite(waysToReturnAtRecipientPopup),

        prepareSuite(deleteDeliveryAddress),

        prepareSuite(backToChangeAddressPopup),

        prepareSuite(deleteRecipient),

        prepareSuite(orderComposition),

        prepareSuite(waysToCreateAddress),

        prepareSuite(onDemandDeliveryFeature),

        prepareSuite(changeDeliveryAddress, {
            params: {
                region: region['Москва'],
                isAuthWithPlugin: true,
            },
        })
    ),
});
