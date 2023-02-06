import assert from 'assert';
import {
    makeSuite,
    makeCase,
} from 'ginny';

import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';

import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {PAYMENT_PURPOSE_DESCRIPTION_TEXT} from '@self/root/src/components/BankAccountForm/constants';
import {
    MAX_LENGTH,
    MAX_LENGTH_ERROR,
    NEED_INFO_ERROR,
} from '@self/root/src/utils/validators/myReturns/bankAccount/constants';

import {fillBankAccountForm} from '@self/root/src/spec/hermione/scenarios/bankAccount';

export default makeSuite('Назначение платежа.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            assert(this.submitForm, 'PageObject submitForm must be defined');

            this.setPageObjects({
                returnsForm: () => this.createPageObject(ReturnsPage),
                returnsMoney: () => this.createPageObject(Account, {parent: this.returnsForm}),
                bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsMoney}),
            });
        },

        'При незаполненном поле "Счет получателя",': {
            'поле "Назначение платежа" не отображается': makeCase({
                issue: 'BLUEMARKET-9839',
                id: 'bluemarket-2988',
                async test() {
                    return this.bankAccountForm.isPaymentPurposeVisible()
                        .should.eventually.be.equal(false, 'Поле "Назначение платежа" не должно отображаться');
                },
            }),
        },

        'Если поле "Счет получателя" начинается не с цифры "3",': {
            beforeEach() {
                return this.bankAccountForm.setAccount('10301810000006000001');
            },

            'поле "Назначение платежа" не отображается': makeCase({
                issue: 'BLUEMARKET-9839',
                id: 'bluemarket-2988',
                async test() {
                    return this.bankAccountForm.isPaymentPurposeVisible()
                        .should.eventually.be.equal(false, 'Поле "Назначение платежа" не должно отображаться');
                },
            }),
        },

        'Если поле "Счет получателя" начинается с цифры "3",': {
            beforeEach() {
                return this.bankAccountForm.setAccount('30301810000006000001');
            },

            'поле "Назначение платежа" отображается корректно': makeCase({
                issue: 'BLUEMARKET-9839',
                id: 'bluemarket-2988',
                async test() {
                    await this.browser.allure.runStep(
                        'Проверяем, что поле "Назначение платежа" отобразилось корректно',
                        async () => {
                            await this.bankAccountForm.waitForVisiblePaymentPurpose();

                            await this.bankAccountForm.getPaymentPurposeDesriptionText()
                                .should.eventually.be.equal(
                                    PAYMENT_PURPOSE_DESCRIPTION_TEXT,
                                    `Под полем "Назначение платежа" должен быть текст "${PAYMENT_PURPOSE_DESCRIPTION_TEXT}"`
                                );

                            return this.bankAccountForm.isPaymentPurposeErrorExisting()
                                .should.eventually.be.equal(false, 'Не должно быть блока с ошибкой');
                        }
                    );

                    return checkPaymentPurposeErrorOnBlur.call(this, {
                        errorText: NEED_INFO_ERROR,
                    });
                },
            }),
        },

        'Если заполнить поле "Счет получателя" цифрой "3" и затем очистить его,': {
            async beforeEach() {
                await this.bankAccountForm.setAccount('3');

                await this.bankAccountForm.isPaymentPurposeVisible()
                    .should.eventually.be.equal(true, 'Поле "Назначение платежа" должно отобразиться');

                return this.browser.allure.runStep(
                    'Очищаем поле "Номер счета"',
                    async () => {
                        await this.bankAccountForm.clickAccount();
                        await this.browser.yaKeyPress('BACKSPACE');
                    }
                );
            },

            'поле "Назначение платежа" исчезает': makeCase({
                issue: 'BLUEMARKET-9839',
                id: 'bluemarket-2988',
                test() {
                    return this.bankAccountForm.waitForHiddenPaymentPurpose()
                        .should.eventually.be.equal(true, 'Поле "Назначение платежа" должно исчезнуть');
                },
            }),
        },

        'При заполнении поля "Назначение платежа" слишком длинным текстом,': {
            async beforeEach() {
                await this.bankAccountForm.setAccount('3');

                const tooMuchTextLength = MAX_LENGTH + 1;

                await this.browser.allure.runStep(
                    `Заполняем поле "Назначение платежа" слишком длинным текстом (${tooMuchTextLength} символ)`,
                    () => (
                        this.bankAccountForm.setPaymentPurpose('a'.repeat(tooMuchTextLength))
                    )
                );
            },

            'отображается ошибка': makeCase({
                issue: 'BLUEMARKET-9839',
                id: 'bluemarket-2988',
                test() {
                    return checkPaymentPurposeErrorOnBlur.call(this, {
                        errorText: MAX_LENGTH_ERROR,
                    });
                },
            }),
        },

        'После завершения заполнения формы возврата,': {
            async beforeEach() {
                const account = '30301810000006000001';
                const paymentPurposeText = 'какой-нибудь текст';

                await this.browser.allure.runStep(
                    `Заполняем реквизиты, в счет получателя вводим ${account}`,
                    async () => {
                        await this.browser.yaScenario(this, fillBankAccountForm, {
                            account,
                            bik: returnsFormData.bankAccount.bik,
                            fullName: returnsFormData.bankAccount.fullName,
                            shouldSubmit: false,
                        });

                        return this.bankAccountForm.setPaymentPurpose(paymentPurposeText);
                    }
                );

                await this.browser.allure.runStep(
                    'Завершаем оформление возврата',
                    async () => {
                        await this.submitForm.submit();
                        await this.bankAccountForm.waitForVisibleBank();
                        await this.submitForm.submit();

                        await this.returnsForm.waitForFinalVisible();
                    }
                );

                this.yaTestData = {
                    paymentPurposeText,
                };
            },

            'в чекаутер передается корректное назначение платежа': makeCase({
                issue: 'BLUEMARKET-9839',
                id: 'bluemarket-2988',
                test() {
                    const {paymentPurposeText} = this.yaTestData;

                    return this.browser.allure.runStep(
                        'Проверяем, что в чекаутер передается корректное назначение платежа',
                        async () => {
                            /*
                                Вызов с ретраями для того, чтобы исключить случай, когда результат null
                             */
                            const result = await this.browser.yaCallFnWithRetries(
                                () => this.browser.yaGetLastKadavrLogByBackendMethod(
                                    'Checkouter',
                                    'createReturn'
                                ),
                                {retries: 3, pause: 500}
                            );

                            const paymentPurposeValue = result.request.body.bankDetails.paymentPurpose;

                            return this.expect(paymentPurposeValue)
                                .to.be.equal(
                                    paymentPurposeText,
                                    'В чекаутер передаётся назначение платежа c корректным значением ' +
                                    '(params[0].bankAccount.paymentPurpose)'
                                );
                        }
                    );
                },
            }),
        },
    },
});

function checkPaymentPurposeErrorOnBlur({errorText}) {
    return this.browser.allure.runStep(
        `Проверяем наличие ошибки "${errorText}" при расфокусе поля "Назначение платежа"`,
        async () => {
            await this.bankAccountForm.clickPaymentPurpose();
            await this.bankAccountForm.clickPaymentPurposeDesription();

            await this.bankAccountForm.isPaymentPurposeErrorExisting()
                .should.eventually.be.equal(true, 'Должен быть блок с ошибкой');

            return this.bankAccountForm.getPaymentPurposeErrorText()
                .should.eventually.be.equal(
                    errorText,
                    `Под полем "Назначение платежа" у ошибки должен быть текст "${errorText}"`
                );
        }
    );
}
