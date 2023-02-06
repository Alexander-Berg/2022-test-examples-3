'use strict';

import {makeSuite, mergeSuites, importSuite, PageObject} from 'ginny';

const ModelQuestions = PageObject.get('ModelQuestions');

/**
 * Тест на блок ModelQuestions.
 */
export default makeSuite('Список вопросов.', {
    issue: 'VNDFRONT-1754',
    environment: 'kadavr',
    feature: 'Вопросы и ответы',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                await this.browser.yaWaitForPageObject(ModelQuestions);
            },
        },
        importSuite('Question', {
            pageObjects: {
                item() {
                    // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                    const modelQuestions = this.modelQuestions;

                    return this.createPageObject('Question', modelQuestions, modelQuestions.getItemByIndex(0));
                },
            },
        }),
        importSuite('ListContainer', {
            meta: {
                id: 'vendor_auto-418',
                issue: 'VNDFRONT-1754',
                environment: 'kadavr',
            },
        }),
        importSuite('ListContainer/__empty', {
            meta: {
                id: 'vendor_auto-419',
                issue: 'VNDFRONT-1754',
                environment: 'kadavr',
            },
        }),
    ),
});
