import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок AppPromo
 * @property {PageObject.banner} AppPromo
 */
export default makeSuite('При проставленной куке.', {
    story: {
        beforeEach() {
            return this.browser.yaSetCookie({name: this.params.closeCookie, value: '1'}).yaReactPageReload();
        },
        afterEach() {
            this.browser.deleteCookie(this.params.closeCookie);
        },
        'Не виден на странце.': makeCase({
            test() {
                return this.browser.allure.runStep(
                    'Проверяем, что баннер при проставленной куке не отображается',
                    () => this.banner.isVisible().should.eventually.to.be.equal(false, 'Баннер не виден')
                );
            },
        }),
    },
});
