import {makeCase, makeSuite} from 'ginny';

import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';

/**
 * Проверяет сновную инфу в корзине
 */
module.exports = makeSuite('Основная информация в корзине', {
    environment: 'kadavr',

    params: {
        itemCount: 'Количество товаров',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                cartHeader: () => this.createPageObject(CartHeader),
            });
        },

        'Содержит ожидаемые данные': makeCase({
            async test() {
                await this.cartHeader.getTitleText()
                    .should.eventually.to.be.include(
                        CART_TITLE,
                        `Заголовок корзины должен содержать текст "${CART_TITLE}"`
                    );
            },
        }),
    },
});
