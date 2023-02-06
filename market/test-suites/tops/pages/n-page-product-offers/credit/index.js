import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {
    PRODUCT_ROUTE,
    OFFER_CREDIT_PAYMENT_TEXT,
    OFFER_CREDIT_DETAILS_TEXT,
    productState as productWithCreditState,
} from '@self/platform/spec/hermione/fixtures/credit';
// suites
import CreditPriceSuite from '@self/platform/spec/hermione/test-suites/blocks/n-credit-price';
import ShopInfoCreditDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/creditDisclaimer';
// page-objects
import CreditPrice from '@self/project/src/components/CreditPrice/__pageObject';
import Tooltip from '@self/project/src/components/HintWithContent/__pageObject';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';

export default makeSuite('Модель с кредитными предложениями', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', productWithCreditState);
                return this.browser.yaOpenPage('market:product-offers', PRODUCT_ROUTE);
            },
        },
        prepareSuite(CreditPriceSuite, {
            meta: {
                id: 'marketfront-3496',
                issue: 'MARKETVERSTKA-34613',
            },
            params: {
                expectedPaymentText: OFFER_CREDIT_PAYMENT_TEXT,
                expectedDetailsText: OFFER_CREDIT_DETAILS_TEXT,
            },
            pageObjects: {
                creditPrice() {
                    return this.createPageObject(CreditPrice);
                },
                tooltip() {
                    return this.createPageObject(Tooltip);
                },
            },
        }),
        {
            'Дисклеймер.': prepareSuite(ShopInfoCreditDisclaimerSuite, {
                meta: {
                    id: 'marketfront-3699',
                    issue: 'MARKETVERSTKA-35816',
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
