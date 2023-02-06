import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.Votes} votes
 */
export default makeSuite('Блок голосовалки', {
    issue: 'MARKETVERSTKA-32738',
    feature: 'Статья',
    story: {
        'по умолчанию': {
            'клик по лайку': {
                'увеличивает количество лайков': makeCase({
                    id: 'marketfront-3122',
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
                    id: 'marketfront-3260',
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
        'когда стоит лайк юзера': mergeSuites(
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
                        id: 'marketfront-3263',
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
                        id: 'marketfront-3261',
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
        'когда стоит дизлайк юзера': mergeSuites(
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
                        id: 'marketfront-3262',
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
                        id: 'marketfront-3264',
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
