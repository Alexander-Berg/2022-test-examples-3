import {makeSuite, prepareSuite} from 'ginny';

import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';

import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import ShopInfo from '@self/project/src/components/ShopInfo/__pageObject';
import ShopRating from '@self/project/src/components/ShopRating/__pageObject';
import DeliveryInfo from '@self/platform/components/DeliveryInfo/__pageObject';

/**
 * Тесты на блок n-w-shop-info.
 * @param {PageObject.ShopsInfo} shopsInfo
 */
export default makeSuite('DSBS офер. Блок с информацией о продавце.', {
    environment: 'kadavr',
    params: {
        shopName: 'Название магазина',
        gradesCount: 'Количество отзывов',
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                defaultOffer: () => this.createPageObject(DefaultOffer),
                shopInfo: () => this.createPageObject(ShopInfo, {parent: this.defaultOffer}),
                shopRating: () => this.createPageObject(ShopRating, {parent: this.defaultOffer}),
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {parent: this.defaultOffer}),
            });
        },

        'должен содержать кнопку добавить в корзину': prepareSuite(CartButtonSuite),
    },
});
