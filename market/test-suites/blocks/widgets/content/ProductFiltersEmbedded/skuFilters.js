import {makeSuite, makeCase} from 'ginny';
import {isEmpty} from 'ambar';
import {getLastReportRequestParams} from '@self/platform/spec/hermione/helpers/getLastReportRequestParams';

/**
 * @property {PageObject.VisualEnumFilter} visualEnumFilter
 */
export default makeSuite('Фильтр товара с sku', {
    story: {
        'KM c SKU. На странице SKU-фильтра': {
            'нет кнопки сброса фильтра (нет варианта "Все")" ': makeCase({
                id: 'm-touch-3582',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу всех фильтров с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-filters',
                                Object.assign({}, this.params.initialPageUrl, {sku: 1})
                            )
                        )
                    );
                    await this.filterCompound.clickOnCard();
                    await this.selectFilter.waitForVisible();

                    const expectedElementExist = await this.selectFilter.getValueItemById('visual-filter-control-all');

                    return this.expect(isEmpty(expectedElementExist.value)).to.be.equal(true, 'Фильтр отсутствует на странице');
                },

            }),
        },
        'КМ без параметра sku. При выборе фильтра с sku.': {
            'в репорт уходит запрос с правильными параметрами': makeCase({
                id: 'm-touch-3448',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу всех фильтров без параметра SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-filters',
                                Object.assign({}, this.params.initialPageUrl)
                            )
                        )
                    );

                    const filterIdWithSku = this.params.applyFilterValueIds[1];

                    await this.filterCompound.clickOnCard();
                    await this.selectFilter.waitForVisible();
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.selectFilter.selectValueItemById(filterIdWithSku)
                    );
                    await this.filterPopup.apply();
                    await this.filterPopup.waitForSpinnerHidden();

                    const {'market-sku': skuId} = await getLastReportRequestParams(this, 'productoffers');

                    return this.expect(Number(skuId)).to.not.be.NaN;
                },

            }),
        },
        'КМ с параметром sku. При выборе фильтра с sku': {
            'в репорт уходит запрос с правильным SkuId': makeCase({
                id: 'm-touch-3451',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу всех фильтров с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-filters',
                                Object.assign({}, this.params.initialPageUrl, {sku: 1})
                            )
                        )
                    );
                    const filterIdWithSku = this.params.applyFilterValueIds[1];

                    await this.filterCompound.clickOnCard();
                    await this.selectFilter.waitForVisible();
                    await this.browser.allure.runStep('Кликаем по фильтру содержащему sku', () =>
                        this.selectFilter.selectValueItemById(filterIdWithSku)
                    );
                    await this.filterPopup.apply();
                    await this.filterPopup.waitForSpinnerHidden();

                    const {'market-sku': skuId} = await getLastReportRequestParams(this, 'productoffers');

                    return this.expect(Number(skuId)).to.be.equal(Number(filterIdWithSku));
                },
            }),
        },
    },
});
