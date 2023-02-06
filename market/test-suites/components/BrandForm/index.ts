'use strict';

import {makeSuite, importSuite, mergeSuites} from 'ginny';

/**
 * Тесты для формы редактирования бренда
 * @param {PageObject.BrandForm} brandForm
 */

export default makeSuite('Форма бренда.', {
    issue: 'VNDFRONT-3197',
    environment: 'kadavr',
    feature: 'Настройки',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления формы бренда', () => this.brandForm.waitForExist());
            },
        },
        importSuite('BrandForm/edit'),
        importSuite('BrandForm/view'),
    ),
});
