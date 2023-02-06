import schema from 'js-schema';
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import nodeConfig from '@self/platform/configs/current/node';

import {cutPriceOffer, OFFER_ID, CUT_PRICE_REASON} from '@self/platform/spec/hermione/fixtures/cutprice';
// suites
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import ConditionTypeSuite from '@self/platform/spec/hermione/test-suites/blocks/ConditionType';
import OfferDescriptionWithSpecsCutPriceSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/parts/OfferDescriptionWithSpecs/cutPrice';
// page-objects
import ConditionType from '@self/project/src/components/ConditionType/__pageObject';
import OfferSummary from '@self/platform/spec/page-objects/widgets/parts/OfferSummary';
import OfferDescriptionWithSpecs from '@self/platform/widgets/parts/OfferDescriptionWithSpecs/components/View/__pageObject';

export default makeSuite('Уценённые товары', {
    environment: 'kadavr',
    feature: 'б/у товары',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('report', cutPriceOffer);

                return this.browser.yaOpenPage('touch:offer', {
                    offerId: OFFER_ID,
                });
            },
        },

        prepareSuite(MetricaClickSuite, {
            meta: {
                id: 'm-touch-2798',
                issue: 'MOBMARKET-12246',
            },
            pageObjects: {
                offerSummary() {
                    return this.createPageObject(OfferSummary);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.offerSummary.waitForVisible();

                    this.params.selector = await this.offerSummary.getSelector(OfferSummary.clickOutButton);
                },
            },
            params: {
                counterId: nodeConfig.yaMetrika.market.id,
                expectedGoalName: 'offer-page_offer-summary_go-to-shop',
                payloadSchema: schema({
                    isCutPrice: true,
                }),
            },
        }),

        prepareSuite(ConditionTypeSuite, {
            meta: {
                id: 'm-touch-2804',
                issue: 'MOBMARKET-12252',
            },
            pageObjects: {
                conditionType() {
                    return this.createPageObject(ConditionType);
                },
            },
            params: {
                expectedConditionType: 'как новый',
            },
        }),

        prepareSuite(OfferDescriptionWithSpecsCutPriceSuite, {
            meta: {
                id: 'm-touch-2804',
                issue: 'MOBMARKET-12252',
            },

            pageObjects: {
                offerDescriptionWithSpecs() {
                    return this.createPageObject(OfferDescriptionWithSpecs);
                },
            },
            params: {
                expectedConditionReason: CUT_PRICE_REASON,
            },
        })
    ),
});

