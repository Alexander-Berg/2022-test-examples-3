import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на наличие блока HeadBanner.
 * @param {PageObject.HeadBanner} headBanner
 */
export default makeSuite('Блок баннера.', {
    story: {
        'Должен быть виден.': makeCase({
            async test() {
                return this.browser.allure.runStep(
                    'Проверяем, что баннер',
                    () => this.headBanner.isVisible()
                        .should.eventually.to.be.equal(true, 'Баннер виден')
                );
            },
        }),
    },
});
