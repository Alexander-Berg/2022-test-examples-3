'use strict';

import {makeSuite, importSuite, mergeSuites} from 'ginny';

export default makeSuite('Заявки на редактирование бренда.', {
    issue: 'VNDFRONT-2330',
    environment: 'testing',
    feature: 'Модерация',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.allure.runStep('Ожидаем появления списка заявок', () => this.list.waitForExist());
                await this.allure.runStep('Ожидаем загрузки списка', () => this.list.waitForLoading());
            },
        },
        importSuite('ModerationFilters'),
        importSuite('ListContainer', {
            meta: {
                id: 'vendor_auto-1006',
                issue: 'VNDFRONT-2375',
                feature: 'Модерация',
            },
        }),
    ),
});
