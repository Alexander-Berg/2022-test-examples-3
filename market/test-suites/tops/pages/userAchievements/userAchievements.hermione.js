import {prepareSuite, makeSuite} from 'ginny';

// suites
import ReviewsAchievementModalUnlockedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementModal/__status/unlocked';
import ReviewsAchievementsListSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsAchievementsList';
// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';
import ReviewsAchievementsList from '@self/platform/spec/page-objects/ReviewsAchievementsList';
import ReviewsAchievementModal from '@self/platform/spec/page-objects/ReviewsAchievementModal';

const userUid = '636368980';
const publicId = 'x1xx9ugctest3z8z7zz';
const userSchema = {
    id: userUid,
    uid: {
        value: userUid,
    },
    login: 'ugctest3',
    public_id: publicId,
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Достижения пользователя".', {
    environment: 'kadavr',
    story: {
        'Модальное окно достижения.': prepareSuite(ReviewsAchievementModalUnlockedSuite, {
            pageObjects: {
                reviewsAchievementsList() {
                    return this.createPageObject(ReviewsAchievementsList);
                },
                reviewsAchievementModal() {
                    return this.createPageObject(ReviewsAchievementModal);
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

                    await this.browser.yaOpenPage('touch:user-achievements', {publicId})
                        .yaClosePopup(this.createPageObject(RegionPopup));

                    return this.reviewsAchievementsList
                        .clickFirstAchievement()
                        .then(() => this.browser.waitForVisible(ReviewsAchievementModal.root, 2000));
                },
            },
        }),
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

                    return this.browser.yaOpenPage('touch:user-achievements', {publicId})
                        .yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            params: {
                type: 'user',
                publicId,
            },
        }),
    },
});
