'use strict';

import {mergeSuites, makeSuite, importSuite} from 'ginny';

/**
 * Список созданных маркетинговых кампаний
 * @param {boolean} params.isManager - признак менеджера
 * @param {PageObject.PagedList} list - список созданных кампаний
 */
export default makeSuite('Список созданных кампаний.', {
    id: 'vendor_auto-1160',
    issue: 'VNDFRONT-3292',
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления и загрузки списка кампаний', async () => {
                    await this.list.waitForExist();
                    await this.list.waitForLoading();
                });
            },
        },
        importSuite('Filters/__pager', {
            params: {
                expectedPage: 3,
                initialItemsCount: 10,
                expectedItemsCount: 5,
            },
            pageObjects: {
                pager() {
                    return this.createPageObject('PagerB2b');
                },
            },
        }),
    ),
});
