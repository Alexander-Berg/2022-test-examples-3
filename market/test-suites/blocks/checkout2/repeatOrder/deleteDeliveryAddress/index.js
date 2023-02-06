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
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject/index.js';
// eslint-disable-next-line max-len
import DeliveryActionButton from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

// suites
import oneAddress from './oneAddress';
import twoAddresses from './twoAddresses';

const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Удаление адреса доставки при условии.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    popupBase: () => this.createPageObject(PopupBase, {
                        root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                    }),
                    editPopup: () => this.createPageObject(EditPopup, {
                        parent: this.popupBase,
                    }),
                    editAddressPopup: () => this.createPageObject(EditAddressPopup, {
                        parent: this.popupBase,
                    }),
                    editableAddressCard: () => this.createPageObject(AddressCard, {
                        parent: this.editAddressPopup,
                    }),
                    addressList: () => this.createPageObject(AddressList, {
                        parent: this.editPopup,
                    }),
                    street: () => this.createPageObject(GeoSuggest, {
                        parent: this.addressForm,
                    }),
                    deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
                });
            },
        },
        prepareSuite(oneAddress, {
            meta: {
                id: 'marketfront-4428',
                issue: 'MARKETFRONT-36085',
            },
            params: {
                carts: simpleCarts,
            },
        }),

        prepareSuite(twoAddresses, {
            meta: {
                id: 'marketfront-4429',
                issue: 'MARKETFRONT-36085',
            },
            params: {
                carts: simpleCarts,
            },
        })
    ),
});
