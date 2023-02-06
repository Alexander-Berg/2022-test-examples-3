'use strict';

import {makeSuite, mergeSuites, importSuite, PageObject} from 'ginny';

const ModelOpinions = PageObject.get('ModelOpinions');
const Opinion = PageObject.get('Opinion');

/**
 * Тест на блок ModelOpinions.
 * @param {PageObject.ModelOpinions} list
 */
export default makeSuite('Список отзывов.', {
    issue: 'VNDFRONT-1256',
    environment: 'kadavr',
    feature: 'Отзывы на модель',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.browser.yaWaitForPageObject(ModelOpinions);
            },
        },
        importSuite('Opinion', {
            meta: {
                environment: 'kadavr',
            },
            pageObjects: {
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('Opinion', this.list, this.list.getItemByIndex(0));
                },
            },
        }),
        importSuite('ListContainer', {
            meta: {
                id: 'vendor_auto-158',
                issue: 'VNDFRONT-1256',
                environment: 'kadavr',
                feature: 'Отзывы на модель',
            },
            params: {
                lazy: true,
            },
            pageObjects: {
                list() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    return this.createPageObject('ListContainer').setItemSelector(Opinion.root);
                },
            },
        }),
    ),
});
