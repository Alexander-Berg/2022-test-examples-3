import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок PromoDealsHeader для промокодов магазина
 * @param {PageObject.PromoDealsHeader} promoDealsHeader
 */
export default makeSuite('Заголовок промокодов магазинов.', {
    params: {
        shopName: 'Ожидаемое название магазина',
    },
    story: {
        'Для промокодов магазина': {
            'отображает корректный заголовок': makeCase({
                issue: 'MOBMARKET-10919',
                id: 'm-touch-2526',
                async test() {
                    const text = await this.promoDealsHeader.getHeaderText();

                    return this.expect(text).to.be.equal(
                        `Товары магазина ${this.params.shopName} со скидкой по промокоду`,
                        'Текст заголовка корректный'
                    );
                },
            }),
        },
    },
});
