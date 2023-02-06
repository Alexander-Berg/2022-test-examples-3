'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отсутствие колокольчика
 * @param {PageObject.Bell} bell
 */
export default makeSuite('Колокольчик.', {
    id: 'vendor_auto-780',
    issue: 'VNDFRONT-2386',
    feature: 'Уведомления',
    environment: 'testing',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При отсутствии вендора': {
            недоступен: makeCase({
                test() {
                    return this.bell.isVisible().should.eventually.be.equal(false, 'Колокольчик отсутствует');
                },
            }),
        },
    },
});
