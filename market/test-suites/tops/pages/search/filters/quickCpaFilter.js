import {makeCase, makeSuite} from 'ginny';
import productWithCpaDo from '@self/platform/spec/hermione/fixtures/product/productWithCpaDo';

import {getLastReportRequestParams} from '@self/project/src/spec/hermione/helpers/getBackendRequestParams';

/**
 * Тест на Фильтр «Покупка на Маркете» на вкладке цены.
 * @param {PageObject.CpaFilterTumbler} filter
 */
export default makeSuite('Фильтр "Покупки на маркете"', {
    story: {
        'Должен быть установлен': makeCase({
            id: 'm-touch-3464',
            issue: 'MARKETFRONT-16419',
            async test() {
                await this.browser.setState('report', productWithCpaDo.state);
                await this.browser.yaOpenPage('touch:search', {...productWithCpaDo.route, hid: 1234});

                await this.browser.yaWaitForPageReady();

                await this.browser.allure.runStep(
                    'Проверяем, что в URL нет параметра CPA',
                    () => this.browser.yaCheckUrlParams({cpa: undefined})
                        .should.eventually.to.be.equal(true, 'Параметр cpa отсутствует в URL')
                );

                await this.browser.setState('report', productWithCpaDo.stateWithCpaFilter);
                await this.browser.yaWaitForChangeUrl(() => this.quickCpaFilter.click());

                await this.browser.waitUntil(
                    () => this.browser.yaCheckUrlParams({cpa: '1'})
                );

                await this.browser.allure.runStep(
                    'Проверяем, что в URL cpa=1',
                    () => this.browser.yaCheckUrlParams({cpa: '1'})
                        .should.eventually.to.be.equal(true, 'Параметр cpa присутствует в URL')
                );

                const {cpa} = await getLastReportRequestParams(this);
                await this.browser.allure.runStep(
                    'Проверяем в запросе на репорт присутствует параметр cpa=real',
                    () => this.expect(cpa).to.be.equal('real', 'В запросе cpa="real"')
                );

                await this.browser.allure.runStep(
                    'Проверяем снят ли фильтр',
                    () => this.quickCpaFilter.isChecked()
                        .then(check => this.expect(check).to.be.equal(true, 'Фильтр включенный'))
                );

                const {query} = await this.snippet.getSnippetUrl();
                const {cpa: cpaGetParam} = query;
                await this.browser.allure.runStep(
                    'Проверяем в ссылке на КМ присутствует параметр cpa=1',
                    () => this.expect(cpaGetParam).to.be.equal('1', 'В ссылке на КМ cpa="1"')
                );

                await this.searchOptions.clickOnFiltersButton();
                await this.browser.allure.runStep(
                    'Проверяем включен ли фильтр',
                    () => this.cpaFilter.isChecked()
                        .then(check => this.expect(check).to.be.equal(true, 'Фильтр включенный'))
                );
            },
        }),
    },
});
