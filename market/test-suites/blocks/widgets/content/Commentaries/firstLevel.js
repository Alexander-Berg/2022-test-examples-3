import {reduce, pathOrUnsafe} from 'ambar';
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
// suites
import AuthorizedUserAddFirstCommentSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/authorizedUserAddFirstComment';
import CommentariesCommentSnippetSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/commentSnippet';
import VotesSuite from '@self/platform/spec/hermione/test-suites/blocks/Votes';
import OwnCommentSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/ownComment';
import ComplainCommentSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/complainComment';
import AddCommentsWithReplySuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/addCommentsWithReply';
// page-objects
import BigForm from '@self/platform/spec/page-objects/components/Comment/BigForm';
import CommentList from '@self/platform/spec/page-objects/components/Comment/List';
import CommentSnippet from '@self/platform/spec/page-objects/components/Comment/Snippet';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import CommentToolbar from '@self/platform/spec/page-objects/components/Comment/Toolbar';
import Notification from '@self/root/src/components/Notification/__pageObject';

import {othersComment, ownComment, users, newComment} from './mocks/comments.mock';

const getSchemaWithoutUsers = params => reduce((acc, value, key) => {
    if (key !== 'users') {
        acc.key = value;
    }
}, pathOrUnsafe({}, ['schema'], params), {});

export default mergeSuites(
    prepareSuite(AuthorizedUserAddFirstCommentSuite, {
        pageObjects: {
            bigForm() {
                return this.createPageObject(BigForm);
            },
            commentList() {
                return this.createPageObject(CommentList);
            },
            notification() {
                return this.createPageObject(Notification);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('schema', {
                    ...getSchemaWithoutUsers(this.params),
                });
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    }),
    prepareSuite(CommentariesCommentSnippetSuite, {
        pageObjects: {
            commentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('schema', {
                    ...getSchemaWithoutUsers(this.params),
                    commentary: [{id: 1, text: 'a'.repeat(5000), state: 'NEW', entityId: this.params.entityId}],
                });
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    }),
    prepareSuite(VotesSuite, {
        meta: {
            feature: 'Лайки на комментарии',
        },
        pageObjects: {
            commentToolbar() {
                return this.createPageObject(CommentToolbar);
            },
            votes() {
                return this.createPageObject(Votes, this.commentToolbar);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('schema', {
                    ...getSchemaWithoutUsers(this.params),
                    users,
                    commentary: ownComment(profiles.ugctest3.uid, this.params.entityId),
                });
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    }),
    prepareSuite(OwnCommentSuite, {
        pageObjects: {
            commentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('schema', {
                    ...getSchemaWithoutUsers(this.params),
                    users,
                    commentary: ownComment(profiles.ugctest3.uid, this.params.entityId),
                });
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);

                await this.commentSnippet.isMoreActionsExist()
                    .should.eventually.to.be.equal(true, 'Троеточие отображается на сниппете.');
                await this.commentSnippet.moreActionsClick();
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    }),
    makeSuite('Жалобы на комментарий', {
        story: prepareSuite(ComplainCommentSuite, {
            pageObjects: {
                commentSnippet() {
                    return this.createPageObject(CommentSnippet);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('schema', {
                        ...getSchemaWithoutUsers(this.params),
                        users,
                        commentary: othersComment(this.params.entityId),
                    });
                    await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
        }),
    }),
    prepareSuite(AddCommentsWithReplySuite, {
        pageObjects: {
            firstLevelSnippet() {
                return this.createPageObject(CommentSnippet);
            },
            firstLevelToolbar() {
                return this.createPageObject(CommentToolbar, {parent: this.firstLevelSnippet});
            },
            secondLevelSnippet() {
                return this.createPageObject(CommentSnippet, {parent: this.firstLevelSnippet});
            },
            secondLevelToolbar() {
                return this.createPageObject(CommentToolbar, {parent: this.secondLevelSnippet});
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
            },
            async afterEach() {
                return this.browser.yaLogout();
            },
        },
    })
);
