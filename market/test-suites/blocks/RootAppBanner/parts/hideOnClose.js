import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок AppPromo
 * @property {PageObject.banner} AppPromo
 */
export default makeSuite('При клике на крестик.', {
    story: {
        afterEach() {
            this.browser.deleteCookie(this.params.closeCookie);
        },
        'Проставляется кука.': makeCase({
            async test() {
                await this.banner.close();
                await this.banner.isVisible().should.eventually.to.be.equal(false, 'Баннер не виден');
                const cookie = await this.browser.getCookie(this.params.closeCookie);
                await this.browser.allure.runStep(
                    'Проверяем, что баннер при закрытии проставляет куку',
                    () => this.expect(cookie.value).to.be.equal('1', 'Кука для скрытия проставилась')
                );
            },
        }),
    },
});
