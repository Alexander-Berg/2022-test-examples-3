// flowlint-next-line untyped-import: off
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import dayjs from 'dayjs';
import localeRu from 'dayjs/locale/ru';

import {postsMock} from '@self/platform/spec/hermione/test-suites/blocks/blog/kadavr-mock/blogPosts';
import BlogSuite from '@self/platform/spec/hermione/test-suites/blocks/blog/blog';
import Blog from '@self/platform/spec/page-objects/widgets/content/Blog';

dayjs.locale(localeRu);

const config = {
    pageTitle: 'Новости компании',
    posts: postsMock,
    firstPostSlug: postsMock[0].slug,
    initialPostsCount: 5,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Блог: список постов.', {
    story: mergeSuites(
        prepareSuite(BlogSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('posts', postsMock);
                    await this.browser.yaOpenPage('touch:blog');
                },
            },
            params: {
                pageTitle: config.pageTitle,
                firstPost: {
                    title: config.posts[0].approvedTitle,
                    date: dayjs(config.posts[0].publishDate).format('D MMMM'),
                    slug: config.posts[0].slug,
                },
            },
            pageObjects: {
                blog() {
                    return this.createPageObject(Blog);
                },
            },
        })
    ),
});
