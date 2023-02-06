import {
    makeCase,
    mergeSuites,
    makeSuite,
} from 'ginny';

import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import Header from '@self/platform/spec/page-objects/widgets/core/Header';
import Footer from '@self/platform/spec/page-objects/Footer';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import CartEmptyView from '@self/root/src/widgets/content/cart/CartEmpty/components/View/__pageObject';
import CheckoutAuth
    from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutAuthRequirement/__pageObject';
import COOKIE_NAME from '@self/root/src/constants/cookie';


export default makeSuite('Деградация.', {
    environment: 'testing',
    feature: 'Деградация',
    defaultParams: {
        items: [{
            skuId: cartItemsIds.asus.skuId,
            offerId: cartItemsIds.asus.offerId,
        }],
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    header: () => this.createPageObject(Header),
                    footer: () => this.createPageObject(Footer),
                    cartItem: () => this.createPageObject(CartItem, {root: `${CartItem.root}:nth-child(1)`}),
                    checkoutAuth: () => this.createPageObject(CheckoutAuth),
                    cartOrderCheckout: () => this.createPageObject(CartCheckoutButton, {
                        parent: this.orderInfo,
                    }),
                    cartEmpty: () => this.createPageObject(CartEmptyView),
                });
            },
        },

        makeSuite('Отказ лоялти.', {
            id: 'bluemarket-2614',
            issue: 'BLUEMARKET-6792',
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.LOYALTY]: {
                                isCrashed: true,
                            },
                        }),
                    },
                },
            },
            story: getTests(),
        }),

        makeSuite('Отказ бэкэнда избранного.', {
            id: 'bluemarket-3363',
            issue: 'BLUEMARKET-8473',
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.WISHLIST]: {
                                isCrashed: true,
                            },
                        }),
                    },
                },
            },
            story: getTests(),
        }),

        makeSuite('Отказ картера.', {
            id: 'bluemarket-2669',
            issue: 'BLUEMARKET-6792',
            environment: 'kadavr',
            defaultParams: {
                cookie: {
                    [COOKIE_NAME.AT_ENDPOINTS_SETTINGS]: {
                        name: COOKIE_NAME.AT_ENDPOINTS_SETTINGS,
                        value: JSON.stringify({
                            [BACKENDS_NAME.CARTER]: {
                                isCrashed: true,
                            },
                        }),
                    },
                },
            },
            story: {
                beforeEach() {
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);
                },

                'Страница должна содержать шапку и футер': makeCase({
                    async test() {
                        await this.header.isVisible()
                            .should.eventually.to.be.equal(true, 'Шапка должна быть видна');

                        await this.browser.yaSlowlyScroll(Footer.root);

                        await this.footer.isVisible()
                            .should.eventually.to.be.equal(true, 'Футер должен быть виден');
                    },
                }),
                'Страница должна содержать заголовок': makeCase({
                    async test() {
                        await this.cartEmpty.title.isVisible()
                            .should.eventually.to.be.equal(true, 'Заголовок должен быть виден');
                    },
                }),
            },
        })
    ),
});

function getTests() {
    return mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);

                return this.browser.yaScenario(
                    this,
                    'cart.prepareCartPageBySkuId',
                    {
                        items: this.params.items,
                        region: this.params.region,
                    }
                );
            },
        },
        {
            'Страница должна содержать шапку и футер': makeCase({
                async test() {
                    await this.header.isVisible()
                        .should.eventually.to.be.equal(true, 'Шапка должна быть видна');

                    await this.browser.yaSlowlyScroll(Footer.root);

                    await this.footer.isVisible()
                        .should.eventually.to.be.equal(true, 'Футер должен быть виден');
                },
            }),

            'Страница должна содержать айтем': makeCase({
                async test() {
                    return this.cartItem.isVisible()
                        .then(value => (Array.isArray(value) ? value.indexOf(true) !== -1 : value))
                        .should.eventually.be.equal(true, 'Корзинный айтем должен быть виден');
                },
            }),
        }
    );
}
