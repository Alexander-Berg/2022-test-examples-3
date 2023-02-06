import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {yandexPlusPerk} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/perks';

import ReturnsPage from '@self/root/src/widgets/parts/ReturnCandidate/components/View/__pageObject';
import {ReturnItems} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItems/__pageObject';
import {ReturnItemReason} from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnItemReason/__pageObject';
import {Credentials} from '@self/root/src/widgets/parts/ReturnCandidate/components/Credentials/__pageObject';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import PlacemarkMap from '@self/root/src/components/PlacemarkMap/__pageObject';
import ReturnMapOutletInfo from '@self/root/src/widgets/parts/ReturnCandidate/widgets/ReturnMapOutletInfo/__pageObject';
import {Account} from '@self/root/src/widgets/parts/ReturnCandidate/components/Account/__pageObject';
import {BankAccountForm} from '@self/root/src/components/BankAccountForm/__pageObject';
import {Submit} from '@self/root/src/widgets/parts/ReturnCandidate/components/Submit/__pageObject';
import {Final} from '@self/root/src/widgets/parts/ReturnCandidate/components/Final/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import ReturnMoneyDisclaimer from '@self/root/src/components/ReturnMoneyDisclaimer/__pageObject';
import ReturnDeliveryCompensationOptions
    from '@self/root/src/widgets/parts/ReturnCandidate/components/ReturnDeliveryCompensationOptions/__pageObject';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';

import newReturnFlowWithYandexPlusSuite from './newReturnFlowWithYandexPlus';
import newReturnFlowWithoutYandexPlusSuite from './newReturnFlowWithoutYandexPlus';
import newReturnFlowWithFreeReturnSuite from './newReturnFlowWithFreeReturn';

const ORDER_ITEM = {
    skuId: checkoutItemIds.asus.skuId,
    offerId: checkoutItemIds.asus.offerId,
    count: 1,
    id: 11111,
    supplierType: 'FIRST_PARTY',
};

