

import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import ShopInfoCreditDisclaimerSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/creditDisclaimer';

import DefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';

import {creditWidgetSuite, creditWidgetWithoutSuite} from '@self/root/src/spec/hermione/test-suites/blocks/credits';
import {prepareStateCarts} from '@self/root/src/spec/hermione/scenarios/credit';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import productWithCredit from '../fixtures/productWithCredit';
import productWithBlueSetInDO from '../fixtures/productWithBlueSetInDO';

export default makeSuite('Кредиты.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', this.params.reportState);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, this.params.routeParams);

                this.setPageObjects({
                    defaultOffer: () => this.createPageObject(DefaultOffer),
                });

                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока с ДО',
                    () => this.defaultOffer.waitForVisible()
                );

                // Готовим стейт для перехода в корзину
                await this.browser.yaScenario(this, prepareStateCarts);
            },
        },
        prepareSuite(creditWidgetSuite, {
            params: {
                reportState: productWithCredit.state,
                routeParams: productWithCredit.route,
                isAuthWithPlugin: true,
            },
            meta: {id: 'bluemarket-4050'},
        }),
        prepareSuite(creditWidgetWithoutSuite, {
            params: {
                reportState: productWithBlueSetInDO.state,
                routeParams: productWithBlueSetInDO.route,
                isAuthWithPlugin: true,
            },
        }),
        {
            'Дисклеймер.': prepareSuite(ShopInfoCreditDisclaimerSuite, {
                params: {
                    reportState: productWithCredit.state,
                    routeParams: productWithCredit.route,
                },
                meta: {
                    id: 'marketfront-3698',
                    issue: 'MARKETVERSTKA-35814',
                },
                pageObjects: {
                    shopsInfo() {
                        return this.createPageObject(LegalInfo);
                    },
                },
            }),
        }
    ),
});
