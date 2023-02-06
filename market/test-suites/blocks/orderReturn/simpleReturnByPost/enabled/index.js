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
    createReturnPositiveScenario,
} from '@self/root/src/spec/hermione/scenarios/returns';
import {SHOP_RETURN_CONTACTS} from '@self/root/src/spec/hermione/kadavr-mock/returns/shopReturnContacts';
import {enabled} from '@self/root/src/spec/hermione/kadavr-mock/tarantino/mp_simple_return_by_post_toggle';

import {dropshipInstructionWithSimpleReturnSuite} from '../finalInstruction';

import fulfillmentSuite from './fulfillment';

export default makeSuite('Функционал лёгкого возврата включён в CMS', {
    params: {
        items: 'Товары',
        fillFormScenarioParams: 'Параметры для сценария fillReturnFormAndGoToMapStep',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                /**
                 * CMS-мок с доступностью лёгкого возврата
                 */
                await this.browser.setState(
                    'Tarantino.data.result',
                    [enabled]
                );
            },
        },

        prepareSuite(fulfillmentSuite),

        makeSuite('Дропшип', {
            story: mergeSuites(
                prepareSuite(dropshipInstructionWithSimpleReturnSuite, {
                    suiteName: 'Инструкция по возврату.',
                    params: {
                        orderId: 1,
                        postAddress: postOutlet.address.fullAddress,
                        returnContacts: [
                            SHOP_RETURN_CONTACTS.PERSON,
                            SHOP_RETURN_CONTACTS.POST,
                            SHOP_RETURN_CONTACTS.CARRIER,
                            SHOP_RETURN_CONTACTS.SELF,
                        ],
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                                items: this.params.items,
                                returnOptionsMock,
                                outletsMock,
                                orderId: this.params.orderId,
                                isDropship: true,
                                returnContacts: this.params.returnContacts,
                            });

                            return this.browser.yaScenario(this, createReturnPositiveScenario, {
                                outlet: postOutlet,
                                shouldBankAccountFormBeShown: false,
                                ...this.params.fillFormScenarioParams,
                            });
                        },
                    },
                })
            ),
        }),

        makeSuite('ДСБС', {
            story: mergeSuites(
                prepareSuite(dropshipInstructionWithSimpleReturnSuite, {
                    suiteName: 'Инструкция по возврату.',
                    params: {
                        orderId: 1,
                        postAddress: postOutlet.address.fullAddress,
                        returnContacts: [
                            SHOP_RETURN_CONTACTS.PERSON,
                            SHOP_RETURN_CONTACTS.POST,
                            SHOP_RETURN_CONTACTS.CARRIER,
                            SHOP_RETURN_CONTACTS.SELF,
                        ],
                        isDsbs: true,
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                                items: this.params.items,
                                returnOptionsMock,
                                outletsMock,
                                orderId: this.params.orderId,
                                isDsbs: this.params.isDsbs,
                                returnContacts: this.params.returnContacts,
                            });

                            return this.browser.yaScenario(this, createReturnPositiveScenario, {
                                outlet: postOutlet,
                                shouldBankAccountFormBeShown: false,
                                ...this.params.fillFormScenarioParams,
                            });
                        },
                    },
                })
            ),
        })
    ),
});
