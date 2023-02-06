'use strict';

import {makeSuite, mergeSuites, importSuite} from 'ginny';

export default makeSuite('Фильтры.', {
    issue: 'VNDFRONT-2330',
    environment: 'testing',
    feature: 'Модерация',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                return this.allure.runStep('Ожидаем появления блока фильтров', () => this.filters.waitForExist());
            },
        },
        importSuite('ModerationFilters/statusFilters', {
            suiteName: 'Статус "Активные".',
            params: {
                checkbox: 0,
                checkboxName: 'Активные',
                status: 'активна',
            },
        }),
        importSuite('ModerationFilters/statusFilters', {
            suiteName: 'Статус "Обработанные".',
            params: {
                checkbox: 1,
                checkboxName: 'Обработанные',
                status: 'закрыта',
            },
        }),
        importSuite('ModerationFilters/textFilter'),
        importSuite('ModerationFilters/sourceRequestSearch'),
    ),
});
