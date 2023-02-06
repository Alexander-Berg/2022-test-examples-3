import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на отсутствие блока HeadBanner.
 * @param {PageObject.HeadBanner} headBanner
 */
export default makeSuite('Блок баннера.', {
    story: {
        'По умолчанию': {
            'должен отсутствовать': makeCase({
                async test() {
                    return this.browser.allure.runStep(
                        'Проверяем, что баннер отсутствует',
                        () => this.headBanner.isVisible()
                            .should.eventually.to.be.equal(false, 'Баннер отсутствует')
                    );
                },
            }),
        },
    },
});
