import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferSummary} offerSummary
 */
export default makeSuite('Заголовок блока "Гарантии"', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = await this.returns.warrantyTitle.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Заголовка нет');
                },
            }),
        },
    },
});
