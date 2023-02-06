import {makeSuite, makeCase} from 'ginny';


/**
 * Для отзыва импортированного от стороннего поставщика.
 *
 * @param {PageObject.ProductReview} productReview
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
                        return this.productReview
                            .hasProviderInfo()
                            .should.eventually.equal(true, 'Текст с информацией про источник отзыва отображается');
                    },
                }),
            },
        },
    },
});
