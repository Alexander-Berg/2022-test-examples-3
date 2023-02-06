import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
// suites
import DefaultOfferDeliverySuite from '@self/platform/spec/hermione/test-suites/blocks/AutomaticallyCalculatedDelivery';
// page-objects
import Delivery from '@self/platform/spec/page-objects/widgets/parts/OfferSummary/Delivery';
// fixtures
import {notInStockOffer, OFFER_ID, preOrderOnlyOffer} from '@self/platform/spec/hermione/fixtures/offer';

const Order60daysSuite = makeSuite('До 60 дней', {
    environment: 'kadavr',
    story: prepareSuite(DefaultOfferDeliverySuite, {
        pageObjects: {
            delivery() {
                return this.createPageObject(Delivery);
            },
        },
        meta: {
            id: 'm-touch-3329',
            issue: 'MARKETFRONT-11608',
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('report', notInStockOffer);

                await this.browser.yaSetCookie({
                    name: 'currentRegionId',
                    value: '213',
                });

                this.params = {
                    expectedText: 'Бесплатная доставка, до 60 дней',
                };
                await this.browser.yaOpenPage('touch:offer', {
                    offerId: OFFER_ID,
                });
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
            id: 'm-touch-3331',
            issue: 'MARKETFRONT-11608',
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('report', preOrderOnlyOffer);

                this.params = {
                    expectedText: 'Предзаказ',
                };
                await this.browser.yaOpenPage('touch:offer', {
                    offerId: OFFER_ID,
                });
            },
        },
    }),
});

export default mergeSuites(
    prepareSuite(Order60daysSuite),
    prepareSuite(PreOrderOnlySuite)
);
