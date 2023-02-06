import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ShopRatingSummary} reviewsShopRatingSummary
 */
export default makeSuite('Блок рейтинга магазина', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = await this.reviewsShopRatingSummary.rating.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Блока с рейтингом нет на странице');
                },
            }),
        },
    },
});
