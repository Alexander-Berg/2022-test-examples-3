import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.BetterChoice} betterChoice
 */
export default makeSuite('Спецификация.', {
    story: {
        'Всегда': {
            'содержит ожидаемый текст': makeCase({
                async test() {
                    const actualText = await this.betterChoice.getSpecsText();

                    await this.expect(actualText)
                        .to.be.equal(
                            this.params.specs
                        );
                },
            }),
        },
    },
});
