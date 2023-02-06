import {
    makeSuite,
    makeCase,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {discount} from '@self/root/src/spec/hermione/configs/card';
import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import CartOfferDetails
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferDetails/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartOfferPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';

// import CheckoutCartPage from '@self/project/src/widgets/content/checkout/common/CheckoutCartPage/components/View/__pageObject';
// import CheckoutPaymentPage
//     from '@self/project/src/widgets/content/checkout/common/CheckoutPaymentPage/components/View/__pageObject';
// import CheckoutSummaryPage
//     from '@self/project/src/widgets/content/checkout/common/CheckoutSummaryPage/components/View/__pageObject';
// import CheckoutCartItems from '@self/project/src/widgets/content/checkout/common/CheckoutCartItems/components/View/__pageObject';
import OrderItem from '@self/root/src/components/OrderItem/__pageObject';
// import CheckoutProcessNextStep
//     from '@self/project/src/widgets/content/checkout/common/CheckoutProcessNextStep/components/View/__pageObject';
import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.desktop.js';

export default makeSuite('Корзина.', {
    feature: 'Корзина',
    defaultParams: {
        isAuthWithPlugin: true,
        items: [{
            skuId: discount.skuId,
            offerId: discount.offerId,
        }],
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                cartOffer: () => this.createPageObject(CartOffer, {parent: this.cartItem}),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                cartItem: () => this.createPageObject(CartItem, {parent: this.cartItems.firstItem}),
                cartOfferDetails: () => this.createPageObject(CartOfferDetails, {parent: this.cartItem}),
                cartOfferPrice: () => this.createPageObject(CartOfferPrice, {parent: this.cartOffer}),
                discountPrice: () => this.createPageObject(DiscountPrice, {parent: this.cartOfferPrice}),
                orderTotal: () => this.createPageObject(OrderTotal),

                // checkoutCartPage: () => this.createPageObject(CheckoutCartPage),
                // checkoutPaymentPage: () => this.createPageObject(CheckoutPaymentPage),
                // checkoutSummaryPage: () => this.createPageObject(CheckoutSummaryPage),
                // nextButtonCartPage: () => this.createPageObject(CheckoutProcessNextStep, {
                //     parent: this.checkoutCartPage,
                // }),
                // nextButtonPaymentPage: () => this.createPageObject(CheckoutProcessNextStep, {
                //     parent: this.checkoutPaymentPage,
                // }),
                // checkoutCartItems: () => this.createPageObject(CheckoutCartItems, {
                //     parent: this.checkoutCartPage,
                // }),
                firstItem: () => this.createPageObject(OrderItem, {
                    parent: this.checkoutCartItems.firstItem,
                }),
                deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions, {
                    parent: this.checkoutCartPage,
                    root: `${DeliveryTypeOptions.root}`,
                }),
            });

            // готовим корзину для оформления
            return this.browser.yaScenario(
                this,
                'cart.prepareCartPageBySkuId',
                {
                    items: this.params.items,
                    region: this.params.region,
                }
            );
        },

        'При клике на кнопку "Перейти к оформлению"': {
            'просходит переход на страницу чекаута': makeCase({
                id: 'bluemarket-2713',
                issue: 'BLUEMARKET-6248',
                environment: 'testing',

                async test() {
                    await this.cartOffer.isVisible()
                        .should.eventually.be.equal(true, 'Корзинный оффер должен быть показан');

                    await this.cartCheckoutButton.goToCheckout();

                    const [openedUrl, expectedPath] = await Promise.all([
                        this.browser.getUrl(),
                        this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT),
                    ]);

                    await this.expect(openedUrl).to.be.link({pathname: expectedPath}, {
                        skipProtocol: true,
                        skipHostname: true,
                    });
                },
            }),
        },
    },
});
