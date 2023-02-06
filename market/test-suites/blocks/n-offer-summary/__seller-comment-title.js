import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferSummary} offerSummary
 */
export default makeSuite('Заголовок блока "Комментарии магазина"', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = this.sellerComment.sellerCommentTitle.isVisible();

                    await this.expect(visible).not.to.be.equal(null, 'Заголовок отсутствует');
                },
            }),
        },
    },
});
