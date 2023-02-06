import {
    makeSuite,
    makeCase,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {Submit} from '@self/root/src/widgets/parts/ReturnCandidate/components/Submit/__pageObject';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {
    selectOutletAndGoToNextStep,
    fillReturnsForm,
    fillReturnFormAndGoToMapStep,
} from '@self/root/src/spec/hermione/scenarios/returns';
import paymentPurpose from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/form/paymentPurpose';
import {pickPointPostamat} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

/**
 * Форма банквоских реквизитов.
 * @todo Все кейсы, кроме проверки отправки формы должны переехать в jest после переезда на реакт
 */

export default makeSuite('Форма банковских реквизитов.', {
    defaultParams: {
        // для постоплатного заказа форма реквизитов всегда показана
        paymentType: 'POSTPAID',
        items: [{
            skuId: checkoutItemIds.asus.skuId,
            offerId: checkoutItemIds.asus.offerId,
            wareMd5: checkoutItemIds.asus.offerId,
            count: 1,
        }],
    },
    environment: 'kadavr',
    id: 'bluemarket-2588',
    story: mergeSuites(
        {
            async beforeEach() {
                return this.setPageObjects({
                    submitForm: () => this.createPageObject(Submit, {parent: this.returnsForm}),
                    bankAccountForm: () => this.returnsBankAccountForm,
                });
            },

            'По умолчанию': {
                async beforeEach() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                    await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {outlet: pickPointPostamat});
                },

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
                    id: 'bluemarket-2590',
                    async test() {
                        return this.submitForm.getButtonText()
                            .should.eventually.to.be.equal('Продолжить', 'Кнопка должна содержать текст "Продолжить"');
                    },
                }),
            },

            'После отправки формы': {
                async beforeEach() {
                    await this.browser.setState('Checkouter.collections', {
                        bankDetails: returnsFormData.bankAccount,
                    });

                    await this.browser.yaScenario(this, fillReturnsForm, {
                        item: this.params.items[0],
                        reasonText: 'test',
                        recipient: {
                            formData: returnsFormData,
                        },
                        bankAccount: returnsFormData.bankAccount,
                        outlet: pickPointPostamat,
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

                'загрузилась верная инфомрация о банке': makeCase({
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
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                    await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {outlet: pickPointPostamat});
                },
            },
        })
    ),
});
