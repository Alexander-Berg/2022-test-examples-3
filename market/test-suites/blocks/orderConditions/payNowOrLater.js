import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Блок "Оплата".', {
    feature: 'Блок "Оплата"',
    story: {
        'По умолчанию': {
            'отображается блок “Оплата”': makeCase({
                id: 'bluemarket-2833',
                issue: 'BLUEMARKET-6972',
                async test() {
                    await this.payNowOrLater.isVisible()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что блок “Оплата” виден на странице'
                        );

                    const title = 'Оплата';
                    await this.expect(this.payNowOrLater.getTitle())
                        .to.be.equal(title, `Текст заголовка должен быть “${title}”`);
                },
            }),
        },
    },
});
