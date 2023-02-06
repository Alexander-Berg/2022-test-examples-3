import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок голосовалки', {
    issue: 'MOBMARKET-9917',
    story: {
        'По умолчанию': {
            'клик по лайку': {
                'увеличивает количество лайков': makeCase({
                    id: 'm-touch-2930',
                    async test() {
                        const likeCount = await this.votes.getLikeCount();
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.votes.clickLike(),
                            valueGetter: () => this.votes.getLikeCount(),
                        });
                        const nextLikeCount = await this.votes.getLikeCount();
                        return this.expect(nextLikeCount)
                            .to.equal(likeCount + 1, 'Количество лайков увеличилось');
                    },
                }),
            },
            'клик по дизлайку': {
                'увеличивает количество дизлайков': makeCase({
                    id: 'm-touch-2931',
                    async test() {
                        const dislikeCount = await this.votes.getDislikeCount();
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.votes.clickDislike(),
                            valueGetter: () => this.votes.getDislikeCount(),
                        });
                        const nextDislikeCount = await this.votes.getDislikeCount();
                        return this.expect(nextDislikeCount)
                            .to.equal(dislikeCount + 1, 'Количество дизлайков увеличилось');
                    },
                }),
            },
        },
        'Когда стоит лайк юзера': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickLike(),
                        valueGetter: () => this.votes.getLikeCount(),
                    });
                },
            },
            {
                'клик по лайку': {
                    'уменьшает количество лайков': makeCase({
                        id: 'm-touch-2934',
                        async test() {
                            const likeCount = await this.votes.getLikeCount();
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickLike(),
                                valueGetter: () => this.votes.getLikeCount(),
                            });
                            const nextLikeCount = await this.votes.getLikeCount();
                            return this.expect(nextLikeCount)
                                .to.equal(likeCount - 1, 'Количество лайков уменьшилось');
                        },
                    }),
                },
                'клик по дизлайку': {
                    'уменьшает количество лайков и увеличивает количество дизлайков': makeCase({
                        id: 'm-touch-2932',
                        async test() {
                            const likeCount = await this.votes.getLikeCount();
                            const dislikeCount = await this.votes.getDislikeCount();
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickDislike(),
                                valueGetter: () => this.votes.getLikeCount(),
                            });
                            const nextLikeCount = await this.votes.getLikeCount();
                            const nextDislikeCount = await this.votes.getDislikeCount();
                            await this.expect(nextLikeCount)
                                .to.equal(likeCount - 1, 'Количество лайков уменьшилось');
                            return this.expect(nextDislikeCount)
                                .to.equal(dislikeCount + 1, 'Количество дизлайков увеличилось');
                        },
                    }),
                },
            }
        ),
        'Когда стоит дизлайк юзера': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.votes.clickDislike(),
                        valueGetter: () => this.votes.getDislikeCount(),
                    });
                },
            },
            {
                'клик по лайку': {
                    'уменьшает количество дизлайков и увеличивает количество лайков': makeCase({
                        id: 'm-touch-2933',
                        async test() {
                            const likeCount = await this.votes.getLikeCount();
                            const dislikeCount = await this.votes.getDislikeCount();
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickLike(),
                                valueGetter: () => this.votes.getDislikeCount(),
                            });
                            const nextLikeCount = await this.votes.getLikeCount();
                            const nextDislikeCount = await this.votes.getDislikeCount();
                            await this.expect(nextDislikeCount)
                                .to.equal(dislikeCount - 1, 'Количество дизлайков уменьшилось');
                            return this.expect(nextLikeCount)
                                .to.equal(likeCount + 1, 'Количество лайков увеличилось');
                        },
                    }),
                },
                'клик по дизлайку': {
                    'уменьшает количество дизлайков': makeCase({
                        id: 'm-touch-2935',
                        async test() {
                            const dislikeCount = await this.votes.getDislikeCount();
                            await this.browser.yaWaitForChangeValue({
                                action: () => this.votes.clickLike(),
                                valueGetter: () => this.votes.getLikeCount(),
                            });
                            const nextDislikeCount = await this.votes.getDislikeCount();
                            return this.expect(nextDislikeCount)
                                .to.equal(dislikeCount - 1, 'Количество дизлайков уменьшилось');
                        },
                    }),
                },
            }
        ),
    },
});
