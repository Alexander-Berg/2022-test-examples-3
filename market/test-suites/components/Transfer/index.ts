'use strict';

import {makeCase, makeSuite, mergeSuites, importSuite} from 'ginny';

/**
 * @param {PageObject.Transfer} transfer
 */
export default makeSuite('Перевод средств.', {
    issue: 'VNDFRONT-1888',
    feature: 'Перевод средств',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    transfer() {
                        return this.createPageObject('Transfer');
                    },
                });
            },
        },
        importSuite('Transfer/transfer'),
        {
            'При разных клиентах у всех услуг': {
                'перевод средств недоступен': makeCase({
                    environment: 'kadavr',
                    id: 'vendor_auto-558',
                    test() {
                        return this.transfer
                            .isVisible()
                            .should.eventually.be.equal(false, 'Перевод средств отсутствует');
                    },
                }),
            },
        },
    ),
});
