import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {yandexMarketPickupPoint} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import {
    prepareReturnStateAndOpenReturnPage,
    fillReturnFormUntilBankAccountStep,
} from '@self/root/src/spec/hermione/scenarios/returns';

import noDeliveryCompensationSuite from './noDeliveryCompensation';

export default makeSuite('Новый флоу возврата. Выбран ПВЗ с бесплатным возвратом', {
    params: {
        items: 'Товары',
        outlet: 'ПВЗ, который нужно выбрать на карте',
    },
    defaultParams: {
        outlet: yandexMarketPickupPoint,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                    items: this.params.items,
                });
            },
        },

        prepareSuite(noDeliveryCompensationSuite, {
            suiteName: 'Выбрана причина возврата "Есть недостатки".',
            hooks: {
                async beforeEach() {
                    await this.reasonTypeSelector.setReasonBadQuality();
                    return this.browser.yaScenario(this, fillReturnFormUntilBankAccountStep, {
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
                        outlet: this.params.outlet,
                        returnReasonOption: 'DO_NOT_FIT',
                    });
                },
            },
        }),

        prepareSuite(noDeliveryCompensationSuite, {
            suiteName: 'Выбрана причина возврата "Привезли не то".',
            hooks: {
                async beforeEach() {
                    return this.browser.yaScenario(this, fillReturnFormUntilBankAccountStep, {
                        outlet: this.params.outlet,
                        returnReasonOption: 'WRONG_ITEM',
                    });
                },
            },
        })
    ),
});
