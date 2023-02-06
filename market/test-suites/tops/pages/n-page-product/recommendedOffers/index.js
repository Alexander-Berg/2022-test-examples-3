import {makeSuite, prepareSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import RecommendedOffersPO
    from '@self/platform/widgets/content/RecommendedOffers/__pageObject';

import RecommendedOffersA11YSuite
    from '@self/platform/spec/hermione/test-suites/blocks/RecommendedOffers/index.a11y';

import productWithTop6Offer from '../fixtures/productWithTop6Offer';

export default makeSuite('Виджет «Рекомендуемые оффера»', {
    environment: 'kadavr',
    story: prepareSuite(RecommendedOffersA11YSuite, {
        pageObjects: {
            recommendedOffers() {
                return this.createPageObject(RecommendedOffersPO);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState(
                    'report',
                    productWithTop6Offer.state
                );
                return this.browser.yaOpenPage(
                    PAGE_IDS_COMMON.YANDEX_MARKET_PRODUCT,
                    productWithTop6Offer.route
                );
            },
        },
    }),
});
