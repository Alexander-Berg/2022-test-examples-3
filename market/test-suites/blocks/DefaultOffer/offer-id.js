import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок DefaultOffer.
 *
 * @property {PageObject.DefaultOffer} this.defaultOffer
 */
export default makeSuite('offerId', {
    story: {
        'По умолчанию': {
            'соответствует ожидаемому': makeCase({
                params: {
                    expectedOfferId: 'ожидаемый offerId в ДО',
                },
                async test() {
                    await this.defaultOffer.isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что ДО присутствует на странице');

                    return this.defaultOffer.getOfferId()
                        .should.eventually.to.equal(this.params.expectedOfferId, `Проверяем, что offerId в ДО равен "${this.params.expectedOfferId}"`);
                },
            }),
        },
    },
});
