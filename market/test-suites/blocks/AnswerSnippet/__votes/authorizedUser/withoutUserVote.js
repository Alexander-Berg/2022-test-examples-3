import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('без лайка или дизлайка пользователя.', {
    story: {
        'При клике по лайку': {
            'счетчик лайков увеличивается': makeCase({
                id: 'm-touch-2260',
                issue: 'MOBMARKET-9115',
                feature: 'Лайки/дизлайки',
                async test() {
                    const beforeLikeCount = await this.votes.getLikeCount();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                    const afterLikeCount = await this.votes.getLikeCount();
                    await this.expect(afterLikeCount)
                        .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');

                    await this.browser.refresh();
                    await this.expect(afterLikeCount)
                        .to.be.greaterThan(beforeLikeCount, 'лайков стало больше');
                },
            }),
        },
        'При клике по дизлайку': {
            'счетчик дизлайков увеличивается': makeCase({
                id: 'm-touch-2313',
                issue: 'MOBMARKET-9278',
                feature: 'Лайки/дизлайки',
                async test() {
                    const beforeDislikeCount = await this.votes.getDislikeCount();
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickDislike(),
                        valueGetter: () => this.votes.getDislikeCount(),
                    });
                    const afterDislikeCount = await this.votes.getDislikeCount();
                    await this.expect(afterDislikeCount)
                        .to.be.greaterThan(beforeDislikeCount, 'дизлайков стало больше');

                    await this.browser.refresh();
                    await this.expect(afterDislikeCount)
                        .to.be.greaterThan(beforeDislikeCount, 'дизлайков стало больше');
                },
            }),
        },
    },
});
