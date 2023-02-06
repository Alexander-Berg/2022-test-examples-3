import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на голосование за ответ
 * @property {PageObject.Votes} this.answerVotes
 */
export default makeSuite('Голосование за ответ. Если пользователь авторизован', {
    story: {
        'По умолчанию': {
            'При нажатии на лайк счетчик лайков увеличивается': makeCase({
                id: 'marketfront-2900',
                issue: 'MARKETVERSTKA-31285',
                async test() {
                    const currentCount = await this.answerVotes.getLikeCount();
                    const expectedCount = currentCount + 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickLike(),
                        valueGetter: () => this.answerVotes.getLikeCount(),
                    });
                    const nextLikeCount = await this.answerVotes.getLikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedCount, 'Количество лайков увеличилось');
                },
            }),

            'При нажатии на дизлайк счетчик дизлайков увеличивается': makeCase({
                id: 'marketfront-2901',
                issue: 'MARKETVERSTKA-31286',
                async test() {
                    const currentCount = await this.answerVotes.getDislikeCount();
                    const expectedCount = currentCount + 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickDislike(),
                        valueGetter: () => this.answerVotes.getDislikeCount(),
                    });
                    const nextLikeCount = await this.answerVotes.getDislikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedCount, 'Количество дизлайков увеличилось');
                },
            }),
        },
        'Если лайк уже был поставлен': {
            'При нажатии на лайк счетчик лайков уменьшается': makeCase({
                id: 'marketfront-2902',
                issue: 'MARKETVERSTKA-31288',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickLike(),
                        valueGetter: () => this.answerVotes.getLikeCount(),
                    });

                    const currentCount = await this.answerVotes.getLikeCount();
                    const expectedCount = currentCount - 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickLike(),
                        valueGetter: () => this.answerVotes.getLikeCount(),
                    });
                    const nextLikeCount = await this.answerVotes.getLikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedCount, 'Количество лайков уменьшилось');
                },
            }),
        },
        'Если дизлайк уже был поставлен': {
            'При нажатии на дизлайк счетчик дизлайков уменьшается': makeCase({
                id: 'marketfront-2903',
                issue: 'MARKETVERSTKA-31289',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickDislike(),
                        valueGetter: () => this.answerVotes.getDislikeCount(),
                    });

                    const currentCount = await this.answerVotes.getDislikeCount();
                    const expectedCount = currentCount - 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickDislike(),
                        valueGetter: () => this.answerVotes.getDislikeCount(),
                    });
                    const nextDislikeCount = await this.answerVotes.getDislikeCount();
                    return this.expect(nextDislikeCount)
                        .to.equal(expectedCount, 'Количество дизлайков уменьшилось');
                },
            }),
        },
        'Если лайк уже был поставлен, при нажатии на дизлайк': {
            'Счетчик лайков уменьшается, а счетчик дизлайков увеличивается': makeCase({
                id: 'marketfront-2904',
                issue: 'MARKETVERSTKA-31290',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickLike(),
                        valueGetter: () => this.answerVotes.getLikeCount(),
                    });
                    const currentLikeCount = await this.answerVotes.getLikeCount();
                    const expectedUpvoteCount = currentLikeCount - 1;
                    const currentDislikeCount = await this.answerVotes.getDislikeCount();
                    const expectedDislikeCount = currentDislikeCount + 1;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickDislike(),
                        valueGetter: () => this.answerVotes.getDislikeCount(),
                    });
                    await this.expect(this.answerVotes.getLikeCount())
                        .to.equal(expectedUpvoteCount, 'Количество лайков уменьшилось');
                    return this.expect(this.answerVotes.getDislikeCount())
                        .to.equal(expectedDislikeCount, 'Количество дизлайков увеличилось');
                },
            }),
        },

        'Если дизлайк уже был поставлен, при нажатии на лайк': {
            'Счетчик дизлайков уменьшается, а счетчик лайков увеличивается': makeCase({
                id: 'marketfront-2905',
                issue: 'MARKETVERSTKA-31291',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickDislike(),
                        valueGetter: () => this.answerVotes.getDislikeCount(),
                    });

                    const currentLikeCount = await this.answerVotes.getLikeCount();
                    const expectedLikeCount = currentLikeCount + 1;
                    const currentDislikeCount = await this.answerVotes.getDislikeCount();
                    const expectedDislikeCount = currentDislikeCount - 1;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.answerVotes.clickLike(),
                        valueGetter: () => this.answerVotes.getLikeCount(),
                    });
                    await this.expect(this.answerVotes.getLikeCount())
                        .to.equal(expectedLikeCount, 'Количество лайков увеличилось');
                    return this.expect(this.answerVotes.getDislikeCount())
                        .to.equal(expectedDislikeCount, 'Количество дизлайков уменьшилось');
                },
            }),
        },
    },
});
