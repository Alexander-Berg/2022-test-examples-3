import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import CartItemGroup from '@self/root/src/widgets/content/cart/CartList/components/CartItemGroup/__pageObject';
import CartItem from '@self/root/src/widgets/content/cart/CartList/components/CartItem/__pageObject';
import CartBonuses from '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/CartCoins/__pageObject';
import BonusWithTearOff from '@self/root/src/components/BonusWithTearOffControl/__pageObject';
import CartGroup from '@self/root/src/widgets/content/cart/CartLayout/components/View/__pageObject';
import CoinsBadges
    from '@self/root/src/widgets/content/cart/CartList/components/CartOfferPrice/components/CoinsBadge/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import CartAvailableBonusesView from
    '@self/root/src/widgets/content/cart/CartAvailableBonuses/components/View/__pageObject';

import freeDeliveryBonus from './freeDeliveryBonus';
import fixDiscount from './fixDiscount';
import percentDiscount from './percentDiscount';
import autoApply from './autoApply';

/**
 * Тесты на Маркет бонусы в корзине.
 */
module.exports = makeSuite('Купоны.', {
    feature: 'Купоны',
    environment: 'kadavr',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                cartBonuses: () => this.createPageObject(CartBonuses),
                bonus: () => this.createPageObject(
                    BonusWithTearOff,
                    {
                        parent: this.cartBonuses,
                        root: `${BonusWithTearOff.root}:nth-child(1)`,
                    }
                ),
                cartGroup: () => this.createPageObject(CartGroup),
                cartItems: () => this.createPageObject(CartItemGroup, {parent: this.cartGroup}),
                cartItem: () => this.createPageObject(CartItem, {root: `${CartItem.root}`}),
                coinsBadges: () => this.createPageObject(CoinsBadges, {parent: this.cartItem}),
                cartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {parent: this.cartItem}),
                orderTotal: () => this.createPageObject(OrderTotal),
                bonusParent: () => this.createPageObject(CartAvailableBonusesView),
            });
        },

        'Применение купонов.': mergeSuites(
            prepareSuite(fixDiscount),
            prepareSuite(percentDiscount),
            prepareSuite(freeDeliveryBonus),
            prepareSuite(autoApply)
        ),
    },
});
