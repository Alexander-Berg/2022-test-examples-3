import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на отсутствие ссылки "Информация о продавцах" виджета CreditDisclaimer.
 * @property {PageObject.CreditDisclaimer} creditDisclaimer
 */
export default makeSuite('Блок с информацией о продавце.', {
    feature: 'Кредиты на Маркете',
    meta: {
        id: 'm-touch-3328',
        issue: 'MARKETFRONT-11097',
    },
    story: {
        'Ссылка о продавце': {
            'для товара не в продаже': {
                'должна отсутствовать на странице': makeCase({
                    test() {
                        return this.creditDisclaimer.isLinkExists(true)
                            .should.eventually.to.be.equal(false, 'Ссылка отсутствует на странице');
                    },

                }),
            },
        },
    },
});
