import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок ScrollBanner
 * @property {PageObject.banner} ScrollAppBanner
 */
export default makeSuite('По умолчанию.', {
    story: {
        'Корректная ссылка': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Проверяем, что баннер не виден',
                    () => this.banner.isVisible()
                        .should.eventually.to.be.equal(false, 'Баннер не виден'));
                await this.browser.allure.runStep(
                    'Скроллим вниз потом вверх и сморим, что баннер виден',
                    async () => {
                        await this.browser.scroll(0, 30);
                        await this.browser.scroll(0, -33);
                        await this.banner.waitForVisible();
                        return this.banner.isVisible()
                            .should.eventually.to.be.equal(true, 'Баннер виден');
                    });
                const url = await this.banner.getUrl();
                return this.expect(url).to.be.equal(this.params.link);
            },
        }),
    },
});
