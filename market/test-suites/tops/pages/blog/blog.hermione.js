import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import dayjs from 'dayjs';
import ru from 'dayjs/locale/ru';

import {postsMock} from '@self/platform/spec/hermione/test-suites/blocks/blog/kadavr-mock/blogPosts';

import BlogPostsSuite from '@self/platform/spec/hermione/test-suites/blocks/blog/blogPosts';
import BlogPostSuite from '@self/platform/spec/hermione/test-suites/blocks/blog/blogPost';
import BlogPosts from '@self/platform/spec/page-objects/widgets/content/BlogPosts';
import BlogPost from '@self/platform/spec/page-objects/widgets/content/BlogPost';

dayjs.locale(ru);
const config = {
    posts: postsMock,
    firstPostSlug: postsMock[0].slug,
    initialPostsCount: 5,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Блог.', {
    story: mergeSuites(
        prepareSuite(BlogPostsSuite, {
            pageObjects: {
                blogPosts() {
                    return this.createPageObject(BlogPosts);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('posts', postsMock);
                    await this.browser.yaOpenPage('market:blog');
                },
            },
            params: {
                posts: config.posts,
                initialPostsCount: config.initialPostsCount,
            },
        }),

        prepareSuite(BlogPostSuite, {
            pageObjects: {
                blogPost() {
                    return this.createPageObject(BlogPost);
                },
                blogPosts() {
                    return this.createPageObject(BlogPosts);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('posts', postsMock);
                    await this.browser.yaOpenPage('market:blog-post', {postSlug: config.firstPostSlug});
                },
            },
            params: {
                firstPostTitle: config.posts[0].approvedTitle,
                firstPostPublishDate: dayjs(config.posts[0].publishDate).format('D MMMM YYYY'),
            },
        })
    ),
});
