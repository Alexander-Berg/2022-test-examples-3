import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на открытие модального окна
 * @property {PageObject.UserAchievementsPage} userAchievementsPage
 * @property {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Блок ачивок', {
    environment: 'testing',
    story: {
        'при клике по изображению достижения': {
            'открывается модальное окно с этим достижением.': makeCase({
                id: 'marketfront-2498',
                issue: 'MARKETVERSTKA-28950',
                test() {
                    return this.userAchievementsPage.clickOnFirstAchievement()
                        .then(() => this.userAchievementsModal.isRootNodeExisting())
                        .should.eventually.be.equal(true, 'модалка появилась на странице');
                },
            }),
        },
    },
});
