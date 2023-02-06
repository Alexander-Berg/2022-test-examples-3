import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main';
import SnippetCell from '@self/platform/spec/page-objects/VertProductSnippet';
import ReviewItem from '@self/platform/components/ShopReview/Review/__pageObject';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import ShopReviewList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';
import Navnode from '@self/platform/containers/Navnode/__pageObject';
import {offers} from '@self/project/src/spec/gemini/configs/offers';

import {
    hideRegionPopup,
    hideDevTools,
    hideElementBySelector,
    hideHeader2,
    hideFooter,
    hideTopmenu,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';


export default {
    suiteName: 'HubShop',
    url: `/shop/${offers.offerWithLinkedKM.shopid}`,
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
        hideHeader2(actions);
        hideFooter(actions);
        hideTopmenu(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            before(actions) {
                MainSuite.before(actions);
                hideElementBySelector(actions, ShopReviewList.root);
                hideElementBySelector(actions, ScrollBox.root);
            },
            ignore: [
                {every: Navnode.root},
            ],
        },
        {
            suiteName: 'ReviewSnippet',
            selector: ReviewItem.root,
            capture() {
            },
        },
        {
            suiteName: 'Popular',
            selector: ScrollBox.root,
            ignore: [
                {every: SnippetCell.root},
            ],
            capture() {},
        },
    ],
};
