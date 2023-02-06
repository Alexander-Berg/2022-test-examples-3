import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ReviewsAchievementModal (без вручения ачивки).
 * @param {PageObject.ReviewsAchievementModal} reviewsAchievementModal
 */
export default makeSuite('Модальное окно вручения ачивки (нет ачивок).', {
    story: {
        'если у пользователя нет новых ачивок': {
            'не должно отображаться': makeCase({
                id: 'm-touch-2115',
                issue: 'MOBMARKET-8133',
                test() {
                    const {allure} = this.browser;

                    return allure.runStep('Проверяем наличие модального окна', () =>
                        this.reviewsAchievementModal
                            .isVisible()
                            .should.eventually.to.be.equal(false, 'Модальное окно не появляется')
                    );
                },
            }),
        },
    },
});
