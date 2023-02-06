import {
    makeSuite,
    makeCase,
} from 'ginny';


/**
 * Тестирование виджета с инфой о заказе
 * Должен быть задан PageObject orderTotal @self/root/src/components/OrderTotal/__pageObject
 */
export default makeSuite('Общая информация о заказе.', {
    environment: 'kadavr',
    params: {
        count: 'Общее количество товаров',
        deliveryType: 'Тип доставки SHOP или YANDEX',
        price: 'Стоимость товаров',
        deliveryPrice: 'Стоимость доставки',
        liftingPrice: 'Стоимость подъема на этаж',
    },
    defaultParams: {
        deliveryType: 'YANDEX',
        liftingPrice: 0,
    },
    story: {
        'Все данные на месте.': makeCase({
            async test() {
                const {
                    count,
                    price,
                    deliveryPrice,
                    liftingPrice,
                } = this.params;
                const totalPrice = Number(price) + Number(deliveryPrice) + Number(liftingPrice);

                await this.orderTotal.getItemsCount()
                    .should.eventually.to.be.equal(
                        count,
                        `Товаров должно быть - ${count}`
                    );

                await this.orderTotal.getItemsValue()
                    .should.eventually.to.be.equal(
                        Number(price),
                        `Товары должны стоить - ${price}`
                    );

                await this.orderTotal.getPriceValue()
                    .should.eventually.to.be.equal(
                        totalPrice,
                        `Итого должно быть - ${totalPrice}`
                    );
            },
        }),
    },
});
