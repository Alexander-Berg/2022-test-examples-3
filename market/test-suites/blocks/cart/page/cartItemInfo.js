import {makeCase, makeSuite} from 'ginny';

import CartOffer from '@self/root/src/widgets/content/cart/CartList/components/CartOffer/__pageObject';
import RemoveCartItemContainer
    from '@self/root/src/widgets/content/cart/CartList/containers/RemoveCartItemContainer/__pageObject';
import WishlistToggler from '@self/root/src/components/WishlistToggler/__pageObject';
import AmountSelect from '@self/root/src/components/AmountSelect/__pageObject';

/**
 * Проверяет офер, добавленный в корзину
 * Требует задания PageObject - this.cartItem, this.removedCartItem
 */
module.exports = makeSuite('Информация о товаре.', {
    environment: 'kadavr',

    params: {
        hasRemoveBtn: 'Есть ли кнопка удаления',
        hasWishlistBtn: 'Есть ли кнопка добавления в избранное',
        cantAddAmount: 'Нельзя увеличить количество',
        estimatedText: 'Предупреждение о возможной дате доставки',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                cartOffer: () => this.createPageObject(CartOffer, {
                    parent: this.cartItem,
                }),
                cartItemRemoveButton: () => this.createPageObject(RemoveCartItemContainer, {
                    parent: this.cartItem,
                }),
                wishlistButton: () => this.createPageObject(WishlistToggler, {
                    parent: this.cartItem,
                }),
                amountSelect: () => this.createPageObject(AmountSelect, {
                    parent: this.cartItem,
                }),
            });
        },

        'Присутствует на странице': makeCase({
            async test() {
                const {
                    hasRemoveBtn,
                    cantAddAmount,
                    estimatedText,
                } = this.params;

                await this.cartOffer.isVisible()
                    .should.eventually.be.equal(
                        true,
                        'Должен быть виден'
                    );

                await this.cartItemRemoveButton.isVisible()
                    .should.eventually.be.equal(
                        hasRemoveBtn,
                        `Должна быть ${hasRemoveBtn ? '' : 'не '}видна`
                    );

                if (cantAddAmount) {
                    await this.amountSelect.isPlusDisabled()
                        .should.eventually.be.equal(
                            true,
                            'Кнопка добавления в корзину должны быть блокирована'
                        );
                }

                if (estimatedText) {
                    await this.cartOffer.getEstimatedDayText()
                        .should.eventually.be.equal(
                            estimatedText,
                            `Текст предупреждения о дате доставки должен быть: "${estimatedText}"`
                        );
                }
            },
        }),

        'Оффер можно удалить': makeCase({
            async test() {
                if (!this.params.hasRemoveBtn) {
                    return this.skip('Скипаем тест если не должно быть кнопки удаления');
                }

                await this.cartItem.scrollToItem();

                await this.cartItemRemoveButton.click();

                await this.removedCartItem.waitForRemoved();
            },
        }),
    },
});