export default makeSuite('Компенсация обратной доставки баллами Плюса', {
    issue: 'MARKETFRONT-45331',
    feature: 'Компенсация обратной доставки баллами Плюса',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    returnsForm: () => this.createPageObject(ReturnsPage),
                    returnItemsScreen: () => this.createPageObject(ReturnItems, {parent: this.returnsForm}),
                    reasonTypeSelector: () => this.createPageObject(ReturnItemReason, {parent: this.returnsForm}),
                    buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsForm}),
                    recipientForm: () => this.createPageObject(RecipientForm, {parent: this.returnsForm}),
                    returnsMoney: () => this.createPageObject(Account, {parent: this.returnsForm}),
                    returnMap: () => this.createPageObject(PlacemarkMap, {parent: this.returnsForm}),
                    returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo, {parent: this.returnsForm}),
                    returnMapSuggest: () => this.createPageObject(GeoSuggest),
                    bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsMoney}),
                    submitForm: () => this.createPageObject(Submit, {parent: this.returnsForm}),
                    finalScreen: () => this.createPageObject(Final, {parent: this.returnsForm}),
                    returnMoneyDisclaimer: () => this.createPageObject(ReturnMoneyDisclaimer, {
                        parent: this.returnsForm,
                    }),
                    returnDeliveryCompensationOptions: () => this.createPageObject(ReturnDeliveryCompensationOptions, {
                        parent: this.returnsForm,
                    }),
                });
            },
        },

        makeSuite('Пользователь с Яндекс.Плюс', {
            params: {
                isAuthWithPlugin: 'Авторизован ли пользователь',
                perks: 'Перки авторизованного пользователя',
            },
            defaultParams: {
                isAuthWithPlugin: true,
                perks: [
                    yandexPlusPerk,
                ],
            },
            story: mergeSuites(
                makeSuite('На карте есть точки для возврата', {
                    story: mergeSuites(
                        makeSuite('Выбран ПВЗ с платной доставкой', {
                            story: mergeSuites(
                                prepareSuite(newReturnFlowWithYandexPlusSuite, {
                                    suiteName: '1P-поставщик в заказе.',
                                    id: 'marketfront-5045',
                                    params: {
                                        items: [{
                                            ...ORDER_ITEM,
                                            supplierType: 'FIRST_PARTY',
                                        }],
                                    },
                                }),

                                prepareSuite(newReturnFlowWithYandexPlusSuite, {
                                    suiteName: '3P-поставщик в заказе.',
                                    id: 'marketfront-5050',
                                    params: {
                                        items: [{
                                            ...ORDER_ITEM,
                                            supplierType: 'THIRD_PARTY',
                                        }],
                                    },
                                })
                            ),
                        }),

                        makeSuite('Выбран ПВЗ с бесплатной доставкой', {
                            story: mergeSuites(
                                prepareSuite(newReturnFlowWithFreeReturnSuite, {
                                    suiteName: '1P-поставщик в заказе.',
                                    id: 'marketfront-5045',
                                    params: {
                                        items: [{
                                            ...ORDER_ITEM,
                                            supplierType: 'FIRST_PARTY',
                                        }],
                                    },
                                })
                            ),
                        })
                    ),
                }),

                makeSuite('Точки на карте отсутствуют', {
                    story: mergeSuites(
                        prepareSuite(newReturnFlowWithYandexPlusSuite, {
                            suiteName: '1P-поставщик в заказе.',
                            id: 'marketfront-5045',
                            params: {
                                items: [{
                                    ...ORDER_ITEM,
                                    supplierType: 'FIRST_PARTY',
                                }],
                                shouldMapStepBeShown: false,
                            },
                        }),

                        prepareSuite(newReturnFlowWithYandexPlusSuite, {
                            suiteName: 'Дропшип-заказ.',
                            id: 'marketfront-5046',
                            params: {
                                isDropship: true,
                                items: [ORDER_ITEM],
                                shouldMapStepBeShown: false,
                            },
                        })
                    ),
                })
            ),
        }),

        makeSuite('Пользователь без Яндекс.Плюса', {
            params: {
                isAuthWithPlugin: 'Авторизован ли пользователь',
                perks: 'Перки авторизованного пользователя',
            },
            defaultParams: {
                isAuthWithPlugin: true,
                perks: [],
            },
            story: mergeSuites(
                makeSuite('На карте есть точки для возврата', {
                    story: mergeSuites(
                        makeSuite('Выбран ПВЗ с платной доставкой', {
                            story: mergeSuites(
                                prepareSuite(newReturnFlowWithoutYandexPlusSuite, {
                                    suiteName: '1P-поставщик в заказе.',
                                    id: 'marketfront-5047',
                                    params: {
                                        items: [{
                                            ...ORDER_ITEM,
                                            supplierType: 'FIRST_PARTY',
                                        }],
                                    },
                                }),

                                prepareSuite(newReturnFlowWithoutYandexPlusSuite, {
                                    suiteName: '3P-поставщик в заказе.',
                                    id: 'marketfront-5051',
                                    params: {
                                        items: [{
                                            ...ORDER_ITEM,
                                            supplierType: 'THIRD_PARTY',
                                        }],
                                    },
                                })
                            ),
                        }),

                        makeSuite('Выбран ПВЗ с бесплатной доставкой', {
                            story: mergeSuites(
                                prepareSuite(newReturnFlowWithFreeReturnSuite, {
                                    suiteName: '1P-поставщик в заказе.',
                                    id: 'marketfront-5047',
                                    params: {
                                        items: [{
                                            ...ORDER_ITEM,
                                            supplierType: 'FIRST_PARTY',
                                        }],
                                    },
                                })
                            ),
                        })
                    ),
                }),

                makeSuite('Точки на карте отсутствуют', {
                    story: mergeSuites(
                        prepareSuite(newReturnFlowWithoutYandexPlusSuite, {
                            suiteName: '1P-поставщик в заказе.',
                            id: 'marketfront-5047',
                            params: {
                                items: [{
                                    ...ORDER_ITEM,
                                    supplierType: 'FIRST_PARTY',
                                }],
                                shouldMapStepBeShown: false,
                            },
                        }),

                        prepareSuite(newReturnFlowWithoutYandexPlusSuite, {
                            suiteName: 'Дропшип-заказ.',
                            id: 'marketfront-5048',
                            params: {
                                isDropship: true,
                                items: [ORDER_ITEM],
                                shouldMapStepBeShown: false,
                            },
                        })
                    ),
                })
            ),
        })
    ),
});
