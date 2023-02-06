import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {productWithCredit, productWithDefaultOffer, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';
// suites
import CreditDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/CreditDisclaimer';
// page-objects
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';

import {creditWidgetSuite, creditWidgetWithoutSuite} from '@self/root/src/spec/hermione/test-suites/blocks/credits';
import {prepareStateCarts} from '@self/root/src/spec/hermione/scenarios/credit';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

export default makeSuite('Кредиты.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    defaultOffer: () => this.createPageObject(DefaultOffer),
                });

                const dataMixin = {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                };

                await this.browser.setState('report', mergeState([
                    this.params.reportState,
                    dataMixin,
                ]));
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, {...phoneProductRoute, hideSeoContent: 1});
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока ДО',
                    () => this.defaultOffer.waitForVisible()
                );
                // сразу меняем стейт для корзины
                await this.browser.yaScenario(this, prepareStateCarts);
            },
        },
        prepareSuite(creditWidgetSuite, {
            params: {
                reportState: productWithCredit,
                isAuthWithPlugin: true,
            },
            meta: {id: 'bluemarket-4050'},
        }),
        prepareSuite(creditWidgetWithoutSuite, {
            params: {
                reportState: productWithDefaultOffer,
                isAuthWithPlugin: true,
            },
        }),
        prepareSuite(CreditDisclaimerSuite, {
            meta: {
                id: 'm-touch-2967',
                issue: 'MOBMARKET-13249',
            },
            params: {
                reportState: productWithCredit,
            },
        })
    ),
});
