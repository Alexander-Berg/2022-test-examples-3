'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на переключатель режима менеджера
 * @param {PageObject.Products} products - список услуг
 * @param {PageObject.SwitchLevitan} tumbler - переключатель режима менеджера
 * @param {Object} params
 * @param {number} params.count - количество блоков услуг
 */
export default makeSuite('Режим менеджера.', {
    feature: 'Управление балансом',
    params: {
        user: 'Пользователь',
    },
    story: {
        async beforeEach() {
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления списка услуг', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.products.waitForExist(),
            );
            // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
            await this.allure.runStep('Ожидаем появления переключателя', () =>
                // @ts-expect-error(TS2339) найдено в рамках VNDFRONT-4580
                this.tumbler.waitForExist(),
            );
        },
        'При нажатии на тумблер': {
            'детали услуг скрываются': makeCase({
                id: 'vendor_auto-814',
                issue: 'VNDFRONT-3341',
                environment: 'kadavr',
                async test() {
                    await this.products.checkDetailsCount(this.params.detailsCount);

                    await this.tumbler.click();

                    await this.products.checkDetailsCount(0);
                },
            }),
        },
    },
});
