import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Widgets.ShopReviewsToolbar} reviewsToolbar
 */
export default makeSuite('Заголовок "Рекомендованные отзывы"', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = await this.reviewsToolbar.title.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Заголовок отсутствует');
                },
            }),
        },
    },
});
