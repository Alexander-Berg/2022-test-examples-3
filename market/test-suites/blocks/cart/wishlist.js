import {makeCase, makeSuite, mergeSuites} from 'ginny';

import knownThumbnails from '@self/root/src/spec/hermione/kadavr-mock/knownThumbnails';
import {
    createOffer,
    mergeState,
    createSku,
    createProductForSku,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    skuMock as skuMockCheckout,
    offerMock as offerMockCheckout,
    productMock as productMockCheckout,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import AuthProxy from '@self/root/src/widgets/content/AuthProxyPopup/components/AuthProxy/__pageObject';
import WishlistToggler from '@self/root/src/components/WishlistToggler/__pageObject';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {NON_BREAKING_SPACE_CHAR as NBSP} from '@self/root/src/constants/string';


const offer = createOffer(offerMockCheckout, offerMockCheckout.wareId);
const productCheckout = createProductForSku(productMockCheckout, skuMockCheckout.id, productMockCheckout.id);
const skuCheckout = {
    ...skuMockCheckout,
    offers: {
        items: [offerMockCheckout],
    },
};
const sku2Checkout = createSku(skuMockCheckout, skuMockCheckout.id);

export default makeSuite('Список избранного', {
    environment: 'kadavr',
    feature: 'Список избранного',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaScenario(this, 'wishList.deleteWishlistItems');

                this.setPageObjects({
                    orderTotal: () => this.createPageObject(OrderTotal),
                    orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    wishlistToggler: () => this.createPageObject(WishlistToggler,
                        {parent: `${CartItem.root}:nth-child(1)`}),
                    authProxy: () => this.createPageObject(AuthProxy),
                });

                const defaultState = mergeState([sku2Checkout, skuCheckout, productCheckout, {
                    data: {
                        search: {
                            total: 2,
                            ...knownThumbnails,
                        },
                    },
                }, offer]);
                const cart = buildCheckouterBucket({
                    items: [{
                        skuMock: skuMockCheckout,
                        offerMock: offerMockCheckout,
                    }],
                });

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    [cart],
                    {existingReportState: defaultState}
                );

                return this.browser.yaScenario(
                    this,
                    'cart.prepareCartPageBySkuId',
                    {
                        items: [{
                            skuId: skuMockCheckout.id,
                        }],
                        region: this.params.region,
                        reportSkus: testState.reportSkus,
                    }
                );
            },

            'Должно быть доступно для незрячих': makeCase({
                id: 'marketfront-5259',
                issue: 'MARKETFRONT-58847',
                defaultParams: {
                    isAuth: true,
                },
                async test() {
                    const EXPECTED_WISHLIST_BUTTON_TAG = 'button';
                    const wishlistButtonTag = await this.wishlistToggler.getTagName();
                    await this.wishlistToggler.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'У товара есть кнопка добавления в избранное'
                        );

                    const textBeforeClick = await this.wishlistToggler.getAriaLabel();

                    await this.wishlistToggler.click();

                    await this.wishlistToggler.waitForCheckedState('true');

                    const textAfterClick = await this.wishlistToggler.getAriaLabel();

                    await this.expect(textBeforeClick).be.equal(`Добавить${NBSP}в${NBSP}избранное`);

                    await this.expect(textAfterClick).be.equal(`Удалить${NBSP}из${NBSP}избранного`);

                    await this.expect(wishlistButtonTag).be.equal(
                        EXPECTED_WISHLIST_BUTTON_TAG,
                        `Тег добавление заказа в избранное должен быть "${EXPECTED_WISHLIST_BUTTON_TAG}".`
                    );
                },
            }),

            'Для неавторизованного пользователя': {
                'при попытке добавить в избранное показывается попап авторизации': makeCase({
                    id: 'bluemarket-2734',
                    issue: 'BLUEMARKET-6886',
                    defaultParams: {
                        isAuth: false,
                    },
                    async test() {
                        await this.wishlistToggler.isVisible()
                            .should.eventually.be.equal(
                                true,
                                'У товара есть кнопка добавления в избранное'
                            );

                        await this.wishlistToggler.click();

                        await this.authProxy.waitForVisible();

                        await this.authProxy.isVisible()
                            .should.eventually.be.equal(
                                true,
                                'Попап авторизации должен появиться'
                            );

                        const exceptedCause = 'Войдите или зарегистрируйтесь, чтобы добавлять товары в избранное';
                        await this.authProxy.getCauseText()
                            .should.eventually.to.be.equal(
                                exceptedCause,
                                `Причина показа попапа авторизации должна быть "${exceptedCause}"`
                            );
                    },
                }),
            },

            'Для авторизованного пользователя': {
                'товар добавляется в список и удаляется': makeCase({
                    environment: 'kadavr',
                    id: 'bluemarket-2733',
                    issue: 'BLUEMARKET-6886',
                    async test() {
                        await this.wishlistToggler.isVisible()
                            .should.eventually.be.equal(
                                true,
                                'У товара есть кнопка добавления в избранное'
                            );

                        await this.wishlistToggler.click();

                        await this.wishlistToggler.waitForCheckedState('true');

                        const addItem = await this.browser.yaGetLastKadavrLogByBackendMethod(
                            'PersBasket',
                            'addItemV2'
                        );
                        await this.expect(addItem.request.body.reference_id)
                            .be.equal(
                                skuMockCheckout.id,
                                'Метод добавления в вишлист должен быть вызван с правильным sku'
                            );

                        await this.wishlistToggler.click();

                        await this.wishlistToggler.waitForCheckedState('false');

                        const removingItem = await this.browser.yaGetLastKadavrLogByBackendMethod(
                            'PersBasket',
                            'removeItemV2'
                        );
                        await this.expect(Boolean(removingItem))
                            .be.equal(
                                true,
                                'Метод удаления из вишлиста должен быть вызван'
                            );
                    },
                }),
            },
        }
    ),
});
