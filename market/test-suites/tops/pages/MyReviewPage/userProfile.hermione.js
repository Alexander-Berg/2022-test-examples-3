import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import UserReviewsAchievementsSuite from '@self/platform/spec/hermione/test-suites/blocks/UserReviewsAchievements';
import ReviewsAchievementModalSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal';
import StatusCongratulationSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/congratulation';
import StatusNonCongratulationSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/nonCongratulation';
import StatusUnlockedSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/unlocked';
import StatusPendingSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/pending';
import UserReviewsAchievementsPendingSuite from '@self/platform/spec/hermione/test-suites/blocks/UserReviewsAchievements/pending';
import UserReviewsAchievementsUnlockedSuite from '@self/platform/spec/hermione/test-suites/blocks/UserReviewsAchievements/unlocked';

// page-objects
import UserAchievementsModal from '@self/platform/spec/page-objects/UserAchievementsModal';
import UserReviewsAchievements from '@self/platform/widgets/content/UserInfo/Achievements/__pageObject';
import LockIcon from '@self/platform/components/Icon/IconLock/__pageObject';

const userUid = '636368980';
const userSchema = {
    id: userUid,
    uid: {
        value: userUid,
    },
    login: 'ugctest3',
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
    dbfields: {
        'userinfo.firstname.uid': 'Willy',
        'userinfo.lastname.uid': 'Wonka',
    },
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Личный кабинет. Сниппет автора.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-7443',
    story: {
        'Ачивки.': mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        userReviewsAchievements: () => this.createPageObject(UserReviewsAchievements),
                        userAchievementsModal: () => this.createPageObject(UserAchievementsModal),
                    });
                },
            },
            prepareSuite(UserReviewsAchievementsSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 1,
                                    pendingEventsCount: 0,
                                }],
                            },
                        });
                        return this.browser.yaProfile('ugctest3', 'market:my-reviews');
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                params: {
                    unlockedAchievementsCount: 1,
                },
            }),
            prepareSuite(ReviewsAchievementModalSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 1,
                                    pendingEventsCount: 0,
                                }, {
                                    achievementId: 1,
                                    confirmedEventsCount: 50,
                                    pendingEventsCount: 0,
                                }],
                            },
                        });
                        await this.browser.yaProfile('ugctest3', 'market:my-reviews');

                        await this.browser.yaWaitForChangeValue({
                            action: () => this.userReviewsAchievements
                                .clickNthAchievement(2),
                            valueGetter: () => this.browser.isVisible(UserAchievementsModal.root),
                        });

                        // eslint-disable-next-line market/ginny/no-pause
                        return this.browser.pause(1000);
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
            }),
            prepareSuite(StatusCongratulationSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 0,
                                    pendingEventsCount: 1,
                                }],
                            },
                            gradeAchievements: {
                                '59795923': [{
                                    achievementId: 0,
                                    confirmedEventsCount: 0,
                                    pendingEventsCount: 1,
                                }],
                            },
                        });

                        await this.browser.yaProfile('ugctest3', 'market:my-reviews', {
                            gradeId: 59795923,
                        });

                        return this.userAchievementsModal.waitForVisible();
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                params: {
                    expectedName: 'Новобранец',
                    expectedTitle: 'У вас новое достижение',
                    expectedDescription: 'Выдаётся за первый отзыв на Маркете',
                },
            }),
            prepareSuite(StatusNonCongratulationSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 1,
                                    pendingEventsCount: 0,
                                }],
                            },
                        });

                        await this.browser.yaProfile('ugctest3', 'market:my-reviews', {
                            gradeId: 12345678,
                        });

                        return this.browser.yaWaitForPageReady();
                    },
                    afterEach() {
                        return this.browser.yaLogout();
                    },
                },
            }),
            prepareSuite(StatusUnlockedSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 1,
                                    pendingEventsCount: 0,
                                }],
                            },
                        });
                        await this.browser.yaProfile('ugctest3', 'market:my-reviews');

                        return this.userReviewsAchievements
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(UserAchievementsModal.root, 2000));
                    },
                    async afterEach() {
                        return this.browser.yaLogout();
                    },
                },
            }),
            prepareSuite(StatusPendingSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 0,
                                    pendingEventsCount: 1,
                                }],
                            },
                        });

                        await this.browser.yaProfile('ugctest3', 'market:my-reviews');

                        return this.userReviewsAchievements
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(UserAchievementsModal.root, 2000));
                    },
                    async afterEach() {
                        return this.browser.yaLogout();
                    },
                },
            }),
            prepareSuite(UserReviewsAchievementsPendingSuite, {
                pageObjects: {
                    lockIcon() {
                        return this.createPageObject(LockIcon, {
                            parent: this.userReviewsAchievements,
                        });
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 0,
                                    pendingEventsCount: 1,
                                }],
                            },
                        });

                        return this.browser.yaProfile('ugctest3', 'market:my-reviews');
                    },
                    async afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                params: {
                    count: 1,
                },
            }),
            prepareSuite(UserReviewsAchievementsUnlockedSuite, {
                pageObjects: {
                    lockIcon() {
                        return this.createPageObject(LockIcon, {
                            parent: this.userReviewsAchievements,
                        });
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 1,
                                    pendingEventsCount: 0,
                                }],
                            },
                        });
                        return this.browser.yaProfile('ugctest3', 'market:my-reviews');
                    },
                    async afterEach() {
                        return this.browser.yaLogout();
                    },
                },
                params: {
                    count: 1,
                },
            })
        ),
    },
});
