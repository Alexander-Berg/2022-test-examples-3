import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// pageObjects
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject/index.touch';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject/index.touch';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {
    offerMock as farmaOfferMock,
    skuMock as farmaSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import {
    deliveryDeliveryMock,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

// suites
import activeAddressEditing from './activeAddressEditing';
import inactiveAddressEditing from './inactiveAddressEditing';
import unavailableAddressEditing from './unavailableAddressEditing';

import {ADDRESSES} from '../constants';

const orders = [
    {
        id: 777,
        status: ORDER_STATUS.DELIVERED,
        delivery: {
            type: DELIVERY_TYPES.DELIVERY,
            regionId: region['Москва'],
            buyerAddress: ADDRESSES.MINIMAL_ADDRESS,
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

const multiCarts = [
    buildCheckouterBucket({
        cartIndex: 0,
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
        }],
    }),
    buildCheckouterBucket({
        cartIndex: 1,
        items: [{
            skuMock: farmaSkuMock,
            offerMock: farmaOfferMock,
            count: 1,
        }],
        deliveryOptions: [{
            ...deliveryDeliveryMock,
        }],
    }),
];

export default makeSuite('Редактирование адреса доставки в попапе "Изменить адрес".', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                        parent: this.confirmationPage,
                    }),
                    popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.editPopup,
                    }),
                    fullAddressForm: () => this.createPageObject(FullAddressForm),
                    citySuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.fullAddressForm,
                    }),
                    streetSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: FullAddressForm.street,
                    }),
                    addressCard: () => this.createPageObject(AddressCard, {
                        parent: this.addressBlock,
                    }),
                    addressList: () => this.createPageObject(AddressList, {
                        parent: this.editPopup,
                    }),
                });

                await this.browser.setState('Checkouter.collections.order', orders);
            },
        },
        prepareSuite(activeAddressEditing, {
            meta: {
                id: 'm-touch-3711',
                issue: 'MARKETFRONT-48976',
            },
            params: {
                carts: simpleCarts,
            },
        }),

        prepareSuite(inactiveAddressEditing, {
            meta: {
                id: 'm-touch-3712',
                issue: 'MARKETFRONT-48976',
            },
            params: {
                carts: simpleCarts,
            },
        }),

        prepareSuite(unavailableAddressEditing, {
            meta: {
                id: 'm-touch-3718',
                issue: 'MARKETFRONT-48976',
            },
            params: {
                carts: multiCarts,
            },
        })
    ),
});
