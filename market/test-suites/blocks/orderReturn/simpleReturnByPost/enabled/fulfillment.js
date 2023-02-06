import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import outletsMock, {
    postOutlet,
} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import returnOptionsMock from '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterMoscowReturnOptions';
import {
    prepareReturnStateAndOpenReturnPage,
    fillReturnFormAndGoToMapStep,
    createReturnPositiveScenario,
} from '@self/root/src/spec/hermione/scenarios/returns';

import outletInfoPriceSuite from '../outletInfoPrice';
import bankAccountFormVisibleSuite from '../bankAccountFormVisible';
import bankAccountFormHiddenSuite from '../bankAccountFormHidden';
import {fulfillmentInstructionWithSimpleReturnSuite} from '../finalInstruction';

export default makeSuite('Фулфиллмент', {
    params: {
        items: 'Товары',
        fillFormScenarioParams: 'Параметры для сценария fillReturnFormAndGoToMapStep',
    },
    story: mergeSuites(
        prepareSuite(outletInfoPriceSuite, {
            suiteName: 'Информация о почтовом отделении.',
            meta: {
                id: 'marketfront-4843',
            },
            params: {
                outlet: postOutlet,
                returnOptions: returnOptionsMock,
                isFreeReturn: true,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                        returnOptionsMock,
                        outletsMock,
                    });

                    return this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        this.params.fillFormScenarioParams
                    );
                },
            },
        }),

        prepareSuite(bankAccountFormHiddenSuite, {
            suiteName: 'Предоплатный заказ.',
            meta: {
                id: 'marketfront-4845',
            },
            params: {
                isOrderPrepaid: true,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                        returnOptionsMock,
                        outletsMock,
                        isOrderPrepaid: this.params.isOrderPrepaid,
                    });

                    return this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        this.params.fillFormScenarioParams
                    );
                },
            },
        }),

        prepareSuite(bankAccountFormVisibleSuite, {
            suiteName: 'Постоплатный заказ.',
            meta: {
                id: 'marketfront-4850',
            },
            params: {
                isOrderPrepaid: false,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                        returnOptionsMock,
                        outletsMock,
                        isOrderPrepaid: this.params.isOrderPrepaid,
                    });

                    return this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        this.params.fillFormScenarioParams
                    );
                },
            },
        }),

        prepareSuite(fulfillmentInstructionWithSimpleReturnSuite, {
            suiteName: 'Инструкция по возврату.',
            params: {
                orderId: 1,
                postAddress: postOutlet.address.fullAddress,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                        returnOptionsMock,
                        outletsMock,
                        orderId: this.params.orderId,
                    });

                    return this.browser.yaScenario(this, createReturnPositiveScenario, {
                        outlet: postOutlet,
                        ...this.params.fillFormScenarioParams,
                        shouldBankAccountFormBeShown: false,
                    });
                },
            },
        })
    ),
});
