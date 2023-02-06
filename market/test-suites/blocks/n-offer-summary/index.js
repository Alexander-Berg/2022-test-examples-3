import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на визитку КО
 *
 * @param {PageObject.ClickoutButton} clickoutButton
 */
export default makeSuite('Визитка КО', {
    story: {
        'По умолчанию': {
            'содержит кнопку "В магазин"': makeCase({
                feature: 'Структура страницы',
                id: 'marketfront-402',
                issue: 'MARKETVERSTKA-27520',
                severity: 'critical',
                test() {
                    return this.clickoutButton.isExisting()
                        .should.eventually.be.equal(true, 'Кнопка присутствует');
                },
            }),
        },
    },
});
