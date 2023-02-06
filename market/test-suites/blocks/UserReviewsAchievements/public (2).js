import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент UserReviewsAchievements чужого пользователя.
 * @param {PageObject.UserReviewsAchievements} userReviewsAchievements
 * @param {PageObject.LockIcon} lockIcon
 */
export default makeSuite('Ачивки за отзывы чужого пользователя.', {
    environment: 'kadavr',
    story: {
        'Список ачивок': {
            'для чужого пользователя': {
                'должен содержать только достигнутые': makeCase({
                    id: 'm-touch-2045',
                    issue: 'MOBMARKET-7941',
                    params: {
                        count: 'Ожидаемое количество видимых ачивок',
                    },
                    async test() {
                        await this.userReviewsAchievements.getAchievementsCount()
                            .should.eventually.to.be.equal(this.params.count, 'Проверяем количество видимых ачивок');
                        await this.lockIcon.isVisible()
                            .should.eventually.to.be.equal(false, 'Иконка замка не должна отображаться');
                    },
                }),
            },
        },
    },
});
