/**
 * TODO(leonidlebedev): Необходимо переписать на юнит-тесты
 */

import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на блок n-w-shop-jur-info.
 * @param {PageObject.shopJurInfo} shopJurInfo
 * @param {Number} params.juridicalAddress - Адрес магазина.
 */
export default makeSuite('Блок информации о магазине.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'информация должна соответствовать информации полученной из ручки shop-info': makeCase({
                params: {
                    juridicalAddress: 'Адрес магазина',
                },
                test() {
                    return this.shopJurInfo.getJurAddressText()
                        .should.eventually.to.be.equal(
                            `Адрес: ${this.params.juridicalAddress}`,
                            'Проверка юридического адреса'
                        );
                },
            }),
        },
    },
});
