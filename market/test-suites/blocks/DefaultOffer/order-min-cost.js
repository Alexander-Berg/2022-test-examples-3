import {makeCase, makeSuite} from 'ginny';


/**
 * Тесты на блок DefaultOffer.
 * @param {PageObject.DefaultOffer}
 */
export default makeSuite('Блок минимальной суммы заказа на дефолтном оффере.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                feature: 'Минимальная сумма заказа',
                id: 'm-touch-2400',
                issue: 'MOBMARKET-9830',
                test() {
                    return this.defaultOffer
                        .orderMinCost
                        .isExisting()
                        .should.eventually.to.equal(true, 'Проверяем, что в ДО есть блок стоимости');
                },
            }),

        },
    },
});
