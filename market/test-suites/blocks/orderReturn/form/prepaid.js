import {
    makeSuite,
    makeCase,
} from 'ginny';

import {
    selectOutletAndGoToNextStep,
    checkReturnItemsFormNextStepButton,
    fillReturnFormAndGoToMapStep,
} from '@self/root/src/spec/hermione/scenarios/returns';
import {pickPointPostamat} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';


const MONEY_TEXTS = {
    FOR_PRODUCT_AND_RETURN: 'Возврат денег за товар и пересылку',
    FOR_RETURN: 'Возврат денег за пересылку',
    FOR_PRODUCT: 'Возврат денег за товар',
    FOR_PRODUCT_DESCRIPTION: 'Деньги придут на вашу карту в течение 10 дней после возвращения товара на склад. Точный срок зависит от вашего банка.',
    FOR_PRODUCT_AND_RETURN_DESCRIPTION_1: 'Укажите реквизиты счёта, на который отправить деньги.',
    FOR_PRODUCT_AND_RETURN_DESCRIPTION_2:
        'Они придут в течение 10 дней после возвращения товара на склад. Точный срок зависит от вашего банка.',
};

/**
 * Тесты на взаимодействие компонентов формы возврата для предоплатного заказа.
 * @todo переписать на jest после переезда на React
 */

export default makeSuite('Предоплатный заказ.', {
    defaultParams: {
        paymentType: 'PREPAID',
    },
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'кнопка "Продолжить" отображается': makeCase({
                id: 'bluemarket-2590',
                async test() {
                    return this.browser.yaScenario(this, checkReturnItemsFormNextStepButton);
                },
            }),

            'на шаге "Возврат денег"': {
                async beforeEach() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                },

                'при выбранном ПВЗ PickPoint': {
                    async beforeEach() {
                        await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {outlet: pickPointPostamat});
                    },

                    'отображается текст о возврате денег за товар и за пересылку': makeCase({
                        id: 'bluemarket-417',
                        async test() {
                            const texts = await this.returnsMoney.getMoneyReturnTexts();

                            await this.expect(texts[0]).to.equal(
                                MONEY_TEXTS.FOR_PRODUCT,
                                'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                            );

                            await this.expect(texts[1]).to.equal(
                                MONEY_TEXTS.FOR_PRODUCT_DESCRIPTION,
                                'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                            );
                            await this.expect(texts[2]).to.equal(
                                MONEY_TEXTS.FOR_RETURN,
                                'Блок "Возврат денег" должен содержать текст о возврате денег за пересылку'
                            );
                            await this.expect(texts[3]).to.equal(
                                MONEY_TEXTS.FOR_PRODUCT_AND_RETURN_DESCRIPTION_1,
                                'Блок "Возврат денег" должен содержать текст о возврате денег за пересылку'
                            );
                        },
                    }),

                    'форма реквизитов отображается': makeCase({
                        id: 'bluemarket-2590',
                        async test() {
                            return this.returnsBankAccountForm.isVisible()
                                .should.eventually.to.be.equal(true, 'Форма реквизитов должна отображаться');
                        },
                    }),
                },
            },
        },

        'При выборе причины "Товар не подошел"': {
            'кнопка "Продолжить" отображается': makeCase({
                id: 'bluemarket-2589',
                async test() {
                    return this.browser.yaScenario(this, checkReturnItemsFormNextStepButton);
                },
            }),

            'на шаге "Возврат денег"': {
                async beforeEach() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {
                        returnReasonOption: 'DO_NOT_FIT',
                    });
                },

                'при выбранном ПВЗ PickPoint': {
                    async beforeEach() {
                        await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {outlet: pickPointPostamat});
                    },

                    'блок "Возврат денег" содержит текст о возврате денег за товар на карту':
                        makeCase({
                            id: 'bluemarket-2589',
                            async test() {
                                const texts = await this.returnsMoney.getMoneyReturnTexts();

                                await this.expect(texts[0]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                                );

                                await this.expect(texts[1]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT_DESCRIPTION,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                                );
                            },
                        }),

                    'форма реквизитов не отображается': makeCase({
                        id: 'bluemarket-2589',
                        async test() {
                            return this.returnsBankAccountForm.isVisible()
                                .should.eventually.to.be.equal(false, 'Форма реквизитов не должна отображаться');
                        },
                    }),
                },
            },
        },

        'При выборе причины "Есть недостатки"': {
            beforeEach() {
                return this.reasonTypeSelector.setReasonBadQuality();
            },

            'кнопка "Продолжить" отображается': makeCase({
                id: 'bluemarket-2590',
                async test() {
                    return this.browser.yaScenario(this, checkReturnItemsFormNextStepButton);
                },
            }),

            'на шаге "Возврат денег"': {
                async beforeEach() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                },

                'при выбранном ПВЗ PickPoint': {
                    async beforeEach() {
                        await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {outlet: pickPointPostamat});
                    },

                    'блок "Возврат денег" содержит текст о возврате денег за товар на карту и за пересылку по реквизитам':
                        makeCase({
                            id: 'bluemarket-2590',
                            async test() {
                                const texts = await this.returnsMoney.getMoneyReturnTexts();

                                await this.expect(texts[0]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                                );

                                await this.expect(texts[1]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT_DESCRIPTION,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                                );
                                await this.expect(texts[2]).to.equal(
                                    MONEY_TEXTS.FOR_RETURN,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за пересылку'
                                );
                                await this.expect(texts[3]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT_AND_RETURN_DESCRIPTION_1,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за пересылку'
                                );
                            },
                        }),

                    'форма реквизитов отображается': makeCase({
                        id: 'bluemarket-2590',
                        async test() {
                            return this.returnsBankAccountForm.isVisible()
                                .should.eventually.to.be.equal(true, 'Форма реквизитов должна отображаться');
                        },
                    }),
                },
            },
        },

        'При выборе причины "Привезли не то"': {
            beforeEach() {
                return this.reasonTypeSelector.setReasonWrongItem();
            },

            'кнопка "Продолжить" отображается': makeCase({
                id: 'bluemarket-2590',
                async test() {
                    return this.browser.yaScenario(this, checkReturnItemsFormNextStepButton);
                },
            }),

            'на шаге "Возврат денег"': {
                async beforeEach() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                },

                'при выбранном ПВЗ PickPoint': {
                    async beforeEach() {
                        await this.browser.yaScenario(this, selectOutletAndGoToNextStep, {outlet: pickPointPostamat});
                    },

                    'блок "Возврат денег" содержит текст о возврате денег за товар на карту и за пересылку по реквизитам':
                        makeCase({
                            id: 'bluemarket-2590',
                            async test() {
                                const texts = await this.returnsMoney.getMoneyReturnTexts();

                                await this.expect(texts[0]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                                );

                                await this.expect(texts[1]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT_DESCRIPTION,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за товар'
                                );
                                await this.expect(texts[2]).to.equal(
                                    MONEY_TEXTS.FOR_RETURN,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за пересылку'
                                );
                                await this.expect(texts[3]).to.equal(
                                    MONEY_TEXTS.FOR_PRODUCT_AND_RETURN_DESCRIPTION_1,
                                    'Блок "Возврат денег" должен содержать текст о возврате денег за пересылку'
                                );
                            },
                        }),

                    'форма реквизитов отображается': makeCase({
                        id: 'bluemarket-2590',
                        async test() {
                            return this.returnsBankAccountForm.isVisible()
                                .should.eventually.to.be.equal(true, 'Форма реквизитов должна отображаться');
                        },
                    }),
                },
            },
        },
    },
});
