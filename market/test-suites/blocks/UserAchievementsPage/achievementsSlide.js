import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на слайды в модалке влево/вправо
 * @property {PageObject.UserAchievementsPage} userAchievementsPage
 * @property {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Модальное окно с ачивками', {
    environment: 'testing',
    story: {
        'при клике стрелочки "вправо"': {
            'вместо текущей ачивки появляется следующая.': makeCase({
                id: 'marketfront-2524',
                issue: 'MARKETVERSTKA-29002',
                test() {
                    return this.userAchievementsPage
                        .clickOnFirstAchievement()
                        .then(() => this.userAchievementsModal.currentAchievementImageHref)
                        .then(initialAchievementHref =>
                            this.userAchievementsModal
                                .clickOnRightArrow()
                                .then(() => this.userAchievementsModal.currentAchievementImageHref)
                                .then(currentAchievementHref =>
                                    this.expect(currentAchievementHref)
                                        .not.to.be.equal(
                                            initialAchievementHref,
                                            'ссылка на изображение текущей ачивки поменялась'
                                        )
                                )
                        );
                },
            }),
        },
        'при клике стрелочки "вправо", затем "влево"': {
            'текущая ачивка вернется на свое место.': makeCase({
                id: 'marketfront-2539',
                issue: 'MARKETVERSTKA-29102',
                test() {
                    return this.userAchievementsPage
                        .clickOnFirstAchievement()
                        .then(() => this.userAchievementsModal.currentAchievementImageHref)
                        .then(initialAchievementHref =>
                            this.userAchievementsModal
                                .clickOnRightArrow()
                                .then(() => this.userAchievementsModal.clickOnLeftArrow())
                                .then(() => this.userAchievementsModal.currentAchievementImageHref)
                                .then(currentAchievementHref =>
                                    this.expect(currentAchievementHref)
                                        .to.be.equal(initialAchievementHref, 'текущая ачивка вернулась на место')
                                )
                        );
                },
            }),
        },
    },
});
