import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок AppPromo
 * @property {PageObject.banner} AppPromo
 */
export default makeSuite('По умолчанию.', {
    story: {
        'Виден на странце.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Проверяем, что баннер приложения присутствует на странице',
                    () => this.banner.isVisible().should.eventually.to.equal(true, 'Присутствует на странице')
                );

                const title = await this.banner.getTitle();
                return this.browser.allure.runStep(
                    'Проверяем, что баннер приложения содржит правильный заголовок',
                    () => this.expect(title).to.be.equal(this.params.text)
                );
            },
        }),
    },
});
