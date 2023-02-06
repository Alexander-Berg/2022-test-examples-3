import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import ProductTabs from '@self/platform/widgets/content/ProductTabs/__pageObject';
import SearchSimilar from '@self/platform/widgets/content/SearchSimilar/__pageObject';
import {initLazyWidgets} from '@self/project/src/spec/gemini/helpers/initLazyWidgets';
import Price from '@self/platform/components/Price/__pageObject';
import ProductRatingStat from '@self/platform/spec/page-objects/components/ProductRatingStat';
import ProductReview from '@self/platform/spec/page-objects/components/ProductReview';
import ShopRating from '@self/platform/spec/page-objects/components/ShopRating';
import SearchSnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import OfferPage from '@self/platform/widgets/pages/OfferPage/__pageObject';
import {offers} from '@self/project/src/spec/gemini/configs/offers';


import {
    hideRegionPopup,
    hideDevTools,
    hideProductTabs,
    hideHeader2,
    hideTopmenu,
    hideFooterSubscriptionWrap,
    hideFooter,
    hideHeadBanner,
    hideAllElementsBySelector,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

const OFFER_WAREID = offers.offerWithLinkedKM.wareid;
const OFFERS_REVIEWS_WAREID = offers.offerWithLinkedKMAndReviews.wareid;

export default {
    suiteName: 'KO-With-Product',
    before(actions) {
        setDefaultGeminiCookies(actions);
        // Тут нельзя заменить на MainSuite.before(actions), потому что надо оставить ProductTabs
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideHeader2(actions);
        hideHeadBanner(actions);
        hideTopmenu(actions);
        hideFooterSubscriptionWrap(actions);
        hideFooter(actions);
    },
    childSuites: [
        {
            suiteName: 'Tabs',
            url: `offer/${OFFER_WAREID}`,
            selector: ProductTabs.root,
            capture() {
            },
        },
        {
            suiteName: 'DescriptionTab',
            url: `offer/${OFFER_WAREID}`,
            before(actions) {
                hideProductTabs(actions);
            },
            childSuites: [
                {
                    suiteName: 'Main',
                    selector: MainSuite.selector,
                    ignore: [
                        Price.root,
                        ShopRating.root,
                    ],
                    before(actions) {
                        initLazyWidgets(actions, 4000);
                        hideAllElementsBySelector(actions, SearchSimilar.root);
                    },
                    capture() {
                    },
                },
                {
                    suiteName: 'SimilarCard',
                    selector: SearchSimilar.snippet,
                    ignore: [
                        SearchSimilar.snippetPrice,
                        SearchSnippetCard.morePrices,
                    ],
                    before(actions) {
                        // Триггерим ленивый виджет
                        // eslint-disable-next-line no-new-func
                        actions.executeJS(new Function(`
                           document.querySelector('${OfferPage.similarContainer}').scrollIntoView()
                        `));
                        actions.waitForElementToShow(SearchSimilar.snippet, 5000);
                    },
                    capture() {},
                },
            ],
        },
        {
            suiteName: 'SpecTab',
            url: `offer/${OFFER_WAREID}/spec`,
            selector: MainSuite.selector,
            before(actions) {
                hideProductTabs(actions);
            },
            capture() {
            },
        },
        {
            suiteName: 'SimilarTab',
            url: `offer/${OFFER_WAREID}/similar`,
            before(actions) {
                hideProductTabs(actions);
            },
            childSuites: [
                {
                    suiteName: 'Main',
                    selector: MainSuite.selector,
                    before(actions) {
                        hideAllElementsBySelector(actions, SearchSimilar.snippet);
                    },
                    capture() {
                    },
                },
                {
                    suiteName: 'SimilarCard',
                    selector: SearchSimilar.snippet,
                    ignore: [
                        SearchSimilar.snippetPrice,
                        SearchSnippetCard.morePrices,
                    ],
                    before(actions) {
                        actions.waitForElementToShow(SearchSimilar.snippet, 1000);
                    },
                    capture() {
                    },
                },
            ],
        },
        {
            suiteName: 'ArticlesTab',
            url: `offer/${OFFER_WAREID}/articles`,
            before(actions) {
                hideProductTabs(actions);
            },
            childSuites: [
                {
                    suiteName: 'Main',
                    selector: MainSuite.selector,
                    capture() {
                    },
                },
            ],
        },
        {
            suiteName: 'ReviewsTab',
            url: `offer/${OFFERS_REVIEWS_WAREID}/reviews`,
            before(actions) {
                hideProductTabs(actions);
            },
            childSuites: [
                {
                    suiteName: 'Main',
                    selector: MainSuite.selector,
                    ignore: [
                        ProductRatingStat.root,
                    ],
                    before(actions) {
                        hideAllElementsBySelector(actions, ProductReview.root);
                    },
                    capture() {
                    },
                },
                {
                    suiteName: 'ReviewFactorSummary',
                    selector: ProductRatingStat.root,
                    capture() {},
                },
                {
                    suiteName: 'ReviewSnippet',
                    selector: ProductReview.root,
                    capture() {
                    },
                },
            ],
        },
    ],
};
