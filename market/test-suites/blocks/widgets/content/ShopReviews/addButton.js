import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Widgets.Content.ShopReviews} shopReviews
 */
export default makeSuite('Кнопка "Написать отзыв"', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'видна на странице': makeCase({
                async test() {
                    const visible = await this.shopReviews.addButton.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Кнопки нет на странице');
                },
            }),
        },
    },
});
