import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import WithoutUserVoteSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/authorizedUser/withoutUserVote';
import WithUserDislikeSuite from
    '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/authorizedUser/withUserDislike';
import WithUserLikeSuite from '@self/platform/spec/hermione/test-suites/blocks/AnswerSnippet/__votes/authorizedUser/withUserLike';
import Votes from '@self/platform/spec/page-objects/components/Votes';

/**
 * @param {PageObject.AnswerSnippet} answerSnippet
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок голосовалки когда пользователь авторизован', {
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    votes: () => this.createPageObject(
                        Votes,
                        {
                            parent: this.answerSnippet,
                        }
                    ),
                });
            },
        },
        prepareSuite(WithoutUserVoteSuite),
        prepareSuite(WithUserDislikeSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickDislike(),
                        valueGetter: () => this.votes.getDislikeCount(),
                    });
                },
            },
        }),
        prepareSuite(WithUserLikeSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                },
            },
        })
    ),
});
