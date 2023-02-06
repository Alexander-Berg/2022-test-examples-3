import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок amount-select.
 * @param {PageObject.AmountSelect} amountSelect
 * @param {number} [params.step=1]
 * @param {number} [params.minAmount=1]
 * @param {number} [params.maxAmount=999]
 */
export default makeSuite('Счетчик.', {
    defaultParams: {
        step: 1,
        minAmount: 1,
        maxAmount: 999,
    },
    story: {
        'Кнопка "плюс".': {
            'При клике': {
                'должна увеличивать количество': makeCase({
                    params: {
                        step: 'Шаг',
                    },
                    test() {
                        const {amountSelect} = this;
                        const {step} = this.params;

                        return amountSelect
                            .getValue()
                            .then(count => amountSelect
                                .plusFromButton()
                                .then(() => amountSelect.getValue())
                                .should.eventually.to.be.equal(
                                    String(Number(count) + step),
                                    `Значение должно было увеличиться на ${step}`
                                ));
                    },
                }),

                'должна блокироваться при установке максимального значения': makeCase({
                    params: {
                        maxAmount: 'Максимальное значение',
                    },
                    test() {
                        const {amountSelect} = this;

                        return amountSelect
                            .setValue(this.params.maxAmount)
                            .then(() => amountSelect.minusFromButton())
                            .then(() => amountSelect.plusFromButton())
                            .then(() => amountSelect.buttonPlus.isEnabled())
                            .should.eventually.to.be.equal(false, 'Кнопка должна быть заблокирована');
                    },
                }),
            },
        },

        'Кнопка "минус".': {
            'При клике': {
                'должна уменьшать значение': makeCase({
                    params: {
                        step: 'Шаг',
                    },
                    test() {
                        const {amountSelect} = this;
                        const {step} = this.params;

                        return amountSelect
                            .setValue(step * 2)
                            .then(() => amountSelect.minusFromButton())
                            .then(() => amountSelect.getValue())
                            .should.eventually.to.be.equal(String(step), `Значение должно было уменьшиться на ${step}`);
                    },
                }),

                'должна блокироваться при установке минимального значения': makeCase({
                    params: {
                        minAmount: 'Минимальное значение',
                    },
                    test() {
                        const {amountSelect} = this;

                        return amountSelect
                            .setValue(this.params.minAmount)
                            .then(() => amountSelect.plusFromButton())
                            .then(() => amountSelect.minusFromButton())
                            .then(() => amountSelect.buttonMinus.isEnabled())
                            .should.eventually.to.be.equal(false, 'Кнопка должна быть заблокирована');
                    },
                }),
            },
        },

        'Инпут.': {
            'При вводе': {
                'невалидного значения должен устанавливать минимальное': makeCase({
                    params: {
                        minAmount: 'Минимальное значение',
                    },
                    test() {
                        const {amountSelect} = this;
                        const minAmount = String(this.params.minAmount);

                        return amountSelect
                            .setValue('текст')
                            .then(() => amountSelect.getValue())
                            .should.eventually.to.be.equal(minAmount, `Значение должно было сброситься в ${minAmount}`);
                    },
                }),
            },
        },
    },
});
