import {
    makeCase,
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {
    createOffer,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import cartItemsIds from '@self/root/src/spec/hermione/configs/cart/items';
import {getCartList, prepareMultiCartState}
    from '@self/root/src/spec/hermione/scenarios/cartResource';
import {offerMock as kettleOfferMock, skuMock as kettleSkuMock}
    from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import UnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc';

import AmountSelect
    from '@self/root/src/components/AmountSelect/__pageObject';
import CartHeader
    from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import CartOffer
    from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import CartOfferAvailabilityInfo
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferAvailabilityInfo/__pageObject';
import Header
    from '@self/platform/spec/page-objects/widgets/core/Header';
import OfferPriceContainer
    from '@self/root/src/containers/Cart/OfferPriceContainer/__pageObject';
import OrderTotal
    from '@self/root/src/components/OrderTotalV2/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import {CartOfferDealTerms}
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferDealTerms/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import {RemovedCartItemNotification}
    from '@self/root/src/widgets/content/cart/CartList/components/CartItem/Notification/__pageObject';
import UnitsCalc from '@self/root/src/components/UnitsCalc/__pageObject';

import cartItemDealTerms from '@self/platform/spec/hermione/test-suites/blocks/cartItemDealTerms';
import cartItemUnitInfo from '@self/platform/spec/hermione/test-suites/blocks/cartItemUnitInfo';

import {unitInfo} from '@self/platform/spec/hermione/fixtures/unitInfo';
import {UNITINFO_EXPECTED_TEXT} from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/constants';
import {getUnitInfoCollectionPath} from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc/helpers';

/**
 * Тесты на блок CartItem.
 * @param {PageObject.CartItem} cartItem
 */
export default makeSuite('Элемент корзины.', {
    feature: 'Оффер',
    defaultParams: {
        items: [{
            skuId: cartItemsIds.asus.skuId,
            offerId: cartItemsIds.asus.offerId,
            slug: cartItemsIds.asus.slug,
        }],
        kadavrItems: [{
            skuId: kettleSkuMock.id,
            offerId: kettleOfferMock.wareId,
            slug: kettleSkuMock.slug,
        }],
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    cartOffer: () => this.createPageObject(CartOffer, {parent: this.cartItem}),
                    cartOfferDealTerms: () => this.createPageObject(
                        CartOfferDealTerms,
                        {parent: this.cartItem}
                    ),
                    cartOfferAvailabilityInfo: () => this.createPageObject(
                        CartOfferAvailabilityInfo,
                        {parent: this.cartOffer}
                    ),
                    amountSelect: () => this.createPageObject(AmountSelect, {parent: this.cartOffer}),
                    offerPriceContainer: () => this.createPageObject(OfferPriceContainer, {parent: this.cartOffer}),
                    removedCartItemNotification: () => this.createPageObject(
                        RemovedCartItemNotification,
                        {parent: this.cartItem}
                    ),
                    cartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {parent: this.cartItem}),
                    header: () => this.createPageObject(Header),
                    cartHeader: () => this.createPageObject(CartHeader),
                });

                await this.browser.yaOpenPage(PAGE_IDS_COMMON.CART);

                let reportSkus = [];

                if (this.getMeta('environment') === 'kadavr') {
                    const result = prepareState.call(this, this.params.kadavrItems);
                    reportSkus = result.reportSkus;

                    const carts = [
                        buildCheckouterBucket({
                            items: [{
                                skuMock: kettleSkuMock,
                                offerMock: kettleOfferMock,
                                count: 1,
                            }],
                        }),
                    ];

                    await this.browser.yaScenario(
                        this,
                        prepareMultiCartState,
                        carts,
                        {existingReportState: result.reportState}
                    );

                    return this.browser.yaScenario(
                        this,
                        'cart.prepareCartPageBySkuId',
                        {
                            items: this.params.kadavrItems,
                            region: this.params.region,
                            reportSkus,
                        }
                    );
                }
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

        makeSuite('Один оффер в корзине, 1 штука.', {
            story: mergeSuites(
                {
                    'При удалении': {
                        async beforeEach() {
                            await this.cartOffer.isVisible()
                                .should.eventually.be.equal(
                                    true,
                                    'Корзинный оффер должен быть показан'
                                );

                            await this.cartItemRemoveButton.waitForVisible();

                            await Promise.all([
                                this.cartItem.waitForRemoved()
                                    .catch(() => true),
                                this.cartItemRemoveButton.click(),
                            ]);
                        },

                        'элемент удаляется из корзины': makeCase({
                            id: 'bluemarket-2238',
                            environment: 'testing',
                            test() {
                                return getCartElementsCount(this)
                                    .should.eventually.be.equal(0, 'Элемента не должно быть в корзине');
                            },
                        }),

                        'скрывается корзинный оффер': makeCase({
                            id: 'bluemarket-2238',
                            environment: 'kadavr',
                            test() {
                                return this.cartItem
                                    .isRemoved()
                                    .should.eventually.be.equal(true, 'Корзинный оффер должен быть скрыт');
                            },
                        }),
                    },

                    'При изменении количества через инпут': {
                        async beforeEach() {
                            if (this.getMeta('environment') === 'kadavr') {
                                const result = prepareState.call(this, this.params.kadavrItems);

                                const carts = [
                                    buildCheckouterBucket({
                                        items: [{
                                            skuMock: kettleSkuMock,
                                            offerMock: kettleOfferMock,
                                            count: this.params.chosenCount,
                                        }],
                                    }),
                                ];

                                await this.browser.yaScenario(
                                    this,
                                    prepareMultiCartState,
                                    carts,
                                    {existingReportState: result.reportState}
                                );
                            }

                            return this.amountSelect.setCurrentCount(this.params.chosenCount);
                        },
                    },
                }
            ),
        }),

        makeSuite('Два оффера в корзине, 1 штука.', {
            defaultParams: {
                items: [{
                    skuId: cartItemsIds.asus.skuId,
                    offerId: cartItemsIds.asus.offerId,
                    slug: cartItemsIds.asus.slug,
                }, {
                    skuId: cartItemsIds.asusZenFone.skuId,
                    offerId: cartItemsIds.asusZenFone.offerId,
                    slug: cartItemsIds.asusZenFone.slug,
                }],
            },
            story: {
                async beforeEach() {
                    this.setPageObjects({
                        orderTotal: () => this.createPageObject(OrderTotal),
                        orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
                    });

                    await this.browser.yaScenario(
                        this,
                        'cart.prepareCartPageBySkuId',
                        {
                            items: this.params.items,
                            region: this.params.region,
                        }
                    );
                },
                'Удаление оффера': makeCase({
                    id: 'bluemarket-2778',
                    issue: 'BLUEMARKET-6354',
                    async test() {
                        // eslint-disable-next-line market/ginny/no-skip
                        return this.skip('MARKETFRONT-49185 Уже были сломаны');

                        // eslint-disable-next-line no-unreachable
                        const oldOrderTotalPrice = await this.orderTotal.getPriceValue();
                        // eslint-disable-next-line no-unreachable
                        await deleteCartElement.call(this);
                        await this.cartOffer.isVisible()
                            .should.eventually.be.equal(true, 'В корзине должен остаться 1 оффер');

                        // eslint-disable-next-line no-unreachable
                        const orderTotalPrice = await this.orderTotal.getPriceValue();

                        // eslint-disable-next-line no-unreachable
                        await this.expect(oldOrderTotalPrice)
                            .to.be.not.equal(orderTotalPrice, 'Сумма итого должна пересчитаться');
                    },
                }),
            },
        }),

        prepareSuite(cartItemUnitInfo, {}),

        prepareSuite(cartItemDealTerms, {}),

        prepareSuite(UnitsCalcSuite, {
            environment: 'kadavr',
            suiteName: 'Калькулятор упаковок.',
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        getUnitInfoCollectionPath(kettleOfferMock.wareId),
                        unitInfo
                    );
                    await this.browser.setState(
                        `report.collections.offer.${kettleOfferMock.wareId}.navnodes`,
                        [{
                            ...kettleOfferMock.navnodes[0],
                            tags: ['unit_calc'],
                        }]
                    );

                    await this.browser.yaReactPageReload();
                },
            },
            params: {
                expectedText: UNITINFO_EXPECTED_TEXT,
            },
            meta: {
                id: 'marketfront-5768',
                issue: 'MARKETFRONT-79800',
            },
            pageObjects: {
                unitsCalc() {
                    return this.createPageObject(UnitsCalc, {
                        parent: this.cartItem,
                    });
                },
            },
        })
    ),
});

