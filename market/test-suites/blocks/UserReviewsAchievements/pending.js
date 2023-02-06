import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент UserReviewsAchievements текущего пользователя.
 * @param {PageObject.UserReviewsAchievements} userReviewsAchievements
 * @param {PageObject.LockIcon} lockIcon
 */
export default makeSuite('Модерируемые ачивки за отзывы текущего пользователя.', {
    environment: 'kadavr',
    story: {
        'Список ачивок': {
            'для текущего пользователя': {
                'должен отображать модерируемые': makeCase({
                    id: 'marketfront-3955',
                    params: {
                        count: 'Ожидаемое количество видимых ачивок',
                    },
                    async test() {
                        const {count} = this.params;
                        return Promise.all([
                            this.userReviewsAchievements.getAchievementsCount()
                                .should.eventually.to.be.equal(count, 'Количество видимых ачивок должно быть верным'),
                            this.lockIcon.isVisible()
                                .should.eventually.to.be.equal(true, 'Иконка замка должна отображаться'),
                        ]);
                    },
                }),
            },
        },
    },
});
