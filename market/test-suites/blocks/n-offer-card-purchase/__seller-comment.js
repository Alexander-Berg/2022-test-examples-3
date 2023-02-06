import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferSummary} offerSummary
 */
export default makeSuite('Контент блока "Комментарии магазина"', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = await this.offerCardPurchase.sellerComment.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Комментарии отсутствуют');
                },
            }),
        },
    },
});
