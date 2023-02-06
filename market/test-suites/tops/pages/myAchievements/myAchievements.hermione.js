import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import StatusLockedSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/locked';
import StatusUnlockedSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/unlocked';
import StatusPendingSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/pending';
import ReviewsAchievementsListSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementsList';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';
import ReviewsAchievementsList from '@self/platform/spec/page-objects/ReviewsAchievementsList';
import ReviewsAchievementModal from '@self/platform/spec/page-objects/ReviewsAchievementModal';

const userUid = '636368980';
const publicId = 'z1z2x3x4c5c';
const userSchema = {
    id: userUid,
    uid: {
        value: userUid,
    },
    login: 'ugctest3',
    public_id: publicId,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Мои достижения".', {
    environment: 'kadavr',
    story: {
        'Модальное окно достижения.': mergeSuites(
            {
                async beforeEach() {
                    this.setPageObjects({
                        reviewsAchievementsList: () => this.createPageObject(ReviewsAchievementsList),
                        reviewsAchievementModal: () => this.createPageObject(ReviewsAchievementModal),
                    });
                },
            },
            prepareSuite(StatusLockedSuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('schema', {
                            users: [userSchema],
                            userAchievements: {
                                [userUid]: [{
                                    achievementId: 0,
                                    confirmedEventsCount: 0,
                                    pendingEventsCount: 0,
                                }],
                            },
                        });

                        await this.browser.yaProfile('ugctest3', 'touch:my-achievements')
                            .yaClosePopup(this.createPageObject(RegionPopup));

                        return this.reviewsAchievementsList
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(ReviewsAchievementModal.root, 2000));
                    },
                    async afterEach() {
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

                        await this.browser.yaProfile('ugctest3', 'touch:my-achievements')
                            .yaClosePopup(this.createPageObject(RegionPopup));

                        return this.reviewsAchievementsList
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(ReviewsAchievementModal.root, 2000));
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

                        await this.browser.yaProfile('ugctest3', 'touch:my-achievements')
                            .yaClosePopup(this.createPageObject(RegionPopup));

                        return this.reviewsAchievementsList
                            .clickFirstAchievement()
                            .then(() => this.browser.waitForVisible(ReviewsAchievementModal.root, 2000));
                    },
                    async afterEach() {
                        return this.browser.yaLogout();
                    },
                },
            })
        ),
        'Список.': prepareSuite(ReviewsAchievementsListSuite, {
            pageObjects: {
                reviewsAchievementsList() {
                    return this.createPageObject(ReviewsAchievementsList);
                },
                breadcrumbsUnified() {
                    return this.createPageObject(BreadcrumbsUnified);
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

                    return this.browser.yaProfile('ugctest3', 'touch:my-achievements')
                        .yaClosePopup(this.createPageObject(RegionPopup));
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
            params: {
                type: 'my',
                publicId,
            },
        }),
    },
});
