import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import BnplInfo from '@self/root/src/components/BnplInfo/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import DefaultOffer from '@self/platform/widgets/parts/OfferSummary/__pageObject';
import {prepareCartState} from '@self/root/src/spec/hermione/scenarios/bnpl';
import {bnplPriceLabelSuite, bnplWidgetSuite} from '@self/root/src/spec/hermione/test-suites/blocks/bnpl';
import {DEFAULT_OFFER_WARE_ID, defaultOfferWithBnpl} from '@self/platform/spec/hermione/fixtures/product';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import OfferSummary from '@self/platform/spec/page-objects/widgets/parts/OfferSummary';

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
                await this.browser.setState('report', this.params.reportState);
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.OFFER, {offerId: DEFAULT_OFFER_WARE_ID});
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                this.setPageObjects({
                    offerSummary: () => this.createPageObject(OfferSummary),
                    bnplInfo: () => this.createPageObject(BnplInfo),
                    confirmationPage: () => this.createPageObject(CheckoutLayoutConfirmation),
                    paymentOptions: () => this.createPageObject(EditPaymentOption),
                });
                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока с главной инофрмацией об оффере',
                    () => this.offerSummary.waitForVisible()
                );
                // сразу меняем стейт для корзины
                await this.browser.yaScenario(this, prepareCartState);
            },
        },
        prepareSuite(bnplWidgetSuite, {
            params: {
                reportState: defaultOfferWithBnpl,
            },
        }),
        prepareSuite(bnplPriceLabelSuite, {
            params: {
                reportState: defaultOfferWithBnpl,
            },
            pageObjects: {
                defaultOffer() {
                    return this.createPageObject(DefaultOffer);
                },
            },
        })
    ),
});

