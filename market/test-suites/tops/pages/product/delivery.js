import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
// suites
import DefaultOfferDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDelivery';
// page-objects
import Delivery from '@self/platform/spec/page-objects/widgets/parts/OfferSummary/Delivery';
// fixtures
import {
    phoneProductRoute,
    productWithDefaultOfferPreOrderOnly,
    productWithDefaultOfferWith60days,
} from '@self/platform/spec/hermione/fixtures/product';

const Order60daysSuite = makeSuite('До 60 дней', {
    environment: 'kadavr',
    story: prepareSuite(DefaultOfferDeliverySuite, {
        pageObjects: {
            delivery() {
                return this.createPageObject(Delivery);
            },
        },
        meta: {
            id: 'm-touch-3334',
            issue: 'MARKETFRONT-11608',
        },
        hooks: {
            async beforeEach() {
                const dataMixin = {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                };

                await this.browser.setState('report', mergeState([
                    productWithDefaultOfferWith60days,
                    dataMixin,
                ]));

                await this.browser.yaSetCookie({
                    name: 'currentRegionId',
                    value: '213',
                });

                this.params = {
                    expectedText: 'Курьером, до 60 дней — бесплатно',
                };

                await this.browser.yaOpenPage('touch:product', phoneProductRoute);
            },
        },
    }),
});

const PreOrderOnlySuite = makeSuite('Предзаказ', {
    environment: 'kadavr',
    story: prepareSuite(DefaultOfferDeliverySuite, {
        pageObjects: {
            delivery() {
                return this.createPageObject(Delivery);
            },
        },
        meta: {
            id: 'm-touch-3330',
            issue: 'MARKETFRONT-11608',
        },
        hooks: {
            async beforeEach() {
                const dataMixin = {
                    data: {
                        search: {
                            total: 1,
                            totalOffers: 1,
                        },
                    },
                };

                await this.browser.setState('report', mergeState([
                    productWithDefaultOfferPreOrderOnly,
                    dataMixin,
                ]));

                await this.browser.yaSetCookie({
                    name: 'currentRegionId',
                    value: '213',
                });

                this.params = {
                    expectedText: 'Предзаказ',
                };

                await this.browser.yaOpenPage('touch:product', phoneProductRoute);
            },
        },
    }),
});

export default mergeSuites(
    prepareSuite(Order60daysSuite),
    prepareSuite(PreOrderOnlySuite)
);
