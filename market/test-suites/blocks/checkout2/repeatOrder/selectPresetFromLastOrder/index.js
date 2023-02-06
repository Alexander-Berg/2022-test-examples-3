import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {deliveryPostMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

import presetFromLastOrder from './presetFromLastOrder';
import {ADDRESSES} from '../../constants';

export default makeSuite('Выбор пресета для обычного товара.', {
    id: 'marketfront-4640',
    issue: 'MARKETFRONT-45599',
    feature: 'Выбор пресета для обычного товара.',
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    popupBase: () => this.createPageObject(PopupBase),
                    editPopup: () => this.createPageObject(EditPopup),
                    popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                        parent: this.editPopup,
                    }),
                    addressList: () => this.createPageObject(AddressList, {
                        parent: this.editPopup,
                    }),
                });
            },
        },
        prepareSuite(presetFromLastOrder, {
            suiteName: 'Тип доставки "Курьером".',
            params: {
                carts: [
                    buildCheckouterBucket({
                        cartIndex: 0,
                        items: [{
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                            count: 1,
                        }],
                    }),
                ],
                deliveryType: DELIVERY_TYPES.DELIVERY,
                delivery: [
                    ADDRESSES.MOSCOW_ADDRESS,
                    ADDRESSES.MOSCOW_LAST_ADDRESS,
                ],
            },
        }),

        prepareSuite(presetFromLastOrder, {
            suiteName: 'Тип доставки "Почтой".',
            params: {
                carts: [
                    buildCheckouterBucket({
                        cartIndex: 0,
                        items: [{
                            skuMock: kettle.skuMock,
                            offerMock: kettle.offerMock,
                            count: 1,
                        }],
                        deliveryOptions: [{
                            ...deliveryPostMock,
                            postCodes: [
                                ADDRESSES.MOSCOW_ADDRESS.zip,
                                ADDRESSES.MOSCOW_LAST_ADDRESS.zip,
                            ],
                        }],
                    }),
                ],
                regionId: region['Москва'],
                deliveryType: DELIVERY_TYPES.POST,
                delivery: [
                    ADDRESSES.MOSCOW_ADDRESS,
                    ADDRESSES.MOSCOW_LAST_ADDRESS,
                ],
            },
        })
    ),
});
