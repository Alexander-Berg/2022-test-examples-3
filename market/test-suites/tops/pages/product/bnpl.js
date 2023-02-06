import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import BnplInfo from '@self/root/src/components/BnplInfo/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import CheckoutLayoutConfirmation from
    '@self/root/src/widgets/content/checkout/layout/CheckoutLayoutConfirmationPage/view/__pageObject';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import {bnplPriceLabelSuite, bnplWidgetSuite} from '@self/root/src/spec/hermione/test-suites/blocks/bnpl';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {productWithBnpl, phoneProductRoute} from '@self/platform/spec/hermione/fixtures/product';

import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import {prepareCartState} from '@self/root/src/spec/hermione/scenarios/bnpl';

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
                await this.browser.yaOpenPage(PAGE_IDS_COMMON.PRODUCT, phoneProductRoute);
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));

                await this.browser.allure.runStep(
                    'Дожидаемся загрузки блока ДО',
                    () => this.defaultOffer.waitForVisible()
                );
                // сразу меняем стейт для корзины
                await this.browser.yaScenario(this, prepareCartState);
            },
        },
        prepareSuite(bnplWidgetSuite, {
            params: {
                reportState: productWithBnpl,
            },
        }),
        prepareSuite(bnplPriceLabelSuite, {
            params: {
                reportState: productWithBnpl,
            },
        })
    ),
});

