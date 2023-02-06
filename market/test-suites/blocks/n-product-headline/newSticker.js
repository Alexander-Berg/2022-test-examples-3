import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на наличие стикера "Новинка" на минивизитке
 * @param {PageObject.Stickers} stickers
 */
export default makeSuite('Минивизитка', {
    environment: 'kadavr',
    story: {
        'Стикер "Новинка"': {
            'присутствует': makeCase({
                id: 'marketfront-693',
                issue: 'MARKETVERSTKA-23912',
                async test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что cтикер присутствует',
                        () => this.sticker.isVisible()
                            .should.eventually.to.be.equal(true, 'Стикер виден')
                    );
                },
            }),
        },
    },
});
