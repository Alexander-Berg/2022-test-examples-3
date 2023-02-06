'use strict';

import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.RestrictPanel} panel
 */
export default makeSuite('Сообщение об ограничении бесплатных услуг.', {
    environment: 'kadavr',
    id: 'vendor_auto-512',
    issue: 'VNDFRONT-1837',
    feature: 'Управление услугами и пользователями',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При истечении срока действий прав на бренд': {
            'должно присутствовать': makeCase({
                test() {
                    return this.panel.waitForVisible().should.eventually.be.equal(true, 'Сообщение отображается');
                },
            }),
        },
    },
});
