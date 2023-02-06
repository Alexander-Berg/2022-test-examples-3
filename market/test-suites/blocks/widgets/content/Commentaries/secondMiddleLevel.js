import {pathOrUnsafe, reduce} from 'ambar';
import {prepareSuite, mergeSuites} from 'ginny';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import CommentWithOneChildSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/commentWithOneChild';
import EditCommentsWithReplySuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/editCommentsWithReply';
import AddMoreCommentsWithReplySuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/addMoreCommentsWithReply';
// page-objects
import SmallForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import CommentList from '@self/platform/spec/page-objects/components/Comment/List';
import CommentSnippet from '@self/platform/spec/page-objects/components/Comment/Snippet';
import CommentToolbar from '@self/platform/spec/page-objects/components/Comment/Toolbar';
import {newComment, users} from './mocks/comments.mock';

const getSchemaWithoutUsers = params => reduce((acc, value, key) => {
    if (key !== 'users') {
        acc.key = value;
    }
}, pathOrUnsafe({}, ['schema'], params), {});

export default mergeSuites(
    prepareSuite(CommentWithOneChildSuite, {
        pageObjects: {
            firstLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
            firstLevelToolbar() {
                return this.createPageObject(CommentToolbar, {parent: this.firstLevelCommentSnippet});
            },
            secondLevelCommentList() {
                return this.createPageObject(CommentList, {parent: this.firstLevelCommentSnippet});
            },
            smallForm() {
                return this.createPageObject(SmallForm, {parent: this.firstLevelCommentSnippet});
            },
            secondLevelCommentToolbar() {
                return this.createPageObject(CommentToolbar, {parent: this.secondLevelCommentList});
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
            },

        },
    }),
    prepareSuite(EditCommentsWithReplySuite, {
        pageObjects: {
            firstLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
            firstLevelToolbar() {
                return this.createPageObject(CommentToolbar);
            },
            secondLevelCommentList() {
                return this.createPageObject(CommentList, {parent: this.firstLevelCommentSnippet});
            },
            secondLevelSnippet1() {
                return this.createPageObject(
                    CommentSnippet, {
                        parent: `${CommentList.snippet}:nth-child(1)`,
                        root: `${CommentList.subtree}:nth-child(1)`,
                    });
            },
            secondLevelSnippet2() {
                return this.createPageObject(
                    CommentSnippet, {
                        parent: `${CommentList.snippet}:nth-child(1)`,
                        root: `${CommentList.subtree}:nth-child(2)`,
                    });
            },
            secondLevelChildrenToolbar1() {
                return this.createPageObject(CommentToolbar, {parent: this.secondLevelSnippet1});
            },
            secondLevelChildrenToolbar2() {
                return this.createPageObject(CommentToolbar, {parent: this.secondLevelSnippet2});
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
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 4,
                            parentId: 1,
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            replyTo: undefined,
                            text: 'third comment',
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 3,
                            parentId: 1,
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            replyTo: {id: profiles.ugctest3.uid, entity: 'user'},
                            text: 'second comment',
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 2,
                            parentId: 1,
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            replyTo: {id: profiles.ugctest3.uid, entity: 'user'},
                            text: 'first comment',
                            entityId: this.params.entityId,
                        }),
                    ],
                });
                this.params = {
                    ...this.params,
                    replyTo: 'Vasya P., ',
                    text1: 'first comment',
                    text2: 'second comment',
                    text3: 'third comment',
                };
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);

                await this.browser.yaWaitForChangeValue({
                    action: () => this.firstLevelToolbar.clickExpandChildren(),
                    valueGetter: () => this.secondLevelCommentList.isVisible(),
                });
                await this.secondLevelSnippet1.isVisible()
                    .should.eventually.to.be.equals(true, 'Сниппет отображается');
                await this.secondLevelChildrenToolbar1.moreActionsClick();
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    }),
    prepareSuite(AddMoreCommentsWithReplySuite, {
        pageObjects: {
            firstLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
            firstLevelToolbar() {
                return this.createPageObject(CommentToolbar);
            },
            secondLevelCommentList() {
                return this.createPageObject(CommentList, {parent: this.firstLevelCommentSnippet});
            },
            secondLevelSnippet() {
                return this.createPageObject(
                    CommentSnippet, {
                        parent: `${CommentList.snippet}:nth-child(1)`,
                        root: `${CommentList.subtree}:nth-child(1)`,
                    });
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
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 2,
                            parentId: 1,
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            replyTo: {id: profiles.ugctest3.uid, entity: 'user'},
                            text: 'first comment',
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 3,
                            parentId: 1,
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            replyTo: {id: profiles.ugctest3.uid, entity: 'user'},
                            text: 'second comment',
                            entityId: this.params.entityId,
                        }),
                        newComment({
                            id: 4,
                            parentId: 1,
                            author: {id: profiles.ugctest3.uid, entity: 'user'},
                            replyTo: undefined,
                            text: 'third comment',
                            entityId: this.params.entityId,
                        }),
                    ],
                });
                this.params = {
                    ...this.params,
                    replyTo: 'Vasya P., ',
                };
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);

                await this.firstLevelToolbar.isVisible()
                    .should.eventually.to.be.equals(true, 'Тулбар отображается на сниппете');
                await this.firstLevelToolbar.moreActionsClick();
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    })
);
