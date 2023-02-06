import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('и поставил дизлайк.', {
    story: {
        'При клике по дизлайку': {
            'счетчик дизлайков уменьшается': makeCase({
                id: 'm-touch-2316',
                issue: 'MOBMARKET-9291',
                feature: 'Лайки/дизлайки',
                async test() {
                    const beforeDislikeCount = await this.votes.getDislikeCount();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickDislike(),
                        valueGetter: () => this.votes.getDislikeCount(),
                    });
                    const afterDislikeCount = await this.votes.getDislikeCount();
                    await this.expect(afterDislikeCount)
                        .to.be.lessThan(beforeDislikeCount, 'дизлайков стало меньше');

                    await this.browser.refresh();
                    await this.expect(afterDislikeCount)
                        .to.be.lessThan(beforeDislikeCount, 'дизлайков стало меньше');
                },
            }),
        },
        'При клике по лайку': {
            'счетчик дизлайков уменьшается а счетчик лайков увеличивается': makeCase({
                id: 'm-touch-2317',
                issue: 'MOBMARKET-9300',
                feature: 'Лайки/дизлайки',
                async test() {
                    const beforeDislikeCount = await this.votes.getDislikeCount();
                    const beforeLikeCount = await this.votes.getLikeCount();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getDislikeCount(),
                    });
                    const afterDisikeCount = await this.votes.getDislikeCount();
                    const afterLikeCount = await this.votes.getLikeCount();

                    await this.expect(afterDisikeCount)
                        .to.be.lessThan(beforeDislikeCount, 'дизлайков стало меньше');
                    await this.expect(afterLikeCount)
                        .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');

                    await this.browser.refresh();
                    await this.expect(afterDisikeCount)
                        .to.be.lessThan(beforeDislikeCount, 'дизлайков стало меньше');
                    await this.expect(afterLikeCount)
                        .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');
                },
            }),
        },
    },
});
