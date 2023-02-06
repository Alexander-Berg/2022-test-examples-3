import {makeSuite} from 'ginny';

import {
    prepareCheckoutPage,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareUserLastState}
    from '@self/root/src/spec/hermione/scenarios/persAddressResource';

import GroupedParcel
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject/';
import EditableCard from '@self/root/src/components/EditableCard/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
// eslint-disable-next-line
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
// eslint-disable-next-line
import DeliveryActionButton from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';

import {
    carts,
    orders,
    addressState,
    contactState,
} from './mocks';
import autoChooseAddress from './autoChooseAddress';

export default makeSuite('Кейсы при смене регионов', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                groupedParcel: () => this.createPageObject(GroupedParcel),
                editableCard: () => this.createPageObject(EditableCard, {
                    parent: this.groupedParcel,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.editableCard,
                }),
                editPopup: () => this.createPageObject(EditPopup),
                popupDeliveryTypeList: () => this.createPageObject(
                    DeliveryTypeList,
                    {parent: this.editPopup}
                ),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                delieryEditorDeliveryTypeList: () => this.createPageObject(
                    DeliveryTypeList,
                    {parent: this.deliveryEditor}
                ),
                deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
                deliveryInfo: () => this.createPageObject(DeliveryInfo),
            });

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            await this.browser.yaScenario(this, prepareUserLastState);
            await this.browser.setState('Checkouter.collections.order', orders);
            await this.browser.setState('persAddress.address', addressState);
            await this.browser.setState('persAddress.contact', contactState);

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    checkout2: true,
                }
            );
        },
        ['Автоматический выбор ранее созданного адреса, ' +
         'который соответствует выбранному региону'
        ]: autoChooseAddress,
    },
});
