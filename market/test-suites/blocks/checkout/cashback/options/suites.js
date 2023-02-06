import {makeCase} from 'ginny';

import {CASHBACK_PROFILE_TYPES} from '@self/root/src/entities/cashbackProfile';

export const makeDefaultCasesForCheckoutCashbackOptions = ({
    // Номера сьютов из пальм
    id,
    isAllowEmit,
    isAllowSpend,
    cashbackAmount,
    selectedCashbackOption,
}) => {
    const resultCases = {};

    const needToShowCashbackOptions = isAllowEmit || isAllowSpend;

    // Если не доступно ни списание ни начисление, проверям только отсутствие блока
    if (!needToShowCashbackOptions) {
        resultCases['блок со списанием и начислением не отображается.'] = makeCase({
            id,
            async test() {
                await this.cashbackControl.isVisible()
                    .should.eventually.to.be.equal(
                        false,
                        'Блок с выбором списания и начисления не должен отображаться'
                    );
            },
        });

        return resultCases;
    }

    resultCases['блок со списанием и начислением отображается.'] = makeCase({
        id,
        async test() {
            await this.cashbackControl.isVisible()
                .should.eventually.to.be.equal(
                    true,
                    'Блок с выбором списания и начисления должен отображаться'
                );
        },
    });

    resultCases[`опция начисления ${isAllowEmit ? '' : 'не'} доступна.`] = makeCase({
        id,
        async test() {
            await this.cashbackOptionSelect.getIsDiabledEmitOption()
                .should.eventually.to.be.equal(
                    // т.к. проверяем наличие аттрибута disabled
                    !isAllowEmit,
                    `Опция начисления должна быть ${isAllowEmit ? '' : 'не'} доступна`
                );
        },
    });

    resultCases[`опция списания ${isAllowSpend ? '' : 'не'} доступна.`] = makeCase({
        id,
        async test() {
            await this.cashbackOptionSelect.getIsDiabledSpendOption()
                .should.eventually.to.be.equal(
                    // т.к. проверяем наличие аттрибута disabled
                    !isAllowSpend,
                    `Опция списания должна быть ${isAllowSpend ? '' : 'не'} доступна`
                );
        },
    });

    // Если выбрана опция начисления проверяем в саммари информацию "Вернется на Плюс"
    if (selectedCashbackOption === CASHBACK_PROFILE_TYPES.EMIT) {
        resultCases['поле "Вернется на Плюс" в саммари содержит корректную информацию.'] = makeCase({
            id,
            async test() {
                await this.cashbackSummaryInfo.isVisible()
                    .should.eventually.to.be.equal(
                        needToShowCashbackOptions,
                        'В саммари должна отображаться информация о начисляемом кешбэке'
                    );

                await this.cashbackSummaryInfo.getCashbackText()
                    .should.eventually.to.be.equal(
                        String(cashbackAmount),
                        `Количество начисляемого кешбэка за заказ должны быть равно ${cashbackAmount}`
                    );
            },
        });
    }

    // Если выбрана опция списания, проверяем информацию со скидками
    if (selectedCashbackOption === CASHBACK_PROFILE_TYPES.SPEND) {
        resultCases['поле "Скидка баллами Плюса" в саммари содержит корректную информацию.'] = makeCase({
            id,
            async test() {
                await this.cashbackSpendTotal.isVisible()
                    .should.eventually.to.be.equal(
                        needToShowCashbackOptions,
                        'В саммари должна отображаться информация о списываемом кешбэке'
                    );

                await this.cashbackSpendTotal.getValue()
                    .should.eventually.to.be.equal(
                        String(cashbackAmount),
                        `Количество списываемого кешбэка за заказ должны быть равно ${cashbackAmount}`
                    );
            },
        });
    }

    return resultCases;
};
