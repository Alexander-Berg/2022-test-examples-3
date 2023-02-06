import {makeCase, makeSuite} from 'ginny';


/**
 * Тест на форму для отзыва на магазин, поле Номер заказа
 * @property {PageObject.ShopReviewNew} shopReviewNew
 */
export default makeSuite('Форма отзыва на магазин. Поле "Номер заказа"', {
    params: {
        expectedOrderId: 'значение параметра orderId',
    },
    story: {
        'При перенном query-параметре orderId': {
            'значение будет выставлено в форму': makeCase({
                id: 'm-touch-3249',
                async test() {
                    await this.expect(await this.shopReviewNew.getOrderIdValue())
                        .to.be.equal(this.params.expectedOrderId, 'Значение в поле ввода совпадает с нужным');
                },
            }),
        },
    },
});
