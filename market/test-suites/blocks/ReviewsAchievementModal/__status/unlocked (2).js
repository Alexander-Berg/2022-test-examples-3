import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementModal (разблокированная ачивка).
 * @param {PageObject.ReviewsAchievementModal} reviewsAchievementModal
 */
export default makeSuite('Модальное окно разблокированной ачивки.', {
    environment: 'kadavr',
    story: {
        'если ачивка разблокирована': {
            'должно содержать полную информацию о разблокированной ачивке': makeCase({
                id: 'm-touch-2073',
                issue: 'MOBMARKET-7934',
                async test() {
                    await this.reviewsAchievementModal.waitForVisible(
                        this.reviewsAchievementModal.achievementName,
                        1000
                    );

                    return Promise.all([
                        this.reviewsAchievementModal
                            .isAchievementImageVisible()
                            .should.eventually.to.be.equal(true, 'Изображение ачивки отображается'),
                        this.reviewsAchievementModal
                            .isAchievementNameVisible()
                            .should.eventually.to.be.equal(true, 'Название ачивки отображается'),
                        this.reviewsAchievementModal
                            .isAchievementDescriptionVisible()
                            .should.eventually.to.be.equal(true, 'Описание ачивки отображается'),
                        this.reviewsAchievementModal
                            .isAchievementProgressBarVisible()
                            .should.eventually.to.be.equal(false, 'Прогрессбар ачивки не отображается'),
                        this.reviewsAchievementModal
                            .isAchievementReviewsLeftTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст оставшихся отзывов ачивки не отображается'),
                        this.reviewsAchievementModal
                            .isModerationTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст о проверке не отображается'),
                    ]);
                },
            }),
        },
    },
});
