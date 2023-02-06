import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import OfferSnippetSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/PricesOfferSnippetSuite';
import JournalScrollboxSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/km-journal-scrollbox';
import ProductHeadlineSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/ProductTitle';
import FiltersSuite from '@self/platform/spec/gemini/test-suites/blocks/KM/product-offers-layout';
import FilterShopsSuite from '@self/platform/spec/gemini/test-suites/blocks/filters/filter-shops';
import FilterDeliveryTypeSuite from '@self/platform/spec/gemini/test-suites/blocks/filters/delivery-type-filter';
import FilterPromoSuite from '@self/platform/spec/gemini/test-suites/blocks/filters/filter-promo-type';
import FilterPaymentSuite from '@self/platform/spec/gemini/test-suites/blocks/filters/payment-filter';
import FilterPriceSuite from '@self/platform/spec/gemini/test-suites/blocks/filters/price-filter';
import FilterShopRatingSuite from '@self/platform/spec/gemini/test-suites/blocks/filters/shop-rating-filter';
import FilterDeliverySuite from '@self/platform/spec/gemini/test-suites/blocks/filters/delivery-filter';

import {
    hideScrollbar,
    hideRegionPopup,
    hideDevTools,
    hideProductTabs,
    hideTopmenu,
    hideFooter,
    hideHeader2,
    hideElementBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';
import ClientAction from '@self/platform/spec/gemini/helpers/clientAction';

import SnippetList from '@self/platform/widgets/content/productOffers/Results/components/ResultsPaged/__pageObject';
import MiniCard from '@self/platform/components/PageCardTitle/MiniCard/__pageObject';

export default {
    suiteName: 'KM-offers',
    url: '/product--smartfon-apple-iphone-11-128gb/558168089/offers',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideScrollbar(actions);
        hideDevTools(actions);
        hideProductTabs(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                hideFooter(actions);
                hideTopmenu(actions);
                hideHeader2(actions);
                const selector = [
                    ProductHeadlineSuite.selector,
                    SnippetList.root, // OfferSnippetSuite
                    FiltersSuite.selector,
                    '[id="journal-pages"]', // JournalScrollboxSuite
                ].join(', ');
                new ClientAction(actions).removeElems(selector);
            },
        },
        ProductHeadlineSuite,
        OfferSnippetSuite,
        {
            suiteName: 'Filters',
            before(actions) {
                hideElementBySelector(actions, MiniCard.root);
            },
            childSuites: [
                {
                    ...FiltersSuite,
                    url: '/product/1726380154/offers',
                    ignore: [
                        '[data-zone-name="minimap"]',
                        {every: '[data-range-input-type="from"]'},
                        {every: '[data-range-input-type="to"]'},
                    ],
                    before(actions) {
                        const selector = [FilterShopsSuite.selector, FilterDeliveryTypeSuite.selector,
                            FilterDeliverySuite.selector, FilterShopRatingSuite.selector, FilterPriceSuite.selector,
                            FilterPromoSuite.selector, FilterPaymentSuite.selector].join(', ');
                        new ClientAction(actions).removeElems(selector);
                    },
                },
                FilterShopsSuite,
                {
                    ...FilterPriceSuite,
                    ignore: [
                        '#glpricefrom',
                        '#glpriceto',
                    ],
                },
                FilterShopRatingSuite,
                FilterDeliverySuite,
                FilterDeliveryTypeSuite,
                FilterPromoSuite,
                FilterPaymentSuite,
            ],
        },
        JournalScrollboxSuite,
    ],
};
