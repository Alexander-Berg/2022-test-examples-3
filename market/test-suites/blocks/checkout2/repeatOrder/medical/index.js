import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

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

import medicalFBSExpressAndDBS from './delivery/medicalFBSExpressAndDBS';
import medicalExpressAndFashion from './delivery/medicalExpressAndFashion';
import onlyMedicalAndCash from './delivery/pickup/onlyMedicalAndCash';

export default makeSuite('Мед. корзина. Повторная покупка', {
    feature: 'Повторная покупка',
    environment: 'kadavr',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        {
            async beforeEach() {
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
        prepareSuite(medicalExpressAndFashion),
        prepareSuite(medicalFBSExpressAndDBS),
        prepareSuite(onlyMedicalAndCash)
    ),
});
