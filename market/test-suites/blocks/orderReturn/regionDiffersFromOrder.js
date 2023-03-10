import {
    makeSuite,
    makeCase,
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

import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import returnOptionsSaintPetersburgMock from
    '@self/root/src/spec/hermione/kadavr-mock/returns/checkouterSaintPetersburgReturnOptions';
import returnOutletsSaintPetersburgMock from
    '@self/root/src/spec/hermione/kadavr-mock/returns/reportSaintPetersburgReturnOutlets';
import {
    prepareReturnStateAndOpenReturnPage,
    checkReturnMapAndPointsVisibility,
    checkReturnOptionsRequestRegion,
    fillReturnFormAndGoToMapStep,
} from '@self/root/src/spec/hermione/scenarios/returns';

import {REGION_IDS} from '@self/root/src/constants/region';

const ORDER_ITEM = {
    skuId: checkoutItemIds.asus.skuId,
    offerId: checkoutItemIds.asus.offerId,
    count: 1,
    id: 11111,
    supplierType: 'FIRST_PARTY',
};

export default makeSuite('Регион отличается от региона в заказе', {
    issue: 'MARKETFRONT-47965',
    id: 'marketfront-4668',
    params: {
        items: 'Товары',
        currentUserRegion: 'Текущий регион пользователя',
    },
    defaultParams: {
        items: [ORDER_ITEM],
        currentUserRegion: REGION_IDS.SAINT_PETERSBURG,
    },
    feature: 'Карта точек возврата',
    story: {
        async beforeEach() {
            await this.setPageObjects({
                returnsForm: () => this.createPageObject(ReturnsPage),
                returnItemsScreen: () => this.createPageObject(ReturnItems, {parent: this.returnsForm}),
                reasonTypeSelector: () => this.createPageObject(ReturnItemReason, {parent: this.returnsForm}),
                buyerInfoScreen: () => this.createPageObject(Credentials, {parent: this.returnsForm}),
                recipientForm: () => this.createPageObject(RecipientForm, {parent: this.returnsForm}),
                returnsMoney: () => this.createPageObject(Account, {parent: this.returnsForm}),
                returnMap: () => this.createPageObject(PlacemarkMap, {parent: this.returnsForm}),
                returnMapOutletInfo: () => this.createPageObject(ReturnMapOutletInfo, {parent: this.returnsForm}),
                bankAccountForm: () => this.createPageObject(BankAccountForm, {parent: this.returnsMoney}),
                submitForm: () => this.createPageObject(Submit, {parent: this.returnsForm}),
                finalScreen: () => this.createPageObject(Final, {parent: this.returnsForm}),
            });

            return this.browser.yaScenario(this, prepareReturnStateAndOpenReturnPage, {
                items: this.params.items,
                returnOptionsMock: returnOptionsSaintPetersburgMock,
                outletsMock: returnOutletsSaintPetersburgMock,
                currentUserRegion: this.params.currentUserRegion,
            });
        },

        'Запрос в чекаутер за точками возврата содержит корректный регион': makeCase({
            async test() {
                await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                return this.browser.yaScenario(this, checkReturnOptionsRequestRegion, {
                    region: this.params.currentUserRegion,
                });
            },
        }),

        'При наличии точек возврата': {
            'карта отображается корректно': makeCase({
                async test() {
                    await this.browser.yaScenario(this, fillReturnFormAndGoToMapStep, {});
                    await this.browser.yaScenario(this, checkReturnMapAndPointsVisibility);
                },
            }),
        },
    },
});
