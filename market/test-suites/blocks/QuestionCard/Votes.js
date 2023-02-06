import {makeCase, makeSuite} from 'ginny';
import Votes from '@self/platform/spec/page-objects/components/Votes';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок снипета вопроса. Лайки под вопросом.', {
    feature: 'Лайки/Дизлайки',
    issue: 'MOBMARKET-9080',
    id: 'm-touch-2259',
    story: {
        async beforeEach() {
            this.setPageObjects({
                votes: () => this.createPageObject(
                    Votes,
                    {
                        parent: this.questionCard,
                    }
                ),
            });
        },

        'При двух кликах на лайк подряд': {
            'количество лайков сначала увеличивается, а потом уменьшается': makeCase({
                async test() {
                    const beforeLikeCount = await this.votes.getLikeCount();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                    const afterFirstClickLikeCount = await this.votes.getLikeCount();

                    await this.expect(afterFirstClickLikeCount)
                        .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                    const afterSecondCLickLikeCount = await this.votes.getLikeCount();

                    await this.expect(afterSecondCLickLikeCount)
                        .to.be.lessThan(afterFirstClickLikeCount, 'лайков стало меньше');
                },
            }),
        },
    },
});
