import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на наличие блока HeadBanner.
 * @param {PageObject.HeadBanner} headBanner
 */
export default makeSuite('Блок баннера.', {
    story: {
        'По умолчанию': {
            'должен быть виден.': makeCase({
                async test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что баннер виден',
                        () => this.headBanner.isVisible()
                            .should.eventually.to.be.equal(true, 'Баннер виден')
                    );
                },
            }),
        },
    },
});
