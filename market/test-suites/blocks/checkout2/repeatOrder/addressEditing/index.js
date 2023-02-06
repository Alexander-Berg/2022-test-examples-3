import {
    makeSuite,
    prepareSuite,
    mergeSuites,
} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// pageObjects
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import EditAddressPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditAddressPopup/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

// suites
import activeAddressEditing from './activeAddressEditing';
import inactiveAddressEditing from './inactiveAddressEditing';
import unavailableAddressEditing from './unavailableAddressEditing';

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

export default makeSuite('Редактирование адреса доставки в попапе "Изменить адрес".', {
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
                    addressForm: () => this.createPageObject(AddressForm, {
                        parent: this.editAddressPopup,
                    }),
                    street: () => this.createPageObject(GeoSuggest, {
                        parent: this.addressForm,
                    }),
                });

                await this.browser.setState('Checkouter.collections.order', orders);
            },
        },
        prepareSuite(activeAddressEditing, {
            meta: {
                id: 'marketfront-4435',
                issue: 'MARKETFRONT-36928',
            },
            params: {
                carts: simpleCarts,
            },
        }),

        prepareSuite(inactiveAddressEditing, {
            meta: {
                id: 'marketfront-4436',
                issue: 'MARKETFRONT-36928',
            },
            params: {
                carts: simpleCarts,
            },
        }),

        prepareSuite(unavailableAddressEditing, {
            meta: {
                id: 'marketfront-5053',
                issue: 'MARKETFRONT-36928',
            },
            params: {
                carts: simpleCarts,
            },
        })
    ),
});
