// блок с кредитом
import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import BnplInfo from '@self/root/src/components/BnplInfo/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import ProductDefaultOffer from '@self/platform/components/DefaultOffer/__pageObject';
import {prepareCartState} from '@self/root/src/spec/hermione/scenarios/bnpl';
import {bnplPriceLabelSuite, bnplWidgetSuite} from '@self/root/src/spec/hermione/test-suites/blocks/bnpl';

export const bnplSuite = makeSuite('Бнпл доступен.', {
    feature: 'БНПЛ',
    environment: 'kadavr',
    params: {
        page: 'Ид страницы',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    bnplInfo: () => this.createPageObject(BnplInfo),
                    confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                    paymentOptions: () => this.createPageObject(EditPaymentOption),
                });
                await this.browser.yaScenario(this, prepareCartState);
                await this.browser.yaOpenPage('market:offer', {offerId: this.params.item.offer.wareId});
            },
        },
        prepareSuite(bnplWidgetSuite),
        prepareSuite(bnplPriceLabelSuite, {
            pageObjects: {
                defaultOffer() {
                    return this.createPageObject(ProductDefaultOffer);
                },
            },
        })
    ),
});
