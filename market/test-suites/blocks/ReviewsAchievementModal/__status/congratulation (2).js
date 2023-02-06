import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementModal (вручение ачивки).
 * @param {PageObject.ReviewsAchievementModal} reviewsAchievementModal
 */
export default makeSuite('Модальное окно вручения ачивки (есть ачивки).', {
    params: {
        expectedTitle: 'Ожидаемый заголовок вручения',
        expectedName: 'Ожидаемое имя ачивки',
        expectedDescription: 'Ожидаемое описание ачивки',
    },
    story: {
        'если у пользователя есть новые ачивки': {
            'должно содержать заголовок о получении нового достижения': makeCase({
                id: 'm-touch-2108',
                issue: 'MOBMARKET-8121',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем заголовок о получении нового достижения', () =>
                        this.reviewsAchievementModal
                            .getCongratulationTitleText()
                            .should.eventually.to.be.equal(this.params.expectedTitle, 'Заголовок корректный')
                    );
                },
            }),

            'должно отображать название заработанной ачивки': makeCase({
                id: 'm-touch-2109',
                issue: 'MOBMARKET-8122',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем название заработанной ачивки', () =>
                        this.reviewsAchievementModal
                            .getAchievementNameText()
                            .should.eventually.to.be.equal(this.params.expectedName, 'Название корректное')
                    );
                },
            }),

            'должно отображать описание заработанной ачивки': makeCase({
                id: 'm-touch-2110',
                issue: 'MOBMARKET-8128',
                async test() {
                    const {allure} = this.browser;
                    await this.browser.yaWaitForChangeValue({
                        action: () => Promise.resolve(null),
                        valueGetter: () => this.reviewsAchievementModal.getAchievementDescriptionText(),
                        compareFunction: (initialValue, nextValue) => initialValue !== '' || nextValue !== '',
                        errorDescribe: 'Описание заработанной ачивки не обновилось',
                    });
                    return allure.runStep('Проверяем название заработанной ачивки', () =>
                        this.reviewsAchievementModal
                            .getAchievementDescriptionText()
                            .should.eventually.to.be.equal(this.params.expectedDescription, 'Описание корректное')
                    );
                },
            }),

            'должно закрываться по клику на крестик': makeCase({
                id: 'm-touch-2111',
                issue: 'MOBMARKET-8131',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверям закрытие модального окна', () =>
                        this.reviewsAchievementModal.closeModal()
                            .then(() => this.reviewsAchievementModal.isVisible()
                                .should.eventually.to.be.equal(false, 'Модальное окно не отображается')
                            )
                    );
                },
            }),
        },
    },
});
