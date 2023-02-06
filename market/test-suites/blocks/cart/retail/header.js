import {makeCase, makeSuite} from 'ginny';

import CartHeader from '@self/root/src/widgets/content/cart/CartHeader/components/View/__pageObject';
import {CART_TITLE} from '@self/root/src/entities/checkout/cart/constants';
import yaBuildURL from '@self/root/src/spec/hermione/commands/yaBuildURL';
import {pluralize} from '@self/root/src/utils/string';

/**
 * Проверяет основную инфу в шапке
 */
module.exports = makeSuite('Основная информация в шапке', {
    environment: 'kadavr',

    params: {
        availableItemsCount: 'Количество доступных товаров',
        unavailableItemsCount: 'Количество недоступных товаров',
        cartTitle: 'Заголовок',
        businessId: 'Id магазина',
        businessSlug: 'Слаг магазина',
        thresholds: 'Информация о трешхолдах',
    },

    story: {
        async beforeEach() {
            this.setPageObjects({
                cartHeader: () => this.createPageObject(CartHeader),
            });
        },

        'Содержит ожидаемое название и ссылку на магазин': makeCase({
            async test() {
                await this.cartHeader.getTitleText()
                    .should.eventually.to.be.equal(
                        this.params.cartTitle || CART_TITLE,
                        `Заголовок корзины должен быть "${this.params.cartTitle || CART_TITLE}"`
                    );
                const shopLink = yaBuildURL('market:business', {
                    businessId: this.params.businessId,
                    slug: this.params.businessSlug,
                });
                const actualLink = await this.cartHeader.getLinkHref();

                await this.expect(actualLink, 'Проверяем что ссылка содержит верные парамеры').to.be.link({
                    pathname: shopLink,
                }, {
                    skipProtocol: true,
                    skipHostname: true,
                }, 'Ссылка коррекная');
            },
        }),
        'Содержит верное количество доступных и недоступных товаров': makeCase({
            async test() {
                const expectedAvailable = this.params.availableItemsCount > 0
                    ? `${this.params.availableItemsCount} ${pluralize(
                        this.params.availableItemsCount, 'товар', 'товара', 'товаров')}`
                    : '';
                await this.cartHeader.getAvailableTotalCount()
                    .should.eventually.to.be.equal(expectedAvailable, 'Колличество доступных товаров верное');

                const expectedUnavalible = this.params.unavailableItemsCount > 0
                    ? `${this.params.unavailableItemsCount} ${pluralize(
                        this.params.unavailableItemsCount, 'товара', 'товаров', 'товаров')} нет в наличии`
                    : '';
                await this.cartHeader.getUnavailableTotalCount()
                    .should.eventually.to.be.equal(expectedUnavalible, 'Колличество недоступных товаров верное');
            },
        }),
        'Содержит верную информацию о трешхолдах': makeCase({
            async test() {
                await this.cartHeader.getDeliveryText()
                    .should.eventually.to.be.equal(this.params.thresholds, 'Информация о доставке отображается корректно');
            },
        }),
    },
});
