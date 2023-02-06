import {makeSuite, makeCase} from 'ginny';

import * as primary from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {prepareCartPageBySkuId} from '@self/root/src/spec/hermione/scenarios/cart';
import {prepareKadavrReportState} from '@self/root/src/spec/hermione/kadavr-mock/report/blueFlash';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import {createCombineStrategy, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import CartGroup from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import DiscountPrice
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/DiscountPrice/__pageObject';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import Notification from '@self/root/src/components/Notification/__pageObject';

import {buildCheckouterBucketLabel, prepareBasicStrategy} from '@self/root/src/spec/utils/checkouter';
import {generateCheckoutCartItemLabelKadavr} from '@self/root/src/spec/utils/kadavr/checkouter';
import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';

export default makeSuite('Blue Flash.', {
    environment: 'kadavr',
    issue: 'BLUEMARKET-10900',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                cartGroup: () => this.createPageObject(CartGroup),
                cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                cartItem: () => this.createPageObject(CartItem, {root: `${CartItem.root}`}),
                discountPrice: () => this.createPageObject(DiscountPrice, {parent: this.cartItem}),

                orderTotal: () => this.createPageObject(OrderTotal),
                priceChangeNotification: () => this.createPageObject(Notification),
            });
        },
        'Акция действует': makeCase({
            id: 'bluemarket-3419',
            async test() {
                await prepareCartPage.call(this, {isExpiredPromo: false});
                const {currentPrice, oldPrice} = this.yaTestData.blueFlash;

                await this.priceChangeNotification.isVisible()
                    .should.eventually.to.be.equal(false, 'Плашка об изменении цены не должна отображаться');

                await this.discountPrice.getOldPriceValue()
                    .should.eventually.to.be.equal(oldPrice,
                        'Должна отображаться правильная старая цена товара');
                await this.discountPrice.getPriceValue()
                    .should.eventually.to.be.equal(currentPrice,
                        'Должна отображаться правильная текущая цена товара');

                return this.orderTotal.getDiscount()
                    .should.eventually.to.be.equal(oldPrice - currentPrice,
                        'Размер скидки должен быть правильный');
            },
        }),
        'Акция закончилась': makeCase({
            id: 'bluemarket-3420',
            async test() {
                await prepareCartPage.call(this, {isExpiredPromo: true});
                const {currentPrice} = this.yaTestData.blueFlash;
                await this.priceChangeNotification.isVisible()
                    .should.eventually.to.be.equal(true, 'Плашка об изменении цены должна отображаться');
                await this.discountPrice.getPriceValue()
                    .should.eventually.to.be.equal(currentPrice,
                        'Должна отображаться правильная текущая цена товара');
            },
        }),
    },
});

async function prepareCartPage({isExpiredPromo}) {
    await this.browser.allure.runStep(
        'Подготавливаем стейт корзины с Flash акцией',
        async () => {
            const cartState = prepareKadavrState(isExpiredPromo);
            const {reportState, cartItem, checkouterState, primarySku, prices} = cartState;
            await this.browser.yaScenario(this, setReportState, {
                state: reportState,
            });

            await this.browser.setState('Checkouter.collections', checkouterState);

            await this.browser.yaScenario(this, prepareCartPageBySkuId, {
                region: this.params.region,
                items: [
                    cartItem,
                ],
                reportSkus: [
                    primarySku,
                ],
            });

            this.yaTestData = this.yaTestData || {};
            this.yaTestData.blueFlash = {
                ...prices,
            };
        }
    );
}


function prepareKadavrState(isExpiredPromo) {
    const promoPriceOptions = {
        promoPrice: {
            currency: 'RUR',
            value: 150,
        },
        discount: {
            oldMin: primary.offerMock.prices.value,
            percent: 75,
        },
    };
    const promoPrice = promoPriceOptions.promoPrice.value;
    const originalPrice = parseFloat(primary.offerMock.prices.value);
    const actualPrice = isExpiredPromo ? originalPrice : promoPrice;
    const reportState = prepareKadavrReportState(
        primary,
        promoPriceOptions
    );

    const {skuMock, offerMock} = reportState.primary;
    const primarySku = {
        ...skuMock,
        offers: {
            items: [offerMock],
        },
    };

    const {cartItem, checkoutCartItem} = createItemMock({
        reportMock: reportState.primary,
        cartItemPrice: promoPrice,
        actualPrice,
    });

    const buyerTotalBeforeDiscount = originalPrice;
    const buyerItemsDiscount = buyerTotalBeforeDiscount - promoPrice;

    const checkouterState = {
        cart: {
            0: {
                shopId: 431782,
                warehouseId: 123,
                label: buildCheckouterBucketLabel(
                    [primary.offerMock.wareId],
                    123
                ),
                items: [0],
                promos: [
                    {
                        type: 'BLUE_FLASH',
                        buyerItemsDiscount,
                    },
                ],
                buyerItemsTotalBeforeDiscount: buyerTotalBeforeDiscount,
                buyerTotalBeforeDiscount,
                buyerItemsTotalDiscount: buyerItemsDiscount,
                buyerTotalDiscount: buyerItemsDiscount,
            },
        },
        cartItem: {
            0: checkoutCartItem,
        },
    };

    const strategy = prepareBasicStrategy(Object.values(checkouterState.cart), Object.values(checkouterState.cartItem));
    const strategyState = createCombineStrategy(strategy, strategy.name);

    return {
        cartItem,
        checkouterState,
        primarySku,
        reportState: mergeState([reportState.state, strategyState]),
        prices: {
            currentPrice: actualPrice,
            oldPrice: originalPrice,
        },
    };
}

function createItemMock({reportMock, cartItemPrice, actualPrice}) {
    const {skuMock, offerMock} = reportMock;
    const {feed} = offerMock;
    const originalPrice = parseFloat(offerMock.prices.value);

    return {
        cartItem: {
            skuId: skuMock.id,
            offerId: offerMock.wareId,
            count: 1,
            price: cartItemPrice,
            isPrimaryInBundle: false,
        },
        checkoutCartItem: {
            offerId: offerMock.wareId,
            feedId: feed.id,
            feedOfferId: feed.offerId,
            buyerPrice: actualPrice,
            buyerPriceNominal: originalPrice,
            buyerPriceBeforeDiscount: originalPrice,
            primaryInBundle: false,
            label: generateCheckoutCartItemLabelKadavr(offerMock),
            count: 1,
            promos: [
                {
                    type: 'BLUE_FLASH',
                    buyerDiscount: originalPrice - actualPrice,
                },
            ],
        },
    };
}
