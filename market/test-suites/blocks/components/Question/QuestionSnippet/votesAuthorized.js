import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на отображение лайк, если пользователь авторизован
 * @property {PageObject.Votes} this.questionVotes
 */
export default makeSuite('Лайк вопроса. Если пользователь авторизован', {
    id: 'marketfront-2886',
    issue: 'MARKETVERSTKA-31287',
    environment: 'kadavr',
    feature: 'Лайки/дизлайки',
    story: {
        'При клике на лайк': {
            'каунтер увеличивается на 1': makeCase({
                async test() {
                    const currentLikeCount = await this.questionVotes.getLikeCount();
                    const expectedLikeCount = currentLikeCount + 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionVotes.clickLike(),
                        valueGetter: () => this.questionVotes.getLikeCount(),
                    });
                    const nextLikeCount = await this.questionVotes.getLikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedLikeCount, 'Количество лайков увеличилось');
                },
            }),
        },
        'При клике на лайк, когда стоит лайк': {
            'каунтер уменьшается на 1': makeCase({
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionVotes.clickLike(),
                        valueGetter: () => this.questionVotes.getLikeCount(),
                    });

                    const currentLikeCount = await this.questionVotes.getLikeCount();
                    const expectedLikeCount = currentLikeCount - 1;
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.questionVotes.clickLike(),
                        valueGetter: () => this.questionVotes.getLikeCount(),
                    });
                    const nextLikeCount = await this.questionVotes.getLikeCount();
                    return this.expect(nextLikeCount)
                        .to.equal(expectedLikeCount, 'Количество лайков уменьшилось');
                },
            }),
        },
    },
});
