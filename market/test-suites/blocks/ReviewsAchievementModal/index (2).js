import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementModal.
 * @param {PageObject.ReviewsAchievementModal} reviewsAchievementModal
 */
export default makeSuite('Модальное окно.', {
    params: {
        nextAchievementImageHref: 'Ссылка на изображение следующей ачивки',
        prevAchievementImageHref: 'Ссылка на изображение предыдущей ачивки',
    },
    story: {
        'Крестик закрытия': {
            'При нажатии': {
                'должeн закрывать окно': makeCase({
                    id: 'm-touch-2074',
                    issue: 'MOBMARKET-7936',
                    test() {
                        return this.browser.allure.runStep('Проверяем закрытие модального окна', () =>
                            this.reviewsAchievementModal.closeModal()
                                .then(() => this.reviewsAchievementModal.isVisible()
                                    .should.eventually.to.be.equal(false, 'Модальное окно не отображается')
                                )
                        );
                    },
                }),
            },
        },

        'Блок со стрелками.': {
            'При нажатии на правую стрелку': {
                'должен переключать на следующую ачивку': makeCase({
                    id: 'm-touch-2076',
                    issue: 'MOBMARKET-7938',
                    test() {
                        return this.browser.allure.runStep(
                            'Проверяем, что отображается изображение следующей ачивки',
                            () => this.reviewsAchievementModal.switchNextAchievement()
                                .then(() => this.reviewsAchievementModal.getAchievementImageSrc()
                                    .should.eventually.to.be.equal(
                                        this.params.nextAchievementImageHref,
                                        'Ачивка переключилась на следующую'
                                    )
                                ));
                    },
                }),
            },
            'При нажатии на левую стрелку': {
                'должен переключать на предыдущую ачивку': makeCase({
                    id: 'm-touch-2075',
                    issue: 'MOBMARKET-7937',
                    test() {
                        const {allure} = this.browser;

                        return allure.runStep('Проверяем, что отображается изображение предыдущей ачивки', () =>
                            this.reviewsAchievementModal.switchPrevAchievement()
                                .then(() => this.reviewsAchievementModal.getAchievementImage().getAttribute('src')
                                    .should.eventually.to.be.equal(
                                        this.params.prevAchievementImageHref,
                                        'Ачивка переключилась на предыдущую'
                                    )
                                ));
                    },
                }),
            },
        },
    },
});
