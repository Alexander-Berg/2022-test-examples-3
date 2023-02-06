import {pluralize} from '@self/project/src/helpers/string';

import {makeSuite, makeCase} from 'ginny';
import ReviewsAchievementModal from '@self/platform/spec/page-objects/ReviewsAchievementModal';

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
                    id: 'm-touch-2044',
                    issue: 'MOBMARKET-7940',
                    test() {
                        return this.browser.allure.runStep('Проверяем адрес ссылки на "Достижения"', () => {
                            const achievevementsLinkHref = this.userReviewsAchievements.getAchievementsLinkHref();
                            const expectedAchievementsLink = this.params.login
                                ? this.browser.yaBuildURL('touch:user-achievements', {publicId: this.params.publicId})
                                : this.browser.yaBuildURL('touch:my-achievements');

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
                    id: 'm-touch-2043',
                    issue: 'MOBMARKET-7939',
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
                                        `${unlockedAchievementsText}\n${lockedAchievementsText}`,
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
                    reviewsAchievementModal: () => this.createPageObject(ReviewsAchievementModal),
                });
            },

            'При нажатии на ачивку': {
                'должно открываться модальное окно': makeCase({
                    id: 'm-touch-2082',
                    issue: 'MOBMARKET-7990',
                    test() {
                        return this.userReviewsAchievements
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(ReviewsAchievementModal.root, 1000))
                            .should.eventually.to.be.equal(true, 'Модальное окно отображается');
                    },
                }),
            },
        },
    },
});
