import {
    prepareSuite,
    makeSuite,
    mergeSuites,
} from 'ginny';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCheckoutCartStateWithPharma} from '@self/root/src/spec/hermione/fixtures/cart/pharmaCart';

import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';

import {ADDRESSES, CONTACTS} from '../../../../constants';
import {makeDeliveredOrder} from '../../../../helpers';
import firstStep from './firstStep';
import secondStep from './secondStep';

export default makeSuite('Флоу повторного заказа', {
    id: 'marketfront-5901',
    issue: 'MARKETFRONT-81924',
    feature: 'Флоу повторного заказа',
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    geoSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    medicalCartPlacemarkMap: () => this.createPageObject(PlacemarkMap, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                    deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.medicalCartDeliveryEditorCheckoutWizard,
                    }),
                });

                const bucket = buildCheckouterBucket({
                    cartIndex: 2,
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                });

                const delivery = {
                    deliveryOptions: [
                        {
                            ...deliveryPickupMock,
                            paymentOptions: [
                                paymentOptions.yandex,
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

                const addDeliveredOrderWithAddresses = makeDeliveredOrder.call(this);
                await addDeliveredOrderWithAddresses(ADDRESSES.MOSCOW_ADDRESS);
                await addDeliveredOrderWithAddresses(ADDRESSES.MOSCOW_LAST_ADDRESS);

                await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);

                await prepareCheckoutCartStateWithPharma.call(this, {bucket, delivery, withPrescriptionCart: true});
            },
            async afterEach() {
                await this.browser.yaLogout();
            },
        },
        prepareSuite(firstStep),
        prepareSuite(secondStep)
    ),
});
