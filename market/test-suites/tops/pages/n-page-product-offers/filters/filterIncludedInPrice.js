import {prepareSuite, makeSuite} from 'ginny';
import {productOffersWithIIPF} from '@self/platform/spec/hermione/fixtures/productOffers/productOffersWithIIPF';

import FilterIncludedInPriceSuite from '@self/platform/spec/hermione/test-suites/blocks/FilterIncludedInPrice';
import FilterIncludedInPrice from '@self/platform/spec/page-objects/FilterIncludedInPrice';
import Delivery from '@self/platform/spec/page-objects/n-delivery';
import SnippetCard from '@self/platform/spec/page-objects/n-snippet-card';

export default makeSuite('Фильтр цены с учетом доставки.', {
    environment: 'kadavr',
    story: prepareSuite(FilterIncludedInPriceSuite, {
        hooks: {
            beforeEach() {
                const {reportState, productId, slug} = productOffersWithIIPF(false);

                return this.browser.setState('report', reportState)
                    .then(() => this.browser.yaOpenPage(
                        'market:product-offers',
                        {productId, slug}
                    ));
            },
        },
        pageObjects: {
            filterIncludedInPrice() {
                return this.createPageObject(FilterIncludedInPrice);
            },
            snippetDelivery() {
                return this.createPageObject(
                    Delivery,
                    {parent: SnippetCard.root}
                );
            },
        },
    }),
});
