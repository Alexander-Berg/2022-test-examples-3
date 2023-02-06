import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import CartCheckoutButton
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject/';
import CheckoutRecipient
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import EditableCard
    from '@self/root/src/components/EditableCard/__pageObject';
import EditPaymentOption
    from '@self/root/src/components/EditPaymentOption/__pageObject';
import PaymentOptionsList
    from '@self/root/src/components/PaymentOptionsList/__pageObject';

import differentRegions from './differentRegions';
import saveStateBetweenPages
    from './saveStateBetweenPages';

export default makeSuite('ХСЧ.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartCheckoutButton: () => this.createPageObject(
                        CartCheckoutButton
                    ),
                    checkoutRecipient: () => this.createPageObject(
                        CheckoutRecipient
                    ),
                    editPaymentOption: () => this.createPageObject(
                        EditPaymentOption
                    ),
                    editPaymentOptionCard: () => this.createPageObject(
                        EditableCard,
                        {parent: this.editPaymentOption}
                    ),
                    paymentOptionsList: () => this.createPageObject(
                        PaymentOptionsList
                    ),
                });
            },
        },

        prepareSuite(differentRegions, {
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(saveStateBetweenPages, {
            suiteName: 'Сохранение стейта при переключении между страницами.',
            meta: {
                id: 'marketfront-4867',
            },
            params: {
                isAuthWithPlugin: true,
            },
        }),

        prepareSuite(saveStateBetweenPages, {
            suiteName: 'Гость. Сохранение стейта при переключении между страницами.',
            meta: {
                id: 'marketfront-4878',
            },
            params: {
                isAuthWithPlugin: false,
            },
        })
    ),
});
