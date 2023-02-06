import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {DEFAULT_OFFER_WARE_ID, defaultOfferWithCredit, defaultOffer} from '@self/platform/spec/hermione/fixtures/product';
// suites
import CreditDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/CreditDisclaimer';
// page-objects
import OfferSummary from '@self/platform/spec/page-objects/widgets/parts/OfferSummary';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

import {creditWidgetSuite, creditWidgetWithoutSuite} from '@self/root/src/spec/hermione/test-suites/blocks/credits';
import {prepareStateCarts} from '@self/root/src/spec/hermione/scenarios/credit';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Кредиты.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', this.params.reportState);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.OFFER, {offerId: DEFAULT_OFFER_WARE_ID, hideSeoContent: 1});
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                this.setPageObjects({
                    offerSummary: () => this.createPageObject(OfferSummary),
                });
                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока с главной инофрмацией об оффере',
                    () => this.offerSummary.waitForVisible()
                );
                // сразу меняем стейт для корзины
                await this.browser.yaScenario(this, prepareStateCarts);
            },
        },
        prepareSuite(creditWidgetSuite, {
            params: {
                reportState: defaultOfferWithCredit,
                isAuthWithPlugin: true,
            },
            meta: {id: 'bluemarket-4050'},
        }),
        prepareSuite(creditWidgetWithoutSuite, {
            params: {
                reportState: defaultOffer,
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(CreditDisclaimerSuite, {
            meta: {
                id: 'm-touch-2967',
                issue: 'MOBMARKET-13249',
            },
            params: {
                reportState: defaultOfferWithCredit,
            },
        })
    ),
});
