import {makeCase, makeSuite, mergeSuites} from 'ginny';

export default makeSuite('Прямая скидка', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-31390',
    params: {
        promoText: 'Текст акции',
    },
    story: mergeSuites({
        'По умолчанию акция отображается': makeCase({
            async test() {
                await this.directDiscountTerms.isVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Показываем прямую скидку'
                    );
            },
        }),
        'Содержит правильный текст': makeCase({
            async test() {
                await this.directDiscountTerms.getText().should.eventually.to.be.equal(
                    this.params.promoText,
                    'Содержит правильный текст'
                );
            },
        }),
    }),
});
