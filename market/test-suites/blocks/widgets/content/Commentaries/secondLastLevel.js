import {pathOrUnsafe, reduce} from 'ambar';
import {prepareSuite, mergeSuites} from 'ginny';

// suites
import LastLevelCommentsWithTwoDepthLevelsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/lastLevelCommentsWithTwoDepthLevels';
// page-objects
import SmallForm from '@self/platform/spec/page-objects/components/Comment/SmallForm';
import CommentList from '@self/platform/spec/page-objects/components/Comment/List';
import CommentSnippet from '@self/platform/spec/page-objects/components/Comment/Snippet';
import CommentToolbar from '@self/platform/spec/page-objects/components/Comment/Toolbar';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {newComment, users} from './mocks/comments.mock';

const getSchemaWithoutUsers = params => reduce((acc, value, key) => {
    if (key !== 'users') {
        acc.key = value;
    }
}, pathOrUnsafe({}, ['schema'], params), {});

export default mergeSuites(
    prepareSuite(LastLevelCommentsWithTwoDepthLevelsSuite, {
        pageObjects: {
            firstLevelCommentSnippet() {
                return this.createPageObject(CommentSnippet);
            },
            firstLevelCommentToolbar() {
                return this.createPageObject(CommentToolbar, {
                    parent: this.firstLevelCommentSnippet,
                });
            },
            firstLevelCommentList() {
                return this.createPageObject(CommentList);
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
            firstSmallForm() {
                return this.createPageObject(SmallForm, {parent: this.firstLevelCommentSnippet});
            },
        },
        hooks: {
            async beforeEach() {
                const schema = {
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
                    ],
                };
                await this.browser.setState('schema', schema);
                await this.browser.yaProfile('ugctest3', this.params.pageTemplate, this.params.pageParams);
            },

        },
    })
);
