import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-product-review-item
 * Для отзыва импортированного от стороннего поставщика.
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Блок с отзывом от стороннего поставщика.', {
    feature: 'Отображение отзыва',
    environment: 'kadavr',
    story: {
        'Текст с информацией про источник отзыва.': {
            'По умолчанию': {
                'должен отображаться': makeCase({
                    id: 'marketfront-3055',
                    issue: 'MARKETVERSTKA-30791',
                    test() {
                        return this.productReviewItem
                            .hasProviderInfo()
                            .should.eventually.equal(true, 'Текст с информацией про источник отзыва отображается');
                    },
                }),
            },
        },
    },
});
