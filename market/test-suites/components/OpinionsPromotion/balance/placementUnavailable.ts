'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на отсутствие тумблера изменения состояния услуги для пользователей без прав
 * @param {PageObject.OpinionsPromotionBalance} balance - блок счёта услуги "Отзывы за баллы"
 */
export default makeSuite('Блок "Счёт".', {
    feature: 'Отзывы за баллы',
    issue: 'VNDFRONT-3989',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При отсутствии прав на запись': {
            'тумблер скрыт': makeCase({
                id: 'vendor_auto-1306',
                async test() {
                    this.setPageObjects({
                        tumbler() {
                            return this.createPageObject('SwitchLevitan', this.balance);
                        },
                    });

                    await this.allure.runStep('Ожидаем появления блока "Счёт"', () => this.balance.waitForExist());

                    await this.allure.runStep('Проверяем состояние тумблера', () =>
                        this.tumbler.isExisting().should.eventually.be.equal(false, 'Тумблер скрыт'),
                    );
                },
            }),
        },
    },
});
