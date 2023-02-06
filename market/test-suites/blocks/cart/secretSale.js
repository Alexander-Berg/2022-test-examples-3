import {makeCase, makeSuite, mergeSuites} from 'ginny';
import assert from 'assert';

import {offerMock, skuMock} from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import {hasSecretSale} from '@self/root/src/spec/hermione/scenarios/loyalty';
import {hasSecretSale as hasSecretSaleMock} from '@self/root/src/spec/hermione/kadavr-mock/loyalty/loyaltyPrograms';
import {
    checkSecretSaleCheckouterRequests,
    prepareSecretSaleCheckouterState,
    checkSecretSaleCartSummaryDiscount,
} from '@self/root/src/spec/hermione/scenarios/secretSale';

import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';


/**
 * Тесты на закрытую распродажу для корзины и чекаута
 *
 * @param pageId
 */
module.exports = makeSuite('Закрытые распродажи', {
    environment: 'kadavr',
    feature: 'Закрытые распродажи',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    params: {
        pageId: 'Идентификатор страницы',
    },
    story: mergeSuites(
        {
            beforeEach() {
                assert(this.params.pageId, 'Param pageId must be defined');

                this.setPageObjects({
                    orderTotal: () => this.createPageObject(OrderTotal),
                    preloader: () => this.createPageObject(Preloader),
                });
            },
        },
        makeSuite('Пользователь участвует в закрытой распродаже', {
            story: {
                async beforeEach() {
                    await this.browser.yaScenario(
                        this,
                        hasSecretSale,
                        hasSecretSaleMock
                    );

                    await this.browser.yaScenario(
                        this,
                        prepareSecretSaleCheckouterState,
                        {skuMock, offerMock}
                    );
                },

                'При запросе корзины в чекаутере,': {
                    'в запросе передается параметр perkPromoId с id закрытой распродажи': makeCase({
                        issue: 'BLUEMARKET-10341',
                        id: 'bluemarket-3224',
                        async test() {
                            await this.browser.yaOpenPage(this.params.pageId);

                            return this.browser.yaScenario(this, checkSecretSaleCheckouterRequests);
                        },
                    }),
                },

                'При добавлении в корзину товара, участвующего в закрытой распродаже,': {
                    async beforeEach() {
                        return this.browser.yaOpenPage(this.params.pageId);
                    },

                    'отображается корректная скидка в саммари': makeCase({
                        issue: 'BLUEMARKET-10341',
                        id: 'bluemarket-3224',
                        async test() {
                            return this.browser.yaScenario(
                                this,
                                checkSecretSaleCartSummaryDiscount,
                                {offerMock}
                            );
                        },
                    }),
                },
            },
        })
    ),
});
