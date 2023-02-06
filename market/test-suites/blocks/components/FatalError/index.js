import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.FatalError} fatalError
 */
export default makeSuite('Блок ошибки.', {
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser.allure.runStep('Проверям видимость блока', () =>
                        this.fatalError.isVisible()
                            .should.eventually.to.be.equal(true, 'Блок отображается')
                    );
                },
            }),
        },
    },
});
