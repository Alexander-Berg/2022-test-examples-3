import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('и поставил лайк.', {
    story: {
        'При клике по лайку': {
            'cчетчик лайков уменьшается': makeCase({
                id: 'm-touch-2315',
                issue: 'MOBMARKET-9292',
                feature: 'Лайки/дизлайки',
                async test() {
                    const beforeLikeCount = await this.votes.getLikeCount();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                    const afterLikeCount = await this.votes.getLikeCount();
                    await this.expect(afterLikeCount)
                        .to.be.lessThan(beforeLikeCount, 'лайков стало меньше');

                    await this.browser.refresh();
                    await this.expect(afterLikeCount)
                        .to.be.lessThan(beforeLikeCount, 'лайков стало меньше');
                },
            }),
        },
        'При клике по дизлайку': {
            'счетчик лайков уменьшается а счетчик дизлайков увеличивается': makeCase({
                id: 'm-touch-2314',
                issue: 'MOBMARKET-9281',
                feature: 'Лайки/дизлайки',
                async test() {
                    const beforeLikeCount = await this.votes.getLikeCount();
                    const beforeDislikeCount = await this.votes.getDislikeCount();

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickDislike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                    const afterLikeCount = await this.votes.getLikeCount();
                    const afterDislikeCount = await this.votes.getDislikeCount();

                    await this.expect(afterLikeCount)
                        .to.be.lessThan(beforeLikeCount, 'лайков стало меньше');
                    await this.expect(afterDislikeCount)
                        .to.be.greaterThan(beforeDislikeCount, 'дизлайков стало больше');

                    await this.browser.refresh();
                    await this.expect(afterLikeCount)
                        .to.be.lessThan(beforeLikeCount, 'лайков стало меньше');
                    await this.expect(afterDislikeCount)
                        .to.be.greaterThan(beforeDislikeCount, 'дизлайков стало больше');
                },
            }),
        },
    },
});
