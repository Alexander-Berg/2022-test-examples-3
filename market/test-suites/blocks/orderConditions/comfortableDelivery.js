import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Блок "Удобная доставка".', {
    feature: 'Блок "Удобная доставка"',
    story: {
        'По умолчанию': {
            'отображается блок “Удобная доставка”': makeCase({
                id: 'bluemarket-2833',
                issue: 'BLUEMARKET-6972',
                async test() {
                    await this.comfortableDelivery.isVisible()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что блок “Удобная доставка” виден на странице'
                        );

                    const title = 'Удобная доставка';
                    await this.expect(this.comfortableDelivery.getTitle())
                        .to.be.equal(title, `Текст заголовка должен быть “${title}”`);
                },
            }),
        },
    },
});
