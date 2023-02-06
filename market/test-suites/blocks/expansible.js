import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Expansible} expansible
 */
export default makeSuite('Блок c раскрывающимся контентом.', {
    story: {
        'По умолчанию': {
            'должен быть показан': makeCase({
                id: 'm-touch-1825',
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что блок c раскрывающимся контентом отображается',
                        () =>
                            this.expansible.isVisible()
                                .should.eventually.to.be.equal(true, 'Блок c раскрывающимся контентом отображается')
                    );
                },
            }),
            'кнопка "Читать полностью" должна быть показана': makeCase({
                id: 'm-touch-1825',
                test() {
                    return this.expansible.expandControlVisible(true)
                        .should.eventually.to.be.equal(true, ' Кнопка "Читать полностью" отображается');
                },
            }),
            'кнопка "Скрыть" должна быть скрыта': makeCase({
                id: 'm-touch-1825',
                test() {
                    return this.expansible.constrictControlVisible(false)
                        .should.eventually.to.be.equal(false, 'Кнопка "Скрыть" скрыта');
                },
            }),
            'контент блока должен быть свернут': makeCase({
                id: 'm-touch-1825',
                test() {
                    return this.expansible.isConstricted()
                        .should.eventually.to.be.equal(true, 'Контент блока свернут');
                },
            }),
        },

        'При клик по кнопке "Читать полностью"': {
            'видим весь контент': makeCase({
                id: 'm-touch-1843',
                test() {
                    return this.expansible.clickExpandControl()
                        .then(() => this.expansible.isExpanded())
                        .should.eventually.to.be.equal(true, 'Контент блока развернут');
                },
            }),
            'скрывается кнопка "Читать полностью"': makeCase({
                id: 'm-touch-1843',
                test() {
                    return this.expansible.clickExpandControl()
                        .then(() => this.expansible.expandControlVisible(false))
                        .should.eventually.to.be.equal(false, 'Кнопка "Читать полностью" скрыта');
                },
            }),
            'показывается кнопка "Скрыть"': makeCase({
                id: 'm-touch-1843',
                test() {
                    return this.expansible.clickExpandControl()
                        .then(() => this.expansible.constrictControlVisible(true))
                        .should.eventually.to.be.equal(true, 'Кнопка "Скрыть" отображается');
                },
            }),
        },

        'При клике по кнопке "Скрыть"': {
            'контент блока сворачивается': makeCase({
                id: 'm-touch-1844',
                test() {
                    return this.expansible.clickExpandControl()
                        .then(() => this.expansible.clickConstrictControl())
                        .then(() => this.expansible.isConstricted())
                        .should.eventually.to.be.equal(true, 'Контент блока свернут');
                },
            }),
            'показывается кнопка "Читать полностью"': makeCase({
                id: 'm-touch-1844',
                test() {
                    return this.expansible.clickExpandControl()
                        .then(() => this.expansible.clickConstrictControl())
                        .then(() => this.expansible.expandControlVisible())
                        .should.eventually.to.be.equal(true, 'Кнопка "Читать полностью" отображается');
                },
            }),
            'скрывается кнопка "Скрыть"': makeCase({
                id: 'm-touch-1844',
                test() {
                    return this.expansible.clickExpandControl()
                        .then(() => this.expansible.clickConstrictControl())
                        .then(() => this.expansible.constrictControlVisible())
                        .should.eventually.to.be.equal(false, 'Кнопка "Скрыть" скрыта');
                },
            }),
        },
    },
});
