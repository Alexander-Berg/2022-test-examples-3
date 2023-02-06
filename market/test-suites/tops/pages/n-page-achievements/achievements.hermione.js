import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import UserAchievementsPageSuite from '@self/platform/spec/hermione/test-suites/blocks/UserAchievementsPage';
import UserAchievementsPageNotAutorizedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/UserAchievementsPage/notAuthtorizatedUser';
import UserAchievementsPage from '@self/platform/spec/page-objects/UserAchievementsPage';
import NotAuthPage from '@self/platform/components/UserAchievementsPage/NotAuthPage/__spec__';
import UserAchievementsModal from '@self/platform/spec/page-objects/UserAchievementsModal';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница всех ачивок пользователя.', {
    story: mergeSuites(
        prepareSuite(UserAchievementsPageSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaBuildURL('market:index')
                        .then(url => {
                            const profile = profiles['pan-topinambur'];
                            return this.browser.yaLogin(profile.login, profile.password, url);
                        })
                        .then(() => this.browser.yaOpenPage('market:my-achievements'));
                },
            },
            pageObjects: {
                userAchievementsPage() {
                    return this.createPageObject(UserAchievementsPage);
                },
                userAchievementsModal() {
                    return this.createPageObject(UserAchievementsModal);
                },
            },
        }),
        prepareSuite(UserAchievementsPageNotAutorizedSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('market:my-achievements');
                },
            },
            pageObjects: {
                notAuthPage() {
                    return this.createPageObject(NotAuthPage);
                },
            },
        })
    ),
});
