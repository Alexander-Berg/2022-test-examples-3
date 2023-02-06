import {makeSuite, makeCase, mergeSuites} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {prepareKadavrState} from '@self/root/src/spec/hermione/test-suites/blocks/product/skuCheapestAsGift';

import CartGroup from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CheapestAsGiftBadge from '@self/root/src/components/PromoBadge/__pageObject';
import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import PriceDistributionNotification
    from '@self/root/src/widgets/content/cart/CartNotifications/components/PriceDistributionNotification/__pageObject';

import {
    offerMock,
    skuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/cheapestAsGift';

import {prepareCartPageBySkuId} from '@self/platform/spec/hermione/scenarios/cart';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';

const offerMockItemPrice = Number.parseInt(offerMock.prices.value, 10);

export default makeSuite('Акция 2=3.', {
    feature: 'Акция 2=3 (самый дешевый товар в подарок)',
    issue: 'BLUEMARKET-10086',
    environment: 'kadavr',
    params: {
        region: 'Регион',
    },
    defaultParams: {
        region: region['Москва'],
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.setPageObjects({
                    cartGroup: () => this.createPageObject(CartGroup),
                    cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                    cartItem: () => this.createPageObject(CartItem, {root: `${CartItem.root}`}),

                    cartItemCheapestAsGiftBadge: () => this.createPageObject(CheapestAsGiftBadge, {
                        parent: this.cartItem,
                    }),
                    discountPrice: () => this.createPageObject(DiscountPrice, {parent: this.cartItem}),

                    orderTotal: () => this.createPageObject(OrderTotal),
                    priceDistributionNotification: () => this.createPageObject(PriceDistributionNotification),
                });

                const {state} = prepareKadavrState();

                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock,
                            offerMock,
                            count: 3,
                        }],
                    }),
                ];

                await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts,
                    {existingReportState: state}
                );

                return this.browser.yaScenario(this, prepareCartPageBySkuId, {
                    region: this.params.region,
                    items: [{
                        skuId: skuMock.id,
                        offerId: offerMock.wareId,
                        count: 3,
                    }],
                    reportSkus: [{
                        ...skuMock,
                        offers: {
                            items: [offerMock],
                        },
                    }],
                });
            },
        },

        makeSuite('Бейдж и скидки.', {
            story: {
                'Акционный бейдж отображается корректно': makeCase({
                    id: 'bluemarket-3292',
                    test() {
                        return this.cartItemCheapestAsGiftBadge.isVisible()
                            .should.eventually.to.be.equal(true,
                                'Должен отображаться бейджик акции 2=3 на сниппете товара');
                    },
                }),

                'Отображаются скидки/нотификашки по акции': makeCase({
                    id: 'bluemarket-3293',
                    async test() {
                        await this.priceDistributionNotification.isVisible()
                            .should.eventually.to.be.equal(true, 'Плашка о размазывании цены должна отображаться');

                        await this.discountPrice.getOldPriceValue()
                            .should.eventually.to.be.equal(offerMockItemPrice * 3,
                                'Должна отображаться правильная старая цена товара');
                        await this.discountPrice.getPriceValue()
                            .should.eventually.to.be.equal(offerMockItemPrice * 2,
                                'Должна отображаться правильная текущая цена товара');

                        return this.orderTotal.getDiscount()
                            .should.eventually.to.be.equal(offerMockItemPrice,
                                'Размер скидки в саммари должен быть равен стоимости товара');
                    },
                }),
            },
        })
    ),
});
