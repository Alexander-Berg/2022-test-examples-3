// блок с кредитом
import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import BnplInfo from '@self/root/src/components/BnplInfo/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import {prepareCartState} from '@self/root/src/spec/hermione/scenarios/bnpl';
import {bnplPriceLabelSuite, bnplWidgetSuite} from '@self/root/src/spec/hermione/test-suites/blocks/bnpl';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import productWithBnpl from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/fixtures/productWithBnpl';

export const bnplSuite = makeSuite('Бнпл доступен.', {
    feature: '',
    environment: 'kadavr',
    params: {
        page: 'Ид страницы',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', this.params.reportState);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, this.params.routeParams);

                this.setPageObjects({
                    bnplInfo: () => this.createPageObject(BnplInfo),
                    confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                    paymentOptions: () => this.createPageObject(EditPaymentOption),
                    defaultOffer: () => this.createPageObject(ProductDefaultOffer),
                });

                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока с ДО',
                    () => this.defaultOffer.waitForVisible()
                );

                await this.browser.yaScenario(this, prepareCartState);
            },
        },
        prepareSuite(bnplWidgetSuite, {
            params: {
                reportState: productWithBnpl.state,
                routeParams: productWithBnpl.route,
            },
        }),
        prepareSuite(bnplPriceLabelSuite, {
            params: {
                reportState: productWithBnpl.state,
                routeParams: productWithBnpl.route,
            },
        })
    ),
});
