import {pathOrUnsafe, reduce} from 'ambar';
import {mergeSuites, makeSuite, prepareSuite} from 'ginny';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import ComplainCommentSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/complainComment';
import VotesSuite from '@self/platform/spec/hermione/test-suites/blocks/Votes';
// page-objects
import CommentSnippet from '@self/platform/spec/page-objects/components/Comment/Snippet';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import CommentToolbar from '@self/platform/spec/page-objects/components/Comment/Toolbar';

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
                    return this.createPageObject(CommentToolbar, {parent: this.firstLevelCommentSnippet});
                },
                commentSnippet() {
                    return this.createPageObject(CommentSnippet, {parent: this.firstLevelCommentSnippet});
                },
                commentToolbar() {
                    return this.createPageObject(CommentToolbar, {parent: this.secondLevelCommentSnippet});
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
                        ],
                    });
                    await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.clickExpandChildren(),
                        valueGetter: () => this.commentSnippet.isVisible(),
                    });
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
    }),
    makeSuite('Лайки на комментарий', {
        story: prepareSuite(VotesSuite, {
            meta: {
                feature: 'Статья',
            },
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
                votes() {
                    return this.createPageObject(Votes, {parent: this.secondLevelCommentSnippet});
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
                        ],
                    });
                    await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.firstLevelToolbar.clickExpandChildren(),
                        valueGetter: () => this.secondLevelCommentSnippet.isVisible(),
                    });
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
    })
);
