import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferSummary} offerSummary
 */
export default makeSuite('Заголовок блока "Описание"', {
    story: {
        'По умолчанию': {
            'виден на странице': makeCase({
                async test() {
                    const visible = this.offerSpecs.descriptionTitle.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Заголовок отсутствует');
                },
            }),
        },
    },
});
