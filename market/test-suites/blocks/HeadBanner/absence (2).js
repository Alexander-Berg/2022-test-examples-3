import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на отсутствие блока HeadBanner.
 * @param {PageObject.HeadBanner} headBanner
 */
export default makeSuite('Блок баннера.', {
    story: {
        'На странице, где блок скрыт': {
            'должен быть не виден.': makeCase({
                test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что баннер не виден',
                        () => this.headBanner.isVisible()
                            .should.eventually.to.be.equal(false, 'Баннер не виден')
                    );
                },
            }),
        },
    },
});
