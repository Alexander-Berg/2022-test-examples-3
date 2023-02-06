import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент UserAchievementsModal (разблокированная ачивка).
 * @param {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Модальное окно разблокированной ачивки.', {
    environment: 'kadavr',
    story: {
        'Если ачивка разблокирована, то': {
            'должно содержать полную информацию о разблокированной ачивке': makeCase({
                id: 'marketfront-3956',
                async test() {
                    await this.userAchievementsModal.waitForVisible(
                        this.userAchievementsModal.achievementName,
                        1000
                    );

                    return Promise.all([
                        this.userAchievementsModal
                            .modalContentImage.isVisible()
                            .should.eventually.to.be.equal(true, 'Изображение ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementNameVisible()
                            .should.eventually.to.be.equal(true, 'Название ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementDescriptionVisible()
                            .should.eventually.to.be.equal(true, 'Описание ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementProgressBarVisible()
                            .should.eventually.to.be.equal(false, 'Прогрессбар ачивки не отображается'),
                        this.userAchievementsModal
                            .isAchievementReviewsLeftTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст оставшихся отзывов ачивки не отображается'),
                        this.userAchievementsModal
                            .isModerationTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст о проверке не отображается'),
                    ]);
                },
            }),
        },
    },
});
