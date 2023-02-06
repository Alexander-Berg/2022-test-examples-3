import {hideRegionPopup, hideModalFloat, hideFirstBlogPost} from '@self/platform/spec/gemini/helpers/hide';
import Blog from '@self/platform/spec/page-objects/widgets/content/Blog';
import {setDefaultGeminiCookies} from '@self/project/src/spec/gemini/helpers/setDefaultCookies';

export default {
    suiteName: 'BlogPosts',
    url: '/blog',
    before(actions) {
        setDefaultGeminiCookies(actions);
        hideRegionPopup(actions);
        hideModalFloat(actions);
    },
    childSuites: [
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A49%3A50.320224.jpg
            suiteName: 'PageWithoutContent',
            before(actions) {
                for (let i = 0; i < 5; i++) {
                    hideFirstBlogPost(actions);
                }
            },
            selector: Blog.blog,
            capture() {},
        },
        {
            // https://jing.yandex-team.ru/files/lengl/uhura_2022-05-13T14%3A49%3A30.632525.jpg
            suiteName: 'FirstBlogPost',
            selector: Blog.nthPost(1),
            capture() {},
        },
    ],
};
