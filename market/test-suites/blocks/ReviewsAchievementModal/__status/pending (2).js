import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementModal (модерируемая ачивка).
 * @param {PageObject.ReviewsAchievementModal} reviewsAchievementModal
 */
export default makeSuite('Модальное окно модерируемой ачивки.', {
    environment: 'kadavr',
    story: {
        'если ачивка на модерирации': {
            'должно содержать полную информацию о модерируемой ачивке': makeCase({
                id: 'm-touch-2072',
                issue: 'MOBMARKET-7933',
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
                            .should.eventually.to.be.equal(true, 'Прогрессбар ачивки отображается'),
                        this.reviewsAchievementModal
                            .isAchievementReviewsLeftTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст оставшихся отзывов ачивки не отображается'),
                        this.reviewsAchievementModal
                            .isModerationTextVisible()
                            .should.eventually.to.be.equal(true, 'Текст о проверке отображается'),
                    ]);
                },
            }),
        },
    },
});
