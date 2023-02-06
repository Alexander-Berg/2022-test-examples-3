import {makeCase, makeSuite} from 'ginny';
import {
    createCombineStrategy,
    createOffer,
    createOfferForSku,
    createSku,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

// scenarios
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';

// page-objects
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import ReorderButton from '@self/root/src/components/Orders/OrderReorderButton/__pageObject';

// fixtures
import skuKettle from '@self/root/src/spec/hermione/kadavr-mock/report/sku/kettle';
import offerKettle from '@self/root/src/spec/hermione/kadavr-mock/report/offer/kettle';

// imports
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

const MINIMUM_QUANTITY = 4;

const ORDER_QUANTITY = MINIMUM_QUANTITY - 1;

const BUNDLE_SETTINGS = {
    quantityLimit: {
        minimum: MINIMUM_QUANTITY,
        limit: 1,
    },
};

const STRATEGY = {
    entity: 'split-strategy',
    default: true,
    name: 'consolidate-without-crossdock',
    buckets: [{
        warehouseId: 123,
        shopId: offerKettle.shop.id,
        isFulfillment: false,
        deliveryDayFrom: 2,
        offers: [{
            wareId: offerKettle.wareId,
            replacedId: offerKettle.wareId,
            count: 0,
        }],
    }],
};

const MINIMUM_QUANTITY_CASE = makeCase({
    async test() {
        const currentUrl = await this.browser.yaWaitForChangeUrl(
            () => this.reorderButton.click(),
            10000
        );

        const expectedUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.CART);

        await this.expect(currentUrl)
            .to.be
            .link(expectedUrl, {
                skipProtocol: true,
                skipHostname: true,
            });

        await this.amountSelect.getCurrentCountText()
            .should.eventually
            .be.equal(`${MINIMUM_QUANTITY} шт`);
    },
});

export default makeSuite('Повторить заказ с минимальным количеством.', {
    id: 'marketfront-5190',
    issue: 'MARKETFRONT-59870',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                amountSelect: () => this.createPageObject(
                    AmountSelect,
                    {parent: this.cartItem}
                ),
                cartItem: () => this.createPageObject(CartItem),
                reorderButton: () => this.createPageObject(ReorderButton),
            });
        },

        'Оффер с минзаказом': {
            async beforeEach() {
                await this.browser.yaScenario(this, prepareOrder, {
                    orders: [{
                        items: [{
                            count: ORDER_QUANTITY,
                            skuId: null,
                            offerName: offerKettle.titles.raw,
                            wareMd5: offerKettle.offerId,
                        }],
                    }],
                    status: ORDER_STATUS.DELIVERED,
                });

                await this.browser.setState(
                    'report',
                    mergeState([
                        createOffer(
                            {
                                ...offerKettle,
                                bundleSettings: BUNDLE_SETTINGS,
                            },
                            offerKettle.offerId
                        ),
                        createCombineStrategy(STRATEGY, STRATEGY.name),
                    ])
                );

                return this.browser.yaProfile('pan-topinambur', PAGE_IDS_COMMON.ORDERS);
            },

            'При нажатии на кнопку "Повторить заказ"': {
                'в корзину добавляется оффер в количестве, указанном в bundleSettings': MINIMUM_QUANTITY_CASE,
            },
        },

        'СКУ с минзаказом': {
            async beforeEach() {
                await this.browser.yaScenario(this, prepareOrder, {
                    orders: [{
                        items: [{
                            count: ORDER_QUANTITY,
                            skuId: skuKettle.id,
                            offerName: offerKettle.titles.raw,
                            wareMd5: offerKettle.offerId,
                        }],
                    }],
                    status: ORDER_STATUS.DELIVERED,
                });

                await this.browser.setState(
                    'report',
                    mergeState([
                        createSku(
                            {
                                ...skuKettle,
                                offers: {
                                    items: [],
                                },
                            },
                            skuKettle.id
                        ),
                        createOfferForSku(
                            {
                                ...offerKettle,
                                bundleSettings: BUNDLE_SETTINGS,
                            },
                            skuKettle.id,
                            offerKettle.offerId
                        ),
                        createCombineStrategy(STRATEGY, STRATEGY.name),
                    ])
                );

                return this.browser.yaProfile('pan-topinambur', PAGE_IDS_COMMON.ORDERS);
            },

            'При нажатии на кнопку "Повторить заказ"': {
                'в корзину добавляется оффер в количестве, указанном в bundleSettings': MINIMUM_QUANTITY_CASE,
            },
        },
    },
});

