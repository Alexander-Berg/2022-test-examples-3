import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на закрытие модалки
 * @property {PageObject.UserAchievementsPage} userAchievementsPage
 * @property {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Модальное окно', {
    environment: 'testing',
    story: {
        'Крестик в правом верхнем углу': {
            'при нажатии': {
                'закрывает модальное окно': makeCase({
                    id: 'marketfront-2523',
                    issue: 'MARKETVERSTKA-29003',
                    test() {
                        return this.userAchievementsPage.clickOnFirstAchievement()
                            .then(() => this.userAchievementsModal.clickOnCloseButton())
                            .then(() => this.userAchievementsModal.isRootNodeExisting())
                            .should.eventually.be.equal(false, 'модальное окно исчезло со страницы');
                    },
                }),
            },
        },
    },
});
