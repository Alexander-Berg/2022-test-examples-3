import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок DefaultOfferMini.
 * @param {PageObject.DefaultOfferMini} defaultOfferMini
 */
export default makeSuite('CPC мини-дефолтный оффер.', {
    id: 'marketfront-4352',
    issue: 'MARKETFRONT-19122',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.defaultOffer
                        .isClickOutButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка "В магазин" отобразилась');
                },
            }),
        },
    },
});
