import {pluralize} from '@self/project/src/helpers/string';

import {makeSuite, makeCase} from 'ginny';
import UserAchievementsModal from '@self/platform/spec/page-objects/UserAchievementsModal';

/**
 * Тесты на компонент UserReviewsAchievements.
 * @param {PageObject.UserReviewsAchievements} userReviewsAchievements
 */
export default makeSuite('Ачивки за отзывы.', {
    params: {
        login: 'Логин пользователя, чьи ачивки просматривает текущий пользователь',
        publicId: 'public_id пользователя, чьи ачивки просматривает текущий пользователь',
        totalAchievementsCount: 'Число всех доступных ачивок',
        unlockedAchievementsCount: 'Число разблокированных пользователем ачивок',
    },
    defaultParams: {
        login: false, // свои ачивки
        totalAchievementsCount: 5, // число совпадет с дефолтным количеством ачивок, замоканным на стороне кадавра
    },
    story: {
        'Cсылка "Достижения".': {
            'По умолчанию': {
                'должна вести на страницу всех ачивок.': makeCase({
                    id: 'marketfront-3952',
                    test() {
                        return this.browser.allure.runStep('Проверяем адрес ссылки на "Достижения"', () => {
                            const achievevementsLinkHref = this.userReviewsAchievements.getAchievementsLinkHref();
                            const expectedAchievementsLink = this.params.login
                                ? this.browser.yaBuildURL('market:user-achievements', {publicId: this.params.publicId})
                                : this.browser.yaBuildURL('market:my-achievements');

                            return Promise.all([achievevementsLinkHref, expectedAchievementsLink])
                                .then(([actualHref, expectedUrl]) => this.expect(actualHref, 'ссылка корректная')
                                    .to.be.link({pathname: expectedUrl}, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                                );
                        });
                    },
                }),

                'должна отображать количество достигнутых ачивок': makeCase({
                    id: 'marketfront-3953',
                    test() {
                        return this.browser.allure.runStep('Проверяем количество достигнутых ачивок в тексте ссылки',
                            () => {
                                const {
                                    totalAchievementsCount,
                                    unlockedAchievementsCount,
                                } = this.params;

                                const lockedAchievementsCount = totalAchievementsCount - unlockedAchievementsCount;

                                const pluralUnlockedAchievementsCount = pluralize(
                                    unlockedAchievementsCount,
                                    'достижение', 'достижения', 'достижений');
                                const pluralLockedAchievementsCount = pluralize(
                                    lockedAchievementsCount,
                                    'открыто', 'открыты', 'открыты');

                                const unlockedAchievementsText = this.params.login
                                    ? `У пользователя ${unlockedAchievementsCount} ${pluralUnlockedAchievementsCount}`
                                    : `У вас ${unlockedAchievementsCount} ${pluralUnlockedAchievementsCount}`;
                                const lockedAchievementsText =
                                    `Ещё ${lockedAchievementsCount} не ${pluralLockedAchievementsCount}`;

                                return this.userReviewsAchievements
                                    .getAchievementsCountTitleText()
                                    .should.eventually.to.be.equal(
                                        `${unlockedAchievementsText}. ${lockedAchievementsText}`,
                                        'Заголовок количества ачивок отображается корректно'
                                    );
                            }
                        );
                    },
                }),
            },
        },

        'Ачивка пользователя.': {
            beforeEach() {
                this.setPageObjects({
                    userAchievementsModal: () => this.createPageObject(UserAchievementsModal),
                });
            },

            'При нажатии на ачивку': {
                'должно открываться модальное окно': makeCase({
                    id: 'marketfront-3954',
                    test() {
                        return this.userReviewsAchievements
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(UserAchievementsModal.root, 1000))
                            .should.eventually.to.be.equal(true, 'Модальное окно отображается');
                    },
                }),
            },
        },
    },
});
