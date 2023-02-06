import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок DefaultOfferMini.
 * @param {PageObject.DefaultOfferMini} defaultOfferMini
 */
export default makeSuite('CPA мини-дефолтный оффер.', {
    id: 'marketfront-4351',
    issue: 'MARKETFRONT-19122',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.defaultOffer
                        .isCartButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка "В корзину" отобразилась');
                },
            }),
        },
    },
});
