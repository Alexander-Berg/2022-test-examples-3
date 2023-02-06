import {makeSuite, mergeSuites} from 'ginny';

import CartLayout from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartTotalInformation from
    '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import Promocode from '@self/root/src/components/Promocode/__pageObject';
import SubmitField from '@self/root/src/components/SubmitField/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Корзина.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    cartGroup: () => this.createPageObject(CartLayout),
                    orderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                    orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    promocodeWrapper: () => this.createPageObject(Promocode, {parent: this.orderInfo}),
                    promocodeInput: () => this.createPageObject(SubmitField, {parent: this.promocodeWrapper}),
                });
            },
        }
    ),
});
