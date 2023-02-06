import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementModal (заблокированная ачивка).
 * @param {PageObject.ReviewsAchievementModal} reviewsAchievementModal
 */
export default makeSuite('Модальное окно залоченной ачивки.', {
    environment: 'kadavr',
    story: {
        'если ачивка заблокирована': {
            'должно содержать полную информацию о залоченной ачивке': makeCase({
                id: 'm-touch-2071',
                issue: 'MOBMARKET-7932',
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
                            .should.eventually.to.be.equal(true, 'Текст оставшихся отзывов ачивки отображается'),
                        this.reviewsAchievementModal
                            .isModerationTextVisible()
                            .should.eventually.to.be.equal(false, 'Текст о проверке не отображается'),
                    ]);
                },
            }),
        },
    },
});
