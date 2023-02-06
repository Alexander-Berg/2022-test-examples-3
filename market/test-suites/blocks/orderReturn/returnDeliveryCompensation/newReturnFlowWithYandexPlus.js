import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {pickPointPostamat} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {
    prepareReturnStateAndOpenReturnPage,
    fillReturnFormUntilBankAccountStep,
} from '@self/root/src/spec/hermione/scenarios/returns';

import deliveryCompensationOptionsShownSuite from './deliveryCompensationOptionsShown';
import noDeliveryCompensationSuite from './noDeliveryCompensation';

export default makeSuite('Новый флоу возврата. Пользователь с Яндекс.Плюс', {
    params: {
        items: 'Товары',
        outlet: 'ПВЗ, который нужно выбрать на карте',
        shouldMapStepBeShown: 'Должен ли отображаться шаг с картой',
    },
    defaultParams: {
        outlet: pickPointPostamat,
        shouldMapStepBeShown: true,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                let prepareReturnStateAndOpenReturnPageScenarioParams = {
                    items: this.params.items,
                };

                if (!this.params.shouldMapStepBeShown) {
                    prepareReturnStateAndOpenReturnPageScenarioParams.returnOptionsMock = {deliveryOptions: []};
                    prepareReturnStateAndOpenReturnPageScenarioParams.outletsMock = [];
                }

                await this.browser.yaScenario(
                    this,
                    prepareReturnStateAndOpenReturnPage,
                    prepareReturnStateAndOpenReturnPageScenarioParams
                );
            },
        },

        prepareSuite(deliveryCompensationOptionsShownSuite, {
            suiteName: 'Выбрана причина возврата "Есть недостатки".',
            hooks: {
                async beforeEach() {
                    await this.reasonTypeSelector.setReasonBadQuality();
                    return this.browser.yaScenario(this, fillReturnFormUntilBankAccountStep, {
                        shouldMapStepBeShown: this.params.shouldMapStepBeShown,
                        outlet: this.params.outlet,
                    });
                },
            },
        }),

        prepareSuite(noDeliveryCompensationSuite, {
            suiteName: 'Выбрана причина возврата "Не подошёл".',
            hooks: {
                async beforeEach() {
                    return this.browser.yaScenario(this, fillReturnFormUntilBankAccountStep, {
                        shouldMapStepBeShown: this.params.shouldMapStepBeShown,
                        outlet: this.params.outlet,
                        returnReasonOption: 'DO_NOT_FIT',
                    });
                },
            },
        }),

        prepareSuite(deliveryCompensationOptionsShownSuite, {
            suiteName: 'Выбрана причина возврата "Привезли не то".',
            hooks: {
                async beforeEach() {
                    return this.browser.yaScenario(this, fillReturnFormUntilBankAccountStep, {
                        shouldMapStepBeShown: this.params.shouldMapStepBeShown,
                        outlet: this.params.outlet,
                        returnReasonOption: 'WRONG_ITEM',
                    });
                },
            },
        })
    ),
});
