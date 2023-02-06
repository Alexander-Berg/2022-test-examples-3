import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент UserAchievementsModal (заблокированная ачивка).
 * @param {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Модальное окно залоченной ачивки.', {
    environment: 'kadavr',
    story: {
        'если ачивка заблокирована': {
            'должно содержать полную информацию о залоченной ачивке': makeCase({
                id: 'marketfront-3949',
                async test() {
                    await this.userAchievementsModal.waitForVisible(
                        this.userAchievementsModal.achievementName,
                        1000
                    );

                    return Promise.all([
                        this.userAchievementsModal
                            .isAchievementImageVisible()
                            .should.eventually.to.be.equal(true, 'Изображение ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementNameVisible()
                            .should.eventually.to.be.equal(true, 'Название ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementDescriptionVisible()
                            .should.eventually.to.be.equal(true, 'Описание ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementProgressBarVisible()
                            .should.eventually.to.be.equal(true, 'Прогрессбар ачивки отображается'),
                        this.userAchievementsModal
                            .isAchievementReviewsLeftTextVisible()
                            .should.eventually.to.be.equal(true, 'Текст оставшихся отзывов ачивки отображается'),
                        this.userAchievementsModal
                            .isModerationTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст о проверке не отображается'),
                    ]);
                },
            }),
        },
    },
});
