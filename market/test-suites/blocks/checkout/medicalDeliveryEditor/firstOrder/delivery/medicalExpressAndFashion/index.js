import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCheckoutCartStateWithPharma} from '@self/root/src/spec/hermione/fixtures/cart/pharmaCart';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';

import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import MedicalPlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import PlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap/components/VectorPlacemarkMap/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import DeliveryActionButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';
import GroupedParcels
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';

import firstStepDeliveryAndCash from './firstStep';
import secondStepDeliveryAndCash from './secondStep';
import thirdStepDeliveryAndCash from './thirdStep';

export default makeSuite('Флоу первого заказа.', {
    id: 'marketfront-5899',
    issue: 'MARKETFRONT-81900',
    feature: 'Покупка списком. Чекаут. Флоу первого заказа',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    geoSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    medicalCartPlacemarkMap: () => this.createPageObject(MedicalPlacemarkMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    deliveryTypes: () => this.createPageObject(DeliveryTypeList),
                    fashionAddressBlock: () => this.createPageObject(GroupedParcel, {
                        parent: this.confirmationPage,
                        root: `${GroupedParcel.root}:nth-child(1)`,
                    }),
                    deliveryFashionActionButton: () => this.createPageObject(DeliveryActionButton, {
                        parent: this.fashionAddressBlock,
                    }),
                    groupedParcels: () => this.createPageObject(GroupedParcels, {
                        parent: this.confirmationPage,
                    }),
                    checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                        parent: this.confirmationPage,
                    }),
                    editPopup: () => this.createPageObject(EditPopup),
                    placemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.deliveryEditorCheckoutWizard,
                    }),
                    recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                        parent: this.confirmationPage,
                    }),
                    courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                });

                const bucket = buildCheckouterBucket({
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
                });

                const medicalDelivery = {
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
                };

                await prepareCheckoutCartStateWithPharma.call(this, {bucket, delivery: medicalDelivery});
            },
        },
        prepareSuite(firstStepDeliveryAndCash),
        prepareSuite(secondStepDeliveryAndCash),
        prepareSuite(thirdStepDeliveryAndCash)
    ),
});
