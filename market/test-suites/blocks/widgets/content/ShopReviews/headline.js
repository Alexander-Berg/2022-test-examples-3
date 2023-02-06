import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Widgets.Content.ShopReviews} shopReviews
 */
export default makeSuite('Заголовок страницы отзывов на магазин', {
    story: {
        'По умолчанию.': {
            'содержит ожидаемый тайтл': makeCase({
                params: {
                    expectedText: 'Ожидаемый текст тайтла',
                },
                async test() {
                    const titleText = this.shopReviews.getHeaderTitleText();
                    await this.expect(titleText).to.be.equal(this.params.expectedText);
                },
            }),
        },
    },
});
