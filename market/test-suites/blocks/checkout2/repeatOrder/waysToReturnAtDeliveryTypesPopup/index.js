import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// pageObjects
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PickupList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/PickupList/__pageObject';
import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import EditAddressPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditAddressPopup/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import InformationPanel from '@self/root/src/components/InformationPanel/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {
    deliveryOptionsMock,
    deliveryPickupMock,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outlet from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

// suites
import whenDeliveryTypeDelivery from './whenDeliveryTypeDelivery';
import whenDeliveryTypePickup from './whenDeliveryTypePickup';

import {ADDRESSES} from '../../constants';


const orders = [
    {
        id: 777,
        status: ORDER_STATUS.DELIVERED,
        delivery: {
            type: DELIVERY_TYPES.DELIVERY,
            regionId: region['Москва'],
            buyerAddress: ADDRESSES.MOSCOW_ADDRESS,
        },
    },
];

const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

const simpleWithPickupCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
        deliveryOptions: [
            ...deliveryOptionsMock,
            {
                ...deliveryPickupMock,
                outlets: [
                    {id: x5outlet.id, regionId: 0},
                ],
            },
        ],
        outlets: [
            x5outlet,
        ],
    }),
];

export default makeSuite('Сценарии возврата к попапу "Способ доставки".', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    popupBase: () => this.createPageObject(PopupBase, {
                        root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                    }),
                    editPopup: () => this.createPageObject(EditPopup),
                    editAddressPopup: () => this.createPageObject(EditAddressPopup, {
                        parent: this.popupBase,
                    }),
                    popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.editPopup,
                    }),
                    addressList: () => this.createPageObject(AddressList, {
                        parent: this.editPopup,
                    }),
                    pickupList: () => this.createPageObject(PickupList, {
                        parent: this.editPopup,
                    }),
                    pickupCard: () => this.createPageObject(InformationPanel, {
                        parent: this.deliveryInfo,
                    }),
                });

                const profile = profiles.ugctest3;

                await this.browser.yaLogin(profile.login, profile.password);

                await this.browser.setState(`persAddress.address.${ADDRESSES.MOSCOW_ADDRESS.id}`, ADDRESSES.MOSCOW_ADDRESS);
                await this.browser.setState('persAddress.pickpoint', {
                    [x5outlet.id]: {
                        regionId: region['Москва'],
                        pickId: x5outlet.id,
                        lastOrderTime: (new Date()).toISOString(),
                    },
                });
                await this.browser.setState('Checkouter.collections.order', orders);
            },
        },
        prepareSuite(whenDeliveryTypeDelivery, {
            meta: {
                id: 'marketfront-5026',
                issue: 'MARKETFRONT-54551',
            },
            params: {
                carts: simpleCarts,
            },
        }),
        prepareSuite(whenDeliveryTypePickup, {
            meta: {
                id: 'marketfront-5027',
                issue: 'MARKETFRONT-54551',
            },
            params: {
                carts: simpleWithPickupCarts,
            },
        })
    ),
});
