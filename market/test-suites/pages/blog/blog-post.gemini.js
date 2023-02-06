import {hideRegionPopup, hideModalFloat} from '@self/platform/spec/gemini/helpers/hide';
import BlogPost from '@self/platform/spec/page-objects/widgets/content/BlogPost';
import Blog from '@self/platform/spec/page-objects/widgets/content/Blog';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'BlogPost',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A48%3A45.407623.png
            suiteName: 'FullBlogPost',
            url: '/blog',
            before(actions, find) {
                actions
                    .click(find(Blog.nthPostLink(1)))
                    .waitForElementToShow(BlogPost.blogPost, 10000);
            },
            selector: BlogPost.blogPost,
            capture() {},
        },
    ],
};
