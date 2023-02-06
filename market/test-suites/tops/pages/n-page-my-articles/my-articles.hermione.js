import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

// suites
import ArticleOnModerationSuite from '@self/platform/spec/hermione/test-suites/blocks/ArticleSnippet/articleOnModeration';
import ArticleRejectedSuite from '@self/platform/spec/hermione/test-suites/blocks/ArticleSnippet/articleRejected';
import ArticlePublishedSuite from '@self/platform/spec/hermione/test-suites/blocks/ArticleSnippet/articlePublished';
import ArticleInProgressSuite from '@self/platform/spec/hermione/test-suites/blocks/ArticleSnippet/articleInProgress';
// page-objects
import PopupContent from '@self/platform/spec/page-objects/components/MyArticles/ArticleSnippet/PopupContent';
import ArticleSnippet from '@self/platform/spec/page-objects/components/MyArticles/ArticleSnippet';
import ArticlesGrid from '@self/platform/spec/page-objects/components/MyArticles/ArticlesGrid';
import Dialog from '@self/platform/spec/page-objects/components/Dialog';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Личный кабинет. Страница со статьями пользователя.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    articleSnippet: () => this.createPageObject(ArticleSnippet),
                    articleSnippetPopupContent: () => this.createPageObject(PopupContent),
                    articlesGrid: () => this.createPageObject(ArticlesGrid),
                });
            },
        },
        prepareSuite(ArticleOnModerationSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        mboCmsApi: [{id: 666}],
                        articleModeration: [{articleId: 666, modState: 'NEW', modTime: 1540979427000}],
                    });
                    await this.browser.yaProfile('ugctest3', 'market:my-articles');
                    await this.articleSnippet.clickContextMenuButton();
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(ArticleRejectedSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        mboCmsApi: [{id: 666}],
                        articleModeration: [{articleId: 666, modState: 'REJECTED', modTime: 1540979427000}],
                    });
                    await this.browser.yaProfile('ugctest3', 'market:my-articles');
                    await this.articleSnippet.clickContextMenuButton();
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(ArticlePublishedSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        mboCmsApi: [{id: 666, semanticId: 'kak-delat-dela', type: 'brand'}],
                        articleModeration: [{articleId: 666, modState: 'APPROVED'}],
                    });
                    await this.browser.setState('Tarantino.data.result', [{
                        entity: 'page',
                        id: 666,
                        type: 'blog',
                    }]);
                    return this.browser.yaProfile('ugctest3', 'market:my-articles');
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        prepareSuite(ArticleInProgressSuite, {
            hooks: {
                async beforeEach() {
                    this.setPageObjects({
                        dialog: () => this.createPageObject(Dialog),
                    });
                    await this.browser.setState('schema', {
                        mboCmsApi: [{id: 666}],
                    });
                    await this.browser.yaProfile('ugctest3', 'market:my-articles');
                    await this.articleSnippet.clickContextMenuButton();
                },
                async afterEach() {
                    await this.browser.yaLogout();
                },
            },
        }),
        makeSuite('Статьи в статусе "Черновик"', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            mboCmsApi: [{id: 666, semanticId: 'kak-delat-dela', type: 'brand'}],
                            articleModeration: [{articleId: 666, modState: 'NEW'}],
                        });
                        await this.browser.setState('Tarantino.data.result', [{
                            entity: 'page',
                            id: 666,
                            type: 'blog',
                        }]);
                        await this.browser.yaProfile('ugctest3', 'market:my-articles');
                    },
                    async afterEach() {
                        await this.browser.yaLogout();
                    },
                }
            ),
        }),
        makeSuite('Статьи в статусе "Опубликованно"', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            mboCmsApi: [{id: 666, semanticId: 'kak-delat-dela', type: 'brand'}],
                            articleModeration: [{articleId: 666, modState: 'APPROVED'}],
                        });
                        await this.browser.setState('Tarantino.data.result', [{
                            entity: 'page',
                            id: 666,
                            type: 'blog',
                        }]);
                        await this.browser.yaProfile('ugctest3', 'market:my-articles');
                    },
                    async afterEach() {
                        await this.browser.yaLogout();
                    },
                }
            ),
        })
    ),
});
