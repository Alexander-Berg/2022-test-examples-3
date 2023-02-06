import {
    makeCase,
    mergeSuites,
    makeSuite,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {BACKENDS_NAME} from '@self/root/src/constants/backendsIdentifier';

import Header from '@self/platform/spec/page-objects/header2';
import Footer from '@self/platform/spec/page-objects/footer-market';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';
import CartTotalInformation from
    '@self/root/src/widgets/content/cart/CartTotalInformation/components/View/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import CartEmptyView from '@self/root/src/widgets/content/cart/CartEmpty/components/View/__pageObject';
import COOKIE_NAME from '@self/root/src/constants/cookie';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {skuMock, offerMock as _offerMock} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {LOW_COST_ITEM_PRICE} from '@self/root/src/spec/hermione/configs/cart/checkouter';

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
                    cartOffer: () => this.createPageObject(CartOffer, {parent: this.cartItem}),
                    orderInfo: () => this.createPageObject(CartTotalInformation, {parent: this.cartGroup}),
                    cartOrderCheckout: () => this.createPageObject(CartCheckoutButton, {
                        parent: this.orderInfo,
                    }),
                    cartEmpty: () => this.createPageObject(CartEmptyView),
                });
            },
        },

        makeSuite('Отказ лоялти.', {
            id: 'bluemarket-2614',
            issue: 'BLUEMARKET-5787',
            environment: 'kadavr',
            defaultParams: {
                items: [{
                    skuId: skuMock.id,
                }],
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
            story: getTests(async function () {
                const offerMock = {
                    ..._offerMock,
                    prices: {
                        currency: 'RUR',
                        value: LOW_COST_ITEM_PRICE,
                        isDeliveryIncluded: false,
                        rawValue: LOW_COST_ITEM_PRICE,
                    },
                };

                const cart = buildCheckouterBucket({
                    items: [{
                        skuMock,
                        offerMock,
                        buyerPrice: LOW_COST_ITEM_PRICE,
                        buyerPriceNominal: LOW_COST_ITEM_PRICE,
                        buyerPriceBeforeDiscount: LOW_COST_ITEM_PRICE,
                    }],
                    region: this.params.region,
                    itemsTotal: LOW_COST_ITEM_PRICE,
                    buyerTotalBeforeDiscount: LOW_COST_ITEM_PRICE,
                    buyerItemsTotalDiscount: LOW_COST_ITEM_PRICE,
                    buyerItemsTotalBeforeDiscount: LOW_COST_ITEM_PRICE,
                    buyerTotalDiscount: 0,
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    [cart]
                );

                return testState.reportSkus;
            }),
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
            issue: 'BLUEMARKET-5787',
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

                'Страница должна содержать шапку с лого и футер': makeCase({
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

function getTests(getReportSkus) {
    return mergeSuites(
        {
            async beforeEach() {
                const reportSkus = getReportSkus ? await getReportSkus.call(this) : undefined;

                return this.browser.yaScenario(
                    this,
                    'cart.prepareCartPageBySkuId',
                    {
                        items: this.params.items,
                        region: this.params.region,
                        reportSkus,
                    }
                );
            },
        },
        {
            'Страница должна содержать шапку с лого и футер': makeCase({
                async test() {
                    await this.header.isVisible()
                        .should.eventually.to.be.equal(true, 'Шапка должна быть видна');

                    await this.browser.yaSlowlyScroll(Footer.root);

                    await this.footer.isVisible()
                        .should.eventually.to.be.equal(true, 'Футер должен быть виден');
                },
            }),

            'Страница должна содержать оффер': makeCase({
                async test() {
                    return this.cartOffer.isVisible()
                        .should.eventually.be.equal(true, 'Корзинный оффер должен быть виден');
                },
            }),
        }
    );
}
