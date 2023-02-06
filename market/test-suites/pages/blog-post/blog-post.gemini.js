import {hideRegionPopup, hideDevTools} from '@self/platform/spec/gemini/helpers/hide';
import BlogPost from '@self/platform/spec/page-objects/widgets/content/BlogPost';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'BlogPost',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            suiteName: 'FullBlogPost',
            url: '/blog/yandeks-market-pomozhet-vybrat-tovary-v-kotorykh-trudno-razobratsya',
            selector: BlogPost.root,
            capture() {},
        },
    ],
};
