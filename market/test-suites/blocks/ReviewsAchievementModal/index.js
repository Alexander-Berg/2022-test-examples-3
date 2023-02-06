import {makeSuite, makeCase} from 'ginny';
import UserAchievementsModal from '@self/platform/spec/page-objects/UserAchievementsModal';

/**
 * Тесты на компонент UserAchievementsModal.
 * @param {PageObject.UserAchievementsModal} userAchievementsModal
 */
export default makeSuite('Модальное окно.', {
    story: {
        'Крестик закрытия': {
            'При нажатии': {
                'должeн закрывать окно': makeCase({
                    id: 'marketfront-3942',
                    async test() {
                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userAchievementsModal.clickOnCloseButton(),
                            valueGetter: () => this.browser.isExisting(UserAchievementsModal.root),
                        });

                        return this.browser.allure.runStep('Проверяем закрытие модального окна', () =>
                            this.browser.isExisting(UserAchievementsModal.root)
                                .should.eventually.to.be.equal(false, 'Модальное окно не отображается')
                        );
                    },
                }),
            },
        },

        'Блок со стрелками.': {
            'При нажатии на правую стрелку': {
                'должен переключать на следующую ачивку': makeCase({
                    id: 'marketfront-3943',
                    async test() {
                        const expectedNextAchievementImageHref = await this.userAchievementsModal
                            .getNextHiddenAchievement();

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userAchievementsModal.clickOnRightArrow(),
                            valueGetter: () => this.userAchievementsModal.currentAchievementImageHref,
                            timeout: 5000,
                        });

                        const {allure} = this.browser;

                        return allure.runStep('Проверяем, что отображается изображение следующей ачивки', () =>
                            this.userAchievementsModal.currentAchievementImageHref
                                .should.eventually.to.be.equal(
                                    expectedNextAchievementImageHref,
                                    'Ачивка переключилась на следующюю'
                                )
                        );
                    },
                }),
            },
            'При нажатии на левую стрелку': {
                'должен переключать на предыдущую ачивку': makeCase({
                    id: 'marketfront-3944',
                    async test() {
                        const expectedPrevAchievementImageHref = await this.userAchievementsModal
                            .getPrevHiddenAchievement();

                        const {allure} = this.browser;

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userAchievementsModal.clickOnLeftArrow(),
                            valueGetter: () => this.userAchievementsModal.currentAchievementImageHref,
                            timeout: 5000,
                        });

                        return allure.runStep('Проверяем, что отображается изображение предыдущей ачивки', () =>
                            this.userAchievementsModal.currentAchievementImageHref
                                .should.eventually.to.be.equal(
                                    expectedPrevAchievementImageHref,
                                    'Ачивка переключилась на предыдущую'
                                )
                        );
                    },
                }),
            },
        },
    },
});
