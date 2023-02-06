import {makeSuite, makeCase} from 'ginny';

import {waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';
import {getLastReportRequestParams} from '@self/platform/spec/hermione/helpers/getBackendRequestParams';

const clickOnFilterAndWaitListUpdate = async ({filterColors, browser, snippetList, skuState}) => {
    const TIMEOUT = 10000;
    const clickOnFilter = () => filterColors.clickItemByIndex(2);

    await browser.setState('report', skuState);
    await waitForSuccessfulSnippetListUpdate(
        browser,
        clickOnFilter,
        snippetList,
        TIMEOUT
    );
};


export default makeSuite('Страница цен c параметром sku', {
    story: {
        'По клику на фильтр со связанным sku': {
            'в репорт уходит запрос с правильным SkuId': makeCase({
                id: 'marketfront-5136',
                issue: 'MARKETFRONT-58266',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу всех фильтров с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'market:product-offers',
                                Object.assign({}, this.params.initialPageUrl, {sku: 1})
                            )
                        )
                    );
                    await clickOnFilterAndWaitListUpdate({
                        filterColors: this.filterColors,
                        browser: this.browser,
                        snippetList: this.snippetList,
                        skuState: this.params.skuState,
                    });

                    const {'market-sku': skuId} = await getLastReportRequestParams(this, 'productoffers');

                    return this.expect(Number(skuId)).to.be.equal(Number(this.params.skuYellowId));
                },
            }),
        },
        'При переходе на главную КМ (по клику на название товара) параметр sku сохраняется в урле': makeCase({
            id: 'marketfront-5137',
            issue: 'MARKETFRONT-58266',
            async test() {
                await this.browser.allure.runStep('Открываем страницу всех фильтров с параметром SKU', () =>
                    this.browser.yaWaitForChangeUrl(() =>
                        this.browser.yaOpenPage(
                            'market:product-offers',
                            Object.assign({}, this.params.initialPageUrl, {sku: 1})
                        )
                    )
                );
                await this.miniCard.productTitleClick();
                const {query} = await this.browser.yaParseUrl();
                return this.expect(Boolean(query.sku))
                    .to.be.equal(true, 'sku параметр находится в url');
            },
        }),
        'При клике на "Ещё N вариантов» в карточке офера отправляется параметр market_sku в репорт': makeCase({
            id: 'marketfront-5138',
            issue: 'MARKETFRONT-58266',
            async test() {
                await this.browser.allure.runStep('Открываем страницу всех фильтров с параметром SKU', () =>
                    this.browser.yaWaitForChangeUrl(() =>
                        this.browser.yaOpenPage(
                            'market:product-offers',
                            Object.assign({}, this.params.initialPageUrl, {sku: this.params.skuYellowId})
                        )
                    )
                );
                await this.moreOffersLink.moreOffersClick();
                const {'market-sku': skuId} = await getLastReportRequestParams(this, 'productoffers');
                return this.expect(Number(skuId)).to.be.equal(Number(this.params.skuYellowId));
            },
        }),
        'В SKU фильтрах отсутствует вариант "Выбрать все"': makeCase({
            id: 'marketfront-5139',
            issue: 'MARKETFRONT-58266',
            async test() {
                const isVisible = await this.filterRadio.isFilterVisible(`${this.params.enumFilterId}_-1`);
                return this.expect(isVisible).to.be.equal(false, 'Вариант "Выбрать все" отсутствует');
            },
        }),
        'При переходе главную КМ (по хлебной крошке в шапке) sku параметр сохраняется в урле': makeCase({
            id: 'marketfront-5140',
            issue: 'MARKETFRONT-58266',
            async test() {
                await this.browser.allure.runStep('Открываем страницу всех фильтров с параметром SKU', () =>
                    this.browser.yaWaitForChangeUrl(() =>
                        this.browser.yaOpenPage(
                            'market:product-offers',
                            Object.assign({}, this.params.initialPageUrl, {sku: 1})
                        )
                    )
                );
                await this.miniCard.backToProductLinkClick();
                const {query} = await this.browser.yaParseUrl();
                return this.expect(Boolean(query.sku))
                    .to.be.equal(true, 'sku параметр находится в url');
            },
        }),
    },
});
