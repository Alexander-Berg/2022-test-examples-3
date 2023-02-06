import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент UserAchievementsModal (без вручения ачивки).
 * @param {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Модальное окно вручения ачивки (нет ачивок).', {
    story: {
        'если у пользователя нет новых ачивок': {
            'не должно отображаться': makeCase({
                id: 'marketfront-3950',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем наличие модального окна', () =>
                        this.userAchievementsModal
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Модальное окно не появляется')
                    );
                },
            }),
        },
    },
});
