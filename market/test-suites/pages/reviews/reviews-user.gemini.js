import MainSuite from '@self/platform/spec/gemini/test-suites/blocks/main.gemini';
import UserReview from '@self/platform/spec/page-objects/components/UserReview';
import {
    hideModalFloat,
    hideParanja,
    hideRegionPopup,
    hideMooa,
} from '@self/platform/spec/gemini/helpers/hide';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'UserReviews',
    url: '/user/yg9wdxft3aa02tpvy0kuwp7ymm/reviews',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideModalFloat(actions);
        hideParanja(actions);
        hideRegionPopup(actions);
        hideMooa(actions);
    },
    childSuites: [
        {
            ...MainSuite,
            ignore: {every: 'img'},
        },
        {
            suiteName: 'ReviewSnippet',
            selector: UserReview.root,
            capture(actions) {
                actions.waitForElementToShow(UserReview.root, 10000);
            },
        },
    ],
};