/**
 * Получает количество элементов корзины.
 * @param {Test} testCase - тест, в контексте которого будет выполняться сценарий
 * @returns {Promise}
 */
function getCartElementsCount(testCase) {
    return testCase.browser.allure.runStep('Получаем количество элементов в корзине', () =>
        testCase.browser
            .yaScenario(testCase, getCartList)
            .then(res => {
                const cartItems = res.result.items;
                const count = cartItems.length;

                return testCase.browser.allure.runStep(`В корзине ${count} элементов`, () => count);
            })
    );
}

async function deleteCartElement() {
    await this.cartItemRemoveButton.waitForVisible();
    await this.cartItemRemoveButton.click();

    await waitForActualize.call(this);
}

function waitForActualize() {
    return this.browser.allure.runStep('Ждем завершения актуализации', async () => {
        const preloaderVisibility = await this.orderInfoPreloader.waitForVisible();

        if (preloaderVisibility) {
            return this.orderInfoPreloader.waitForHidden(10 * 1000);
        }
    });
}

function prepareState(items) {
    const reportSkus = [];

    const reportState = items.reduce((acc, item) => {
        const offer = createOffer(kettleOfferMock, item.offerId);

        reportSkus.push({
            ...kettleSkuMock,
            id: item.skuId,
            offers: {
                items: [{
                    ...kettleOfferMock,
                    wareId: item.offerId,
                    marketSku: item.skuId,
                }],
            },
        });

        return mergeState([acc, offer]);
    }, {});

    return {
        reportSkus,
        reportState,
    };
}
