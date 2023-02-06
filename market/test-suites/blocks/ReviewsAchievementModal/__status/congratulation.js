import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент UserAchievementsModal (вручение ачивки).
 * @param {PageObject.UserAchievementsModal} userAchievementsModal
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
                id: 'marketfront-3945',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем заголовок о получении нового достижения', () =>
                        this.userAchievementsModal
                            .getCongratulationTitleText()
                            .should.eventually.to.be.equal(this.params.expectedTitle, 'Заголовок корректный')
                    );
                },
            }),

            'должно отображать название заработанной ачивки': makeCase({
                id: 'marketfront-3946',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем название заработанной ачивки', () =>
                        this.userAchievementsModal
                            .getAchievementNameText()
                            .should.eventually.to.be.equal(this.params.expectedName, 'Название корректное')
                    );
                },
            }),

            'должно отображать описание заработанной ачивки': makeCase({
                id: 'marketfront-3947',
                async test() {
                    const {allure} = this.browser;
                    await this.browser.yaWaitForChangeValue({
                        action: () => Promise.resolve(null),
                        valueGetter: () => this.userAchievementsModal.getAchievementDescriptionText(),
                        compareFunction: (initialValue, nextValue) => initialValue !== '' || nextValue !== '',
                        errorDescribe: 'Описание заработанной ачивки не обновилось',
                    });
                    return allure.runStep('Проверяем название заработанной ачивки', () =>
                        this.userAchievementsModal
                            .getAchievementDescriptionText()
                            .should.eventually.to.be.equal(this.params.expectedDescription, 'Описание корректное')
                    );
                },
            }),

            'должно закрываться по клику на крестик': makeCase({
                id: 'marketfront-3948',
                async test() {
                    const {allure} = this.browser;

                    // eslint-disable-next-line market/ginny/no-pause
                    await this.browser.pause(1000);

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userAchievementsModal.clickOnCloseButton(),
                        valueGetter: () => this.userAchievementsModal.isExisting(),
                    });

                    return allure.runStep('Проверям закрытие модального окна', () =>
                        this.userAchievementsModal.isExisting()
                            .should.eventually.to.be.equal(false, 'Модальное окно не отображается')
                    );
                },
            }),
        },
    },
});
