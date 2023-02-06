import {
    makeSuite,
    mergeSuites,
    makeCase,
    prepareSuite,
} from 'ginny';

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
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import returnsFormData from '@self/root/src/spec/hermione/configs/returns/formData';
import {
    prepareReturnStateAndOpenReturnPage,
    checkReturnMapAndPointsVisibility,
    fillReturnFormAndGoToMapStep,
} from '@self/root/src/spec/hermione/scenarios/returns';
import outletsMock, {
    pickPointPostamat,
    yandexMarketPostamat,
} from '@self/root/src/spec/hermione/kadavr-mock/returns/reportMoscowReturnOutlets';
import returnOptionsMock, {
    outletCount,
} from '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterMoscowReturnOptions';
import returnPostamatsFromCMS from '@self/root/src/spec/hermione/kadavr-mock/tarantino/mp_market_return_postomats';
import outletInfoSuite from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/map/outletInfo';
import filtersSuite from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/map/filters';
import searchSuite from '@self/project/src/spec/hermione/test-suites/blocks/orderReturn/map/search';

const ORDER_ITEM = {
    skuId: checkoutItemIds.asus.skuId,
    offerId: checkoutItemIds.asus.offerId,
    count: 2,
    id: 11111,
    supplierType: 'FIRST_PARTY',
};

const FILL_FORM_SCENARIO_PARAMS = {
    itemsIndexes: [1],
    itemsCount: ORDER_ITEM.count,
    itemsReasons: [{reason: 'bad_quality', text: 'test'}],
    recipient: {
        formData: returnsFormData,
    },
};

export default makeSuite('Карта точек возврата', {
    params: {
        items: 'Товары',
    },
    defaultParams: {
        items: [ORDER_ITEM],
    },
    feature: 'Карта точек возврата',
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
                    returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo),
                    returnMapSuggest: () => this.createPageObject(GeoSuggest),
                    bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsMoney}),
                    submitForm: () => this.createPageObject(Submit, {parent: this.returnsForm}),
                    finalScreen: () => this.createPageObject(Final, {parent: this.returnsForm}),
                    reasonsChooseScreen: () => this.createPageObject(Reason, {parent: this.returnsForm}),
                });
            },
        },

        makeSuite('1P-поставщик в заказе', {
            issue: 'MARKETFRONT-47965',
            id: 'marketfront-4665',
            defaultParams: {
                items: [{
                    ...ORDER_ITEM,
                    supplierType: 'FIRST_PARTY',
                }],
            },
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                    });

                    return this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        FILL_FORM_SCENARIO_PARAMS
                    );
                },

                'При наличии точек возврата': {
                    'карта отображается корректно': makeCase({
                        test() {
                            return this.browser.yaScenario(this, checkReturnMapAndPointsVisibility);
                        },
                    }),
                },
            },
        }),

        makeSuite('3P-поставщик в заказе', {
            issue: 'MARKETFRONT-47965',
            id: 'marketfront-4666',
            defaultParams: {
                items: [{
                    ...ORDER_ITEM,
                    supplierType: 'THIRD_PARTY',
                }],
            },
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                    });

                    return this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        FILL_FORM_SCENARIO_PARAMS
                    );
                },

                'При наличии точек возврата': {
                    'карта отображается корректно': makeCase({
                        test() {
                            return this.browser.yaScenario(this, checkReturnMapAndPointsVisibility);
                        },
                    }),
                },
            },
        }),

        prepareSuite(outletInfoSuite, {
            suiteName: 'Информация о ПВЗ с платным возвратом.',
            params: {
                outlet: pickPointPostamat,
                returnOptions: returnOptionsMock,
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
                        FILL_FORM_SCENARIO_PARAMS
                    );
                },
            },
        }),

        prepareSuite(outletInfoSuite, {
            suiteName: 'Информация о ПВЗ с бесплатным возвратом.',
            params: {
                outlet: yandexMarketPostamat,
                returnOptions: returnOptionsMock,
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
                        FILL_FORM_SCENARIO_PARAMS
                    );
                },
            },
        }),

        prepareSuite(filtersSuite, {
            params: {
                outletCount: outletCount + returnPostamatsFromCMS.outlets.length,
            },
            hooks: {
                async beforeEach() {
                    /**
                     * CMS-мок с постаматами для возврата
                     */
                    await this.browser.setState(
                        'Tarantino.data.result',
                        [returnPostamatsFromCMS]
                    );

                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                        returnOptionsMock,
                        outletsMock,
                    });

                    return this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        FILL_FORM_SCENARIO_PARAMS
                    );
                },
            },
        }),

        prepareSuite(searchSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                        items: this.params.items,
                    });

                    await this.browser.yaScenario(
                        this,
                        fillReturnFormAndGoToMapStep,
                        FILL_FORM_SCENARIO_PARAMS
                    );
                },
            },
        })
    ),
});
