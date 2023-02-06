import {makeSuite, makeCase} from 'ginny';

import {isEqual} from 'ambar';
import {
    createOffersWithChechedFiltersState,
    SKU_TITLES_RAW,
    UPDATED_SHOP_NAME,
} from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/productWithVisualFilters';


/**
 * @property {PageObject.VisualEnumFilter} visualEnumFilter
 */
export default makeSuite('Км без параметра sku с sku-оффером', {
    story: {
        'По клику на фильтр со связанным sku на главной КМ': {
            'Обновляется список офферов': makeCase({
                id: 'm-touch-3579',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-offers',
                                Object.assign({}, this.params.initialPageUrl)
                            )
                        )
                    );

                    const applyColorFilterId = this.params.applyFilterValueIds[0];
                    const applyTypeFilterId = this.params.applyFilterValueIds[1];
                    const state = createOffersWithChechedFiltersState({applyColorFilterId, applyTypeFilterId});

                    await this.browser.setState('report', state);
                    await this.browser.allure.runStep(
                        'Кликаем по фильтру содержащему sku',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.visualEnumFilter.clickOnValueById(applyColorFilterId),
                            valueGetter: () => this.offerSnippet.getShopName(),
                        }));

                    const offerShopNameAfterRequest = await this.offerSnippet.getShopName();

                    return this.expect(isEqual(UPDATED_SHOP_NAME, offerShopNameAfterRequest)).to.be.equal(true, 'Список офферов обновился');
                },

            }),
            'Тайтл модели обновляется на тайтл SKU': makeCase({
                id: 'm-touch-3578',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-offers',
                                Object.assign({}, this.params.initialPageUrl)
                            )
                        )
                    );

                    const applyColorFilterId = this.params.applyFilterValueIds[0];
                    const applyTypeFilterId = this.params.applyFilterValueIds[1];
                    const state = createOffersWithChechedFiltersState({applyColorFilterId, applyTypeFilterId});

                    await this.browser.setState('report', state);
                    await this.browser.allure.runStep(
                        'Кликаем по фильтру содержащему sku',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.visualEnumFilter.clickOnValueById(applyColorFilterId),
                            valueGetter: () => this.breadcrumbsUnified.getCrumbText(1, true),
                        }));

                    const title = await this.breadcrumbsUnified.getCrumbText(1, true);

                    return this.expect(title).to.be.equal(SKU_TITLES_RAW, 'Тайтл модели обновился на тайтл SKU');
                },

            }),
            'Выставляются все связанные фильтры': makeCase({
                id: 'm-touch-3580',
                issue: 'MARKETFRONT-25487',
                async test() {
                    await this.browser.allure.runStep('Открываем страницу с параметром SKU', () =>
                        this.browser.yaWaitForChangeUrl(() =>
                            this.browser.yaOpenPage(
                                'touch:product-offers',
                                Object.assign({}, this.params.initialPageUrl)
                            )
                        )
                    );

                    const applyColorFilterId = this.params.applyFilterValueIds[0];
                    const applyTypeFilterId = this.params.applyFilterValueIds[1];
                    const state = createOffersWithChechedFiltersState({applyColorFilterId, applyTypeFilterId});

                    await this.browser.setState('report', state);
                    await this.browser.allure.runStep(
                        'Кликаем по фильтру содержащему sku',
                        () => this.browser.yaWaitForChangeValue({
                            action: () => this.visualEnumFilter.clickOnValueById(applyColorFilterId),
                            valueGetter: () => this.visualEnumFilter.getCurrentActiveValues(),
                        }));

                    const visualFilterValues = await this.visualEnumFilter.getCurrentActiveValues();
                    const typeFilterValues = await this.typeEnumFilter.getCurrentActiveValues();

                    return this.expect(isEqual(
                        [...visualFilterValues, ...typeFilterValues],
                        [applyColorFilterId, applyTypeFilterId])
                    ).to.be.equal(true, 'Выставились все связанные фильтры');
                },

            }),
        },
    },
});
