import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на голосование за вопрос в списке вопросов
 * @property {PageObject.Votes} this.questionVotes
 */
export default makeSuite('Голосование за вопрос в списке вопросов. Если пользователь авторизован', {
    story: {
        'По умолчанию': {
            'При нажатии на лайк счетчик лайков увеличивается': makeCase({
                id: 'marketfront-3342',
                issue: 'MARKETVERSTKA-32994',
                async test() {
                    const currentCount = await this.questionVotes.getLikeCount();
                    const expectedCount = currentCount + 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionVotes.clickLike(),
                        valueGetter: () => this.questionVotes.getLikeCount(),
                    });
                    const nextLikeCount = await this.questionVotes.getLikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedCount, 'Количество лайков увеличилось');
                },
            }),
        },
        'Если лайк уже был поставлен': {
            'При нажатии на лайк счетчик лайков уменьшается': makeCase({
                id: 'marketfront-3343',
                issue: 'MARKETVERSTKA-32994',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionVotes.clickLike(),
                        valueGetter: () => this.questionVotes.getLikeCount(),
                    });

                    const currentCount = await this.questionVotes.getLikeCount();
                    const expectedCount = currentCount - 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionVotes.clickLike(),
                        valueGetter: () => this.questionVotes.getLikeCount(),
                    });
                    const nextLikeCount = await this.questionVotes.getLikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedCount, 'Количество лайков уменьшилось');
                },
            }),
        },
    },
});
