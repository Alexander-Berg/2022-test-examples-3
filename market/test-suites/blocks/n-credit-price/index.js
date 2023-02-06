import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-credit-price.
 * @property {PageObject.CreditPrice} creditPrice
 */
export default makeSuite('Стоимость выплаты по кредиту.', {
    feature: 'Кредиты на Маркете',
    params: {
        expectedPaymentText: 'Ожидаемый текст выплаты по кредиту, пример: от 1000 ₽/мес',
        expectedDetailsText: 'Ожидаемый текст условий покупки в кредит, пример: до 9 месяцев, от 16%',
    },
    story: {
        'По умолчанию': {
            'должна присутствовать.': makeCase({
                test() {
                    const {expectedPaymentText} = this.params;
                    return this.creditPrice.getText()
                        .should.eventually.to.be.equal(expectedPaymentText,
                            `Стоимость выплаты по кредиту соответствует '${expectedPaymentText}'.`);
                },
            }),
        },
    },
});
