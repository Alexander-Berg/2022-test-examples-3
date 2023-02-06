import {makeCase, makeSuite} from 'ginny';
import {getLastReportRequestParams} from '@self/project/src/spec/hermione/helpers/getBackendRequestParams';
import productWithCpaDo from '@self/platform/spec/hermione/fixtures/product/productWithCpaDo';


const preExpectations = async function (initialClientState, updatedClientState) {
    await this.browser.setState('report', initialClientState);
    await this.browser.yaOpenPage('touch:product-offers', productWithCpaDo.route);
    await this.browser.yaWaitForPageReady();
    await this.productOffers.waitForFiltersBtnVisible();
    await this.productOffers.clickOnAllFiltersButton();

    // после клика на фильтр репорт должен отдать данные с фильтром
    await this.browser.setState('report', updatedClientState);
    await this.browser.yaWaitForChangeUrl(() => this.filter.click());
};

/**
 * Тест на Фильтр «Покупка на Маркете» на вкладке цены.
 * @param {PageObject.CpaFilterTumbler} filter
 * @param snippetCard
 */
export default makeSuite('Фильтр "Покупки на маркете"\\u200B', {
    story: {
        'Должен быть снят': makeCase({
            id: 'm-touch-3467',
            issue: 'MARKETFRONT-16419',
            async test() {
                await preExpectations.call(this, productWithCpaDo.stateWithCpaFilter, productWithCpaDo.state);

                const isCpaUrParamCorrect = await this.browser.waitUntil(
                    () => this.browser.yaCheckUrlParams({cpa: '0'})
                );

                await this.browser.allure.runStep(
                    'Проверяем, что в URL параметр cpa=0',
                    () => this.expect(isCpaUrParamCorrect).to.be.equal(true, 'Параметр cpa=0 в URL')
                );

                await this.browser.allure.runStep(
                    'Проверяем снят ли фильтр',
                    () => this.filter.isChecked()
                        .then(check => this.expect(check).to.be.equal(false, 'Свитч выключенный'))
                );

                const isCpaAny = await this.browser.waitUntil(() =>
                    getLastReportRequestParams(this, 'productoffers')
                        .then(({cpa}) => cpa === 'any')
                );
                await this.browser.allure.runStep(
                    'Проверяем в запросе на репорт отсутствует параметр cpa',
                    () => this.expect(isCpaAny).to.be.equal(true, 'В запросе cpa=any')
                );
            },
        }),
        'Должен быть установлен': makeCase({
            id: 'm-touch-3466',
            issue: 'MARKETFRONT-16419',
            async test() {
                await preExpectations.call(this, productWithCpaDo.state, productWithCpaDo.stateWithCpaFilter);

                const isCpaUrParamCorrect = await this.browser.waitUntil(
                    () => this.browser.yaCheckUrlParams({cpa: '1'})
                );

                await this.browser.allure.runStep(
                    'Проверяем, что в URL cpa=1',
                    () => this.expect(isCpaUrParamCorrect).to.be.equal(true, 'Параметр cpa присутствует в URL')
                );

                await this.browser.allure.runStep(
                    'Проверяем проставлен ли фильтр',
                    async () => this.filter.isChecked()
                        .then(check => this.expect(check).to.be.equal(true, 'Свитч включенный'))
                );

                const isCpaReal = await this.browser.waitUntil(() =>
                    getLastReportRequestParams(this, 'productoffers')
                        .then(({cpa}) => cpa === 'real')
                );
                await this.browser.allure.runStep(
                    'Проверяем в запросе на репорт присутствует параметр cpa',
                    () => this.expect(isCpaReal).to.be.equal(true, 'в запросе cpa=real')
                );
            },
        }),
    },
});
