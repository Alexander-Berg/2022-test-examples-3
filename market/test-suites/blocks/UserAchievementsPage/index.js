import {mergeSuites, makeSuite, prepareSuite} from 'ginny';

import AchievementsBlockSuite from '@self/platform/spec/hermione/test-suites/blocks/UserAchievementsPage/achievementsBlock';
import BreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/UserAchievementsPage/breadcrumbs';
import AchievementModalCloseSuite from '@self/platform/spec/hermione/test-suites/blocks/UserAchievementsPage/achievementModalClose';
import AchievementsSlideSuite from '@self/platform/spec/hermione/test-suites/blocks/UserAchievementsPage/achievementsSlide';

export default makeSuite('Страница ачивок.', {
    story: mergeSuites(
        prepareSuite(AchievementsBlockSuite),
        prepareSuite(BreadcrumbsSuite),
        prepareSuite(AchievementModalCloseSuite),
        prepareSuite(AchievementsSlideSuite)
    ),
});
