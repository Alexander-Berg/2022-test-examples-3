import {mergeSuites, makeSuite, makeCase} from 'ginny';
import Search2 from '@self/platform/spec/page-objects/search2';

/**
 * Тесты на блок search2/yandexmarketRegionCookie
 * @param {PageObject.Search2} search2
 * @param {PageObject.Input} input
 */
export default makeSuite('Блок поиска.', {
    feature: 'Регион',
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    search2: () => this.createPageObject(Search2),
                });
            },

            'При клике на кнопку "Найти".': {
                'устанавилвается cookie lr с кодом региона': makeCase({
                    id: 'marketfront-1028',
                    issue: 'MARKETVERSTKA-25089',
                    async test() {
                        await this.search2.input.setValue('платье в Самаре');
                        await this.browser.yaWaitForPageReloadedExtended(
                            () => this.search2.clickSearch(),
                            5000
                        );
                        const cookie = await this.browser.getCookie('lr');
                        return this.expect(cookie.value).to.be.equal('51');
                    },
                }),

            },
        }
    ),
});
