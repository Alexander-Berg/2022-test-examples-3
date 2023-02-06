import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';
import assert from 'assert';

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
import {Reason} from '@self/root/src/widgets/parts/ReturnCandidate/components/Reason/__pageObject';

import enabled from './enabled';
import disabled from './disabled';

export default makeSuite('Лёгкий возврат Почтой России', {
    issue: 'MARKETFRONT-40722',
    params: {
        items: 'Товары',
        fillFormScenarioParams: 'Параметры для сценария fillReturnFormAndGoToMapStep',
    },
    defaultParams: {
        fillFormScenarioParams: {},
    },
    feature: 'Лёгкий возврат',
    story: mergeSuites(
        {
            async beforeEach() {
                assert(this.params.items, 'Param items must be defined');

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

        prepareSuite(enabled),
        prepareSuite(disabled)
    ),
});
