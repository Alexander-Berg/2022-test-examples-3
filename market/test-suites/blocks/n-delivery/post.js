import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-delivery__text
 * @param {PageObject.Delivery} delivery
 */
export default makeSuite('Дисклеймер о Почте России.', {
    story: {
        'По умолчанию': {
            'должен присутствовать': makeCase({
                test() {
                    return this.delivery.postDelivery.isExisting().should.eventually.to.equal(
                        true, 'Дисклеймер о Почте России должен присутствовать'
                    );
                },
            }),
        },
    },
});
