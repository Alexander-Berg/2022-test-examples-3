import {
    makeCase,
    makeSuite,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';
import CheckoutAuth
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutAuthRequirement/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import checkAuthRetpath from '@self/root/src/spec/utils/checkAuthRetpath';

/**
 * Тест на блок w-checkout-auth
 * @param {PageObject.checkoutAuth}
 */
export default makeSuite('Экран авторизации.', {
    feature: 'Экран авторизации.',
    defaultParams: {
        items: [{
            offerId: cartItemsIds.asus.offerId,
            skuId: cartItemsIds.asus.skuId,
        }],
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                checkoutAuth: () => this.createPageObject(CheckoutAuth),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
            });

            await this.browser.yaScenario(
                this,
                'cart.prepareCartPageBySkuId',
                {
                    items: this.params.items,
                    region: this.params.region,
                }
            );

            await this.cartCheckoutButton.goToCheckout();
        },

        'Кнопка "Продолжить как гость".': {
            'Должна присутствовать': makeCase({
                id: 'bluemarket-3750',
                issue: 'MARKETFRONT-22355',
                test() {
                    return this.checkoutAuth.authDismisButton.isExisting()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что кнопка Продолжить как гость присутствует на странице'
                        );
                },
            }),
            'По нажатию открывается экран чекаута': makeCase({
                id: 'bluemarket-3750',
                issue: 'MARKETFRONT-22355',
                async test() {
                    await this.browser.yaWaitForChangeUrl(
                        () => this.checkoutAuth.dismissLogin(),
                        15000
                    );

                    const [openedUrl, expectedPath] = await Promise.all([
                        this.browser.getUrl(),
                        this.browser.yaBuildURL(PAGE_IDS_COMMON.CHECKOUT2),
                    ]);

                    await this.expect(openedUrl).to.be.link(
                        expectedPath,
                        {
                            skipProtocol: true,
                            skipHostname: true,
                        }
                    );
                },
            }),
        },
        'Кнопка "Войти".': {
            'Должна присутствовать': makeCase({
                id: 'bluemarket-3750',
                issue: 'MARKETFRONT-22355',
                test() {
                    return this.checkoutAuth.authButton.isExisting()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что кнопка Войти присутствует на странице'
                        );
                },
            }),
            'По нажатию открывается экран паспорта': makeCase({
                id: 'bluemarket-3750',
                issue: 'MARKETFRONT-22355',
                async test() {
                    await this.browser.yaWaitForChangeUrl(() => this.checkoutAuth.login());
                    await checkAuthRetpath.call(this, {
                        page: PAGE_IDS_COMMON.CHECKOUT,
                        params: {
                            strategy: 'consolidate-without-crossdock',
                            loggedin: 1,
                        },
                    });
                },
            }),
        },
    },
});
