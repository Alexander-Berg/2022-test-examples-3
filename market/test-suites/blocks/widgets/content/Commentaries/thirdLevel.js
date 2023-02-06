import {pathOrUnsafe, reduce} from 'ambar';
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import ComplainCommentSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/complainComment';
import CommentsWithThreeDepthLevelsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/commentsWithThreeDepthLevels';
import VotesSuite from '@self/platform/spec/hermione/test-suites/blocks/Votes';
// page-objects
import SmallForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import CommentList from '@self/platform/spec/page-objects/components/Comment/List';
import CommentSnippet from '@self/platform/spec/page-objects/components/Comment/Snippet';
import CommentToolbar from '@self/platform/spec/page-objects/components/Comment/Toolbar';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import {newComment, users} from './mocks/comments.mock';

const getSchemaWithoutUsers = params => reduce((acc, value, key) => {
    if (key !== 'users') {
        acc.key = value;
    }
}, pathOrUnsafe({}, ['schema'], params), {});

export default mergeSuites(
    makeSuite('Жалобы на комментарий', {
        story: prepareSuite(ComplainCommentSuite, {
            pageObjects: {
                firstLevelCommentSnippet() {
                    return this.createPageObject(CommentSnippet);
                },
                firstLevelToolbar() {
                    return this.createPageObject(CommentToolbar);
                },
                secondLevelCommentSnippet() {
                    return this.createPageObject(CommentSnippet, {parent: this.firstLevelCommentSnippet});
                },
                secondLevelCommentToolbar() {
                    return this.createPageObject(CommentToolbar, {parent: this.secondLevelCommentSnippet});
                },
                thirdLevelCommentSnippet() {
                    return this.createPageObject(CommentSnippet, {parent: this.secondLevelCommentSnippet});
                },
                commentToolbar() {
                    return this.createPageObject(CommentToolbar, {parent: this.thirdLevelCommentSnippet});
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        ...getSchemaWithoutUsers(this.params),
                        users,
                        commentary: [
                            newComment({
                                id: 1,
                                author: {id: Number(profiles.ugctest3.uid) + 1},
                                entityId: this.params.entityId,
                            }),
                            newComment({
                                id: 2,
                                parentId: 1,
                                author: {id: Number(profiles.ugctest3.uid) + 1},
                                entityId: this.params.entityId,
                            }),
                            newComment({
                                id: 3,
                                parentId: 2,
                                author: {id: Number(profiles.ugctest3.uid) + 1},
                                entityId: this.params.entityId,
                            }),
                        ],
                    });
                    await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentSnippet.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.thirdLevelCommentSnippet.isVisible(),
                    });
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
    }),
    makeSuite('Лайки на комментарии', {
        story: prepareSuite(VotesSuite, {
            pageObjects: {
                firstLevelCommentSnippet() {
                    return this.createPageObject(CommentSnippet);
                },
                firstLevelCommentToolbar() {
                    return this.createPageObject(CommentToolbar, {parent: this.firstLevelCommentSnippet});
                },
                secondLevelCommentSnippet() {
                    return this.createPageObject(CommentSnippet, {parent: this.firstLevelCommentSnippet});
                },
                secondLevelCommentToolbar() {
                    return this.createPageObject(CommentToolbar, {parent: this.secondLevelCommentSnippet});
                },
                thirdLevelCommentSnippet() {
                    return this.createPageObject(CommentSnippet, {parent: this.secondLevelCommentSnippet});
                },
                votes() {
                    return this.createPageObject(Votes, {parent: this.thirdLevelCommentSnippet});
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        ...getSchemaWithoutUsers(this.params),
                        users,
                        commentary: [
                            newComment({
                                id: 1,
                                author: {id: Number(profiles.ugctest3.uid)},
                                entityId: this.params.entityId,
                            }),
                            newComment({
                                id: 2,
                                parentId: 1,
                                author: {id: Number(profiles.ugctest3.uid)},
                                entityId: this.params.entityId,
                            }),
                            newComment({
                                id: 3,
                                parentId: 2,
                                author: {id: Number(profiles.ugctest3.uid)},
                                entityId: this.params.entityId,
                            }),
                        ],
                    });
                    await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentSnippet.isVisible(),
                    });
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.secondLevelCommentToolbar.clickExpandChildren(),
                        valueGetter: () => this.thirdLevelCommentSnippet.isVisible(),
                    });
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
    }),
    prepareSuite(CommentsWithThreeDepthLevelsSuite, {
        pageObjects: {
            firstLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
            firstLevelCommentToolbar() {
                return this.createPageObject(CommentToolbar, {
                    parent: this.firstLevelCommentSnippet,
                });
            },
            secondLevelCommentList() {
                return this.createPageObject(CommentList, {
                    parent: this.firstLevelCommentSnippet,
                });
            },
            secondLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet, {
                    parent: this.secondLevelCommentList,
                });
            },
            secondLevelCommentToolbar() {
                return this.createPageObject(CommentToolbar, {
                    parent: this.secondLevelCommentSnippet,
                });
            },
            thirdLevelCommentList() {
                return this.createPageObject(CommentList, {
                    parent: this.secondLevelCommentSnippet,
                });
            },
            thirdLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet, {
                    parent: this.thirdLevelCommentList,
                });
            },
            thirdLevelCommentToolbar() {
                return this.createPageObject(CommentToolbar, {
                    parent: this.thirdLevelCommentSnippet,
                });
            },
            secondSmallForm() {
                return this.createPageObject(SmallForm, {parent: this.secondLevelCommentSnippet});
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('schema', {
                    ...getSchemaWithoutUsers(this.params),
                    users,
                    commentary: [
                        newComment({
                            id: 1,
                            author: {id: Number(profiles.ugctest3.uid)},
                            state: 'NEW',
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 2,
                            parentId: 1,
                            author: {id: Number(profiles.ugctest3.uid)},
                            state: 'NEW',
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 3,
                            parentId: 2,
                            author: {id: Number(profiles.ugctest3.uid)},
                            state: 'NEW',
                            entityId: this.params.entityId,
                        }),
                    ],
                });
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);

                await this.browser.yaWaitForChangeValue({
                    action: () => this.firstLevelCommentToolbar.clickExpandChildren(),
                    valueGetter: () => this.secondLevelCommentList.isVisible(),
                });
            },

        },
    })
);
