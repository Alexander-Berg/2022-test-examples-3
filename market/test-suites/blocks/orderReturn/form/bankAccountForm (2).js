import {
    makeSuite,
    makeCase,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {fillReturnsForm} from '@self/root/src/spec/hermione/scenarios/returns';
import {fillBankAccountForm} from '@self/root/src/spec/hermione/scenarios/bankAccount';

import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';


import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';

import paymentPurpose from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/form/paymentPurpose';
import {pickPointPostamat} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';


/**
 * Форма банковских реквизитов.
 */

export default makeSuite('Форма банковских реквизитов.', {
    defaultParams: {
        // для постоплатного заказа форма реквизитов всегда показана
        paymentType: 'POSTPAID',
    },
    environment: 'kadavr',
    id: 'bluemarket-828',
    story:
    mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    returnItemsScreen: () => this.createPageObject(
                        ReturnItems,
                        {parent: this.returnsPage}
                    ),
                    reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsPage}),
                    buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsPage}),
                    recipientForm: () => this.createPageObject(RecipientForm, {parent: this.buyerInfoScreen}),
                    bankAccountScreen: () => this.createPageObject(Account, {parent: this.returnsPage}),
                    bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsPage}),
                });

                return this.browser.yaScenario(this, fillReturnsForm, {
                    itemsIndexes: [3, 5],
                    itemsCount: 5,
                    itemsReasons: [
                        {reason: 'bad_quality', text: 'hello market'},
                        {reason: 'do_not_fit', text: 'bad bad bad'},
                    ],
                    recipient: {
                        // параметры для checkout.fillRecipientForm
                        formData: returnsFormData,
                    },
                    outlet: pickPointPostamat,
                });
            },

            'По умолчанию': {
                'поля БИК, Номер счета отображаются': makeCase({
                    async test() {
                        await this.bankAccountForm.bik.isVisible()
                            .should.eventually.to.be.equal(true, 'Поле БИК должно отображаться');

                        await this.bankAccountForm.account.isVisible()
                            .should.eventually.to.be.equal(true, 'Поле Номер счета должно отображаться');
                    },
                }),

                'поля Банк, Город, Корр. счет не отображаются': makeCase({
                    async test() {
                        await this.bankAccountForm.bank.isVisible()
                            .should.eventually.to.be.equal(false, 'Поле Банк не должна отображаться');

                        await this.bankAccountForm.bankCity.isVisible()
                            .should.eventually.to.be.equal(false, 'Поле Город не должно отображаться');

                        await this.bankAccountForm.corrAccount.isVisible()
                            .should.eventually.to.be.equal(false, 'Поле Корр.счет не должно отображаться');
                    },
                }),

                'кнопка "Продолжить" отображается': makeCase({
                    async test() {
                        await this.bankAccountForm.button.isVisible()
                            .should.eventually.to.be.equal(true, 'Кнопка продолжить должна отображаться');
                    },
                }),
            },

            'После отправки формы': {
                async beforeEach() {
                    await this.browser.setState('Checkouter.collections', {
                        bankDetails: returnsFormData.bankAccount,
                    });

                    await this.browser.yaScenario(this, fillBankAccountForm, {
                        bik: returnsFormData.bankAccount.bik,
                        account: returnsFormData.bankAccount.account,
                        fullName: returnsFormData.bankAccount.fullName,
                    });
                },

                'поля Банк, Город, Корр. счет отображаются': makeCase({
                    async test() {
                        await this.bankAccountForm.bank.isVisible()
                            .should.eventually.to.be.equal(true, 'Поле Банк должо отображаться');

                        await this.bankAccountForm.bankCity.isVisible()
                            .should.eventually.to.be.equal(true, 'Поле Город должно отображаться');

                        await this.bankAccountForm.corrAccount.isVisible()
                            .should.eventually.to.be.equal(true, 'Поле Корр.счет должно отображаться');
                    },
                }),

                'загрузилась верная информация о банке': makeCase({
                    async test() {
                        await this.bankAccountForm.getBank()
                            .should.eventually.to.be.equal(
                                returnsFormData.bankAccount.bank,
                                `Поле Название банка должно иметь значение ${returnsFormData.bankAccount.bank}`
                            );

                        await this.bankAccountForm.getBankCity()
                            .should.eventually.to.be.equal(
                                returnsFormData.bankAccount.bankCity,
                                `Поле Город должно иметь значение ${returnsFormData.bankAccount.bankCity}`
                            );

                        await this.bankAccountForm.getCorrAccount()
                            .should.eventually.to.be.equal(
                                returnsFormData.bankAccount.corrAccount,
                                `Поле Корр.счет должно иметь значение ${returnsFormData.bankAccount.corrAccount}`
                            );
                    },
                }),
            },
        },

        prepareSuite(paymentPurpose, {
            pageObjects: {
                submitForm() {
                    return this.bankAccountForm;
                },
            },
        })
    ),
});
