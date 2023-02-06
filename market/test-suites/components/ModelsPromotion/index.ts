'use strict';

import {mergeSuites, importSuite, makeSuite} from 'ginny';

/**
 * Тесты на список продвигаемых товаров
 * @param {PageObject.ListContainer} list - список товаров
 */
export default makeSuite('Список товаров.', {
    issue: 'VNDFRONT-2150',
    environment: 'kadavr',
    feature: 'Прогнозатор',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления списка товаров', () => this.list.waitForExist());
                await this.list.waitForLoading();
            },
        },
        importSuite('ModelsPromotion/filters', {
            pageObjects: {
                filters() {
                    return this.createPageObject('Filters');
                },
            },
        }),
        importSuite('ModelsPromotion/modelForecast', {
            pageObjects: {
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ModelsPromotionListItem', this.list, this.list.getItemByIndex(0));
                },
            },
        }),
        importSuite('ListContainer', {
            meta: {
                id: 'vendor_auto-659',
                issue: 'VNDFRONT-3331',
                environment: 'kadavr',
            },
        }),
    ),
});
