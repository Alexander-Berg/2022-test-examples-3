import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на блок OrderMinCost.
 * @param {PageObject.OrderMinCost}
 */
export default makeSuite('Блок минимальной суммы заказа.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                feature: 'Минимальная сумма заказа',
                id: 'm-touch-2400',
                issue: 'MOBMARKET-9830',
                async test() {
                    await this.orderMinCost
                        .isExisting()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что отображается блок минимальной стоимости'
                        );
                },
            }),
        },
    },
});
