import {
    makeSuite,
    makeCase,
} from 'ginny';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {SummaryPlaceholder} from '@self/root/src/components/OrderTotalV2/components/SummaryPlaceholder/__pageObject';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

import {skuMock, offerMock as _offerMock} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {
    LOW_COST_DISCOUNT_PRICE,
    LOW_COST_ITEM_PRICE,
} from '@self/root/src/spec/hermione/configs/cart/checkouter';

const offerMock = {
    ..._offerMock,
    prices: {
        currency: 'RUR',
        value: LOW_COST_ITEM_PRICE - LOW_COST_DISCOUNT_PRICE,
        isDeliveryIncluded: false,
        rawValue: LOW_COST_ITEM_PRICE - LOW_COST_DISCOUNT_PRICE,
        discount: {oldMin: LOW_COST_ITEM_PRICE, percent: 16, isBestDeal: false},
    },
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Саммари.', {
    feature: 'Саммари',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                orderTotal: () => this.createPageObject(OrderTotal),
                orderInfoPreloader: () => this.createPageObject(SummaryPlaceholder, {parent: this.orderInfo}),
            });

            const cart = buildCheckouterBucket({
                items: [{
                    skuMock,
                    offerMock,
                    buyerPrice: LOW_COST_ITEM_PRICE - LOW_COST_DISCOUNT_PRICE,
                    buyerPriceNominal: LOW_COST_ITEM_PRICE,
                    buyerPriceBeforeDiscount: LOW_COST_ITEM_PRICE,
                }],
                region: this.params.region,
                itemsTotal: LOW_COST_ITEM_PRICE - LOW_COST_DISCOUNT_PRICE,
                buyerTotalBeforeDiscount: LOW_COST_ITEM_PRICE,
                buyerItemsTotalDiscount: LOW_COST_ITEM_PRICE - LOW_COST_DISCOUNT_PRICE,
                buyerItemsTotalBeforeDiscount: LOW_COST_ITEM_PRICE,
                buyerTotalDiscount: LOW_COST_DISCOUNT_PRICE,

                promos: [{
                    buyerItemsDiscount: LOW_COST_DISCOUNT_PRICE,
                    deliveryDiscount: 0,
                    type: 'SECRET_SALE',
                }],
            });

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                [cart]
            );

            return this.browser.yaScenario(
                this,
                'cart.prepareCartPageBySkuId',
                {
                    items: [{
                        skuId: skuMock.id,
                    }],
                    region: this.params.region,
                    reportSkus: testState.reportSkus,
                }
            );
        },
        'Товар со скидкой.': {
            'Информация по корзине.': makeCase({
                id: 'bluemarket-2717',
                issue: 'BLUEMARKET-5558',
                async test() {
                    const totalCount = await this.orderTotal.getItemsCount();
                    await this.expect(totalCount)
                        .eventually
                        .equal(
                            1,
                            'Отображается количество товара и равно 1'
                        );

                    const basketPrice = await this.orderTotal.getItemsValue();
                    await this.expect(basketPrice)
                        .eventually
                        .equal(
                            LOW_COST_ITEM_PRICE,
                            'Указана правильная цена до скидки'
                        );
                    const discountPrice = await this.orderTotal.getDiscount();

                    await this.expect(discountPrice)
                        .eventually
                        .equal(
                            LOW_COST_DISCOUNT_PRICE,
                            'Указан правильный размер скидки'
                        );

                    return this.orderTotal.getPriceValue()
                        .should
                        .eventually
                        .equal(
                            basketPrice - discountPrice,
                            `Итоговая стоимость товаров в корзине правильная и равна ${basketPrice - discountPrice}`
                        );
                },
            }),
        },
    },
});
