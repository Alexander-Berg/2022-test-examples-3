import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.OfferCardSpecs} offerCardSpecs
 */
export default makeSuite('Заголовок "Описание"', {
    story: {
        'По умолчанию.': {
            'виден на странице': makeCase({
                async test() {
                    const visible = await this.offerCardSpecs.descriptionHeader.isVisible();

                    await this.expect(visible).to.be.equal(true, 'Заголовок не видно на странице');
                },
            }),
        },
    },
});
