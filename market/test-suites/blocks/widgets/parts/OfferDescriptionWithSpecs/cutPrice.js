import {makeSuite, makeCase} from 'ginny';


/**
 * @param {PageObject.OfferDescriptionWithSpecs} offerDescriptionWithSpecs
 * @param {this.params.expectedConditionReason}
 */
export default makeSuite('Виджет "Описание товара"', {
    feature: 'б/у товары',
    story: {
        'Причина уценки': {
            'Для уценённого оффера': {
                'присутствует на странице': makeCase({
                    async test() {
                        const text = await this.offerDescriptionWithSpecs.getCutPriceReasonText();


                        return this.expect(text).to.be.equal(
                            this.params.expectedConditionReason,
                            'Описание причины уценки присутствует на странице'
                        );
                    },
                }),
            },
        },
    },
});
