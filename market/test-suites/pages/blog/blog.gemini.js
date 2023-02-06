import {hideRegionPopup, hideDevTools, hideFirstBlogPost} from '@self/platform/spec/gemini/helpers/hide';
import BlogPosts from '@self/platform/spec/page-objects/widgets/content/BlogPosts';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'BlogPosts',
    url: '/blog',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideDevTools(actions);
        hideRegionPopup(actions);
    },
    childSuites: [
        {
            suiteName: 'PageWithoutContent',
            before(actions) {
                for (let i = 0; i < 5; i++) {
                    hideFirstBlogPost(actions);
                }
            },
            selector: BlogPosts.root,
            capture() {},
        },
        {
            suiteName: 'FirstBlogPost',
            selector: BlogPosts.firstBlogPost,
            capture() {},
        },
    ],
};
