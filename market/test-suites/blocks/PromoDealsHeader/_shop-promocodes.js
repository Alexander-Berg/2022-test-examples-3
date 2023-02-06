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
                issue: 'MARKETVERSTKA-31557',
                id: 'marketfront-2924',
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
