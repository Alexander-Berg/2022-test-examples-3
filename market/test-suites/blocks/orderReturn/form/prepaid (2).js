import {
    makeSuite,
    makeCase,
} from 'ginny';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {pickPointPostamat} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';

import {fillReturnsForm} from '@self/root/src/spec/hermione/scenarios/returns';

import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';

const MONEY_TEXTS = {
    BANK_ACCOUNT_SCREEN: {
        FOR_PRODUCT: 'Возврат денег за товар',
        FOR_PRODUCT_DESCRIPTION: 'Деньги придут на вашу карту в течение 10 дней после возвращения товара на склад. Точный срок зависит от вашего банка.',
        FOR_RETURN: 'Возврат денег за пересылку',
        FOR_RETURN_DESCRIPTION: 'Укажите реквизиты счёта, на который отправить деньги.',
    },
};

/**
 * Тесты на взаимодействие компонентов формы возврата для предоплатного заказа.
 */

export default makeSuite('Предоплатный заказ.', {
    defaultParams: {
        paymentType: 'PREPAID',
    },
    environment: 'kadavr',
    story: {
        'Экран возврата денег.': {
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
            },

            'Указана причина возврата "Есть недостатки" хотя бы для 1го товара.': {
                beforeEach() {
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

                'Текст содержит информацию о возврате денег за товар на карту, за пересылку на реквизиты': makeCase({
                    id: 'bluemarket-824',
                    async test() {
                        const texts = await this.bankAccountScreen.getMoneyReturnTexts();

                        await this.expect(texts[0]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_PRODUCT,
                            'Блок "Куда вернуть деньги" должен содержать заголовок про возврат средств за товар'
                        );

                        await this.expect(texts[1]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_PRODUCT_DESCRIPTION,
                            'Блок "Куда вернуть деньги" должен содержать текст о возврате денег за товар'
                                + ' и сроках возврата денег'
                        );

                        await this.expect(texts[2]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_RETURN,
                            'Блок "Куда вернуть деньги" должен содержать заголовок о возврате денег за пересылку'
                        );

                        await this.expect(texts[3]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_RETURN_DESCRIPTION,
                            'Блок "Куда вернуть деньги" должен содержать текст о возврате денег за пересылку'
                        );
                    },
                }),

                'Форма банковских реквизитов отображается': makeCase({
                    id: 'bluemarket-824',
                    async test() {
                        await this.bankAccountForm.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Форма банковских реквизитов должна отображаться'
                            );
                    },
                }),
            },

            'Указана причина возврата "Привезли не то" хотя бы для 1го товара.': {
                beforeEach() {
                    return this.browser.yaScenario(this, fillReturnsForm, {
                        itemsIndexes: [3, 5],
                        itemsCount: 5,
                        itemsReasons: [
                            {reason: 'wrong_item', text: 'hello market'},
                            {reason: 'do_not_fit', text: 'bad bad bad'},
                        ],
                        recipient: {
                            // параметры для checkout.fillRecipientForm
                            formData: returnsFormData,
                        },
                        outlet: pickPointPostamat,
                    });
                },

                'Текст содержит информацию о возврате денег за товар на карту, за пересылку на реквизиты': makeCase({
                    id: 'bluemarket-826',
                    async test() {
                        const texts = await this.bankAccountScreen.getMoneyReturnTexts();

                        await this.expect(texts[0]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_PRODUCT,
                            'Блок "Куда вернуть деньги" должен содержать заголовок про возврат средств за товар'
                        );

                        await this.expect(texts[1]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_PRODUCT_DESCRIPTION,
                            'Блок "Куда вернуть деньги" должен содержать текст о возврате денег за товар'
                            + ' и сроках возврата денег'
                        );

                        await this.expect(texts[2]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_RETURN,
                            'Блок "Куда вернуть деньги" должен содержать заголовок о возврате денег за пересылку'
                        );

                        await this.expect(texts[3]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_RETURN_DESCRIPTION,
                            'Блок "Куда вернуть деньги" должен содержать текст о возврате денег за пересылку'
                        );
                    },
                }),

                'Форма банковских реквизитов отображается': makeCase({
                    id: 'bluemarket-826',
                    async test() {
                        await this.bankAccountForm.isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Форма банковских реквизитов должна отображаться'
                            );
                    },
                }),
            },

            'Указана причина возврата "Не подошел" у всех товаров.': {
                beforeEach() {
                    return this.browser.yaScenario(this, fillReturnsForm, {
                        itemsIndexes: [3, 5],
                        itemsCount: 5,
                        itemsReasons: [
                            {reason: 'do_not_fit', text: 'hello market'},
                            {reason: 'do_not_fit', text: 'bad bad bad'},
                        ],
                        recipient: {
                            // параметры для checkout.fillRecipientForm
                            formData: returnsFormData,
                        },
                        outlet: pickPointPostamat,
                    });
                },

                'Текст содержит информацию о возврате денег за товар на карту': makeCase({
                    id: 'bluemarket-825',
                    async test() {
                        const texts = await this.bankAccountScreen.getMoneyReturnTexts();

                        await this.expect(texts[0]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_PRODUCT,
                            'Блок "Куда вернуть деньги" должен содержать заголовок про возврат средств за товар'
                        );

                        await this.expect(texts[1]).to.be.equal(
                            MONEY_TEXTS.BANK_ACCOUNT_SCREEN.FOR_PRODUCT_DESCRIPTION,
                            'Блок "Куда вернуть деньги" должен содержать текст о возврате денег за товар'
                            + ' и сроках возврата денег'
                        );
                    },
                }),

                'Форма банковских реквизитов не отображается': makeCase({
                    id: 'bluemarket-825',
                    async test() {
                        await this.bankAccountForm.isVisible()
                            .should.eventually.to.be.equal(
                                false,
                                'Форма банковских реквизитов НЕ должна отображаться'
                            );
                    },
                }),
            },
        },
    },
});
