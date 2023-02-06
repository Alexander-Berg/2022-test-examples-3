import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import dayjs from 'dayjs';
import localeRu from 'dayjs/locale/ru';

import {postsMock} from '@self/platform/spec/hermione/test-suites/blocks/blog/kadavr-mock/blogPosts';
import BlogPostSuite from '@self/platform/spec/hermione/test-suites/blocks/blog/blogPost';
import BlogPost from '@self/platform/spec/page-objects/widgets/content/BlogPost';

dayjs.locale(localeRu);

const config = {
    posts: postsMock,
    firstPostSlug: postsMock[0].slug,
    initialPostsCount: 5,
};


// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Блог: страница поста.', {
    story: mergeSuites(
        prepareSuite(BlogPostSuite, {
            pageObjects: {
                blogPost() {
                    return this.createPageObject(BlogPost);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('posts', postsMock);
                    await this.browser.yaOpenPage('touch:blog-post', {postSlug: config.firstPostSlug});
                },
            },
            params: {
                firstPostTitle: config.posts[0].approvedTitle,
                firstPostPublishDate: dayjs(config.posts[0].publishDate).format('D MMMM'),
            },
        })
    ),
});
