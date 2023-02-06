package ru.yandex.direct.grid.processing.service.group;

import javax.annotation.ParametersAreNonnullByDefault;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.Test;
import org.junit.runner.RunWith;

import ru.yandex.direct.core.entity.adgroup.model.AdGroupType;
import ru.yandex.direct.core.entity.banner.model.BannerStatusModerate;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupAccess;
import ru.yandex.direct.grid.processing.model.group.GdAdGroupModerationStatus;
import ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.grid.processing.service.group.AdGroupActionConditionsUtil.canAcceptModerationConditions;
import static ru.yandex.direct.grid.processing.service.group.AdGroupActionConditionsUtil.canBeSentToRemoderationConditions;
import static ru.yandex.direct.grid.processing.service.group.AdGroupActionConditionsUtil.isAdGroupInDraftAndNotArchived;
import static ru.yandex.direct.grid.processing.service.group.AdGroupActionConditionsUtil.isAdGroupNotDraftAndNotArchived;
import static ru.yandex.direct.grid.processing.util.AdGroupTestDataUtils.defaultGdAdGroupStatus;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@RunWith(JUnitParamsRunner.class)
@ParametersAreNonnullByDefault
public class AdGroupActionConditionsUtilTest {

    @SuppressWarnings("unused")
    private Object[] adGroupAccesses() {
        return new Object[][]{
                {"empty adGroup",
                        defaultAdGroupAccess()
                                .withMainAdStatusModerate(null),
                        ClassMethodsResults.allResultsIsFalse()
                },

                {"archived adGroup",
                        defaultAdGroupAccess()
                                .withStatus(defaultGdAdGroupStatus().withArchived(true)),
                        ClassMethodsResults.allResultsIsFalse()
                },
                {"draft adGroup",
                        defaultAdGroupAccess().withStatus(
                                defaultGdAdGroupStatus().withModerationStatus(GdAdGroupModerationStatus.DRAFT)),
                        ClassMethodsResults.allResultsIsFalse()
                                .withAdGroupInDraftAndNotArchived(true)
                                .withCanAcceptModerationConditions(true)
                },
                {"draft mainAd of adGroup",
                        defaultAdGroupAccess()
                                .withMainAdStatusModerate(BannerStatusModerate.NEW),
                        ClassMethodsResults.allResultsIsFalse()
                                .withAdGroupInDraftAndNotArchived(true)
                                .withCanAcceptModerationConditions(true)
                },
                {"active adGroup",
                        defaultAdGroupAccess()
                                .withMainAdStatusModerate(BannerStatusModerate.YES)
                                .withStatus(
                                defaultGdAdGroupStatus().withModerationStatus(GdAdGroupModerationStatus.ACCEPTED)),
                        ClassMethodsResults.allResultsIsTrue()
                                .withAdGroupInDraftAndNotArchived(false)
                },

                {"performance adGroup",
                        defaultAdGroupAccess()
                                .withType(AdGroupType.PERFORMANCE),
                        ClassMethodsResults.allResultsIsTrue()
                                .withAdGroupInDraftAndNotArchived(false)
                                .withCanBeSentToRemoderationConditions(false)
                                .withCanAcceptModerationConditions(false)
                },
                {"text adGroup",
                        defaultAdGroupAccess()
                                .withType(AdGroupType.BASE),
                        ClassMethodsResults.allResultsIsTrue()
                                .withAdGroupInDraftAndNotArchived(false)
                },
        };
    }


    @Test
    @Parameters(method = "adGroupAccesses")
    @TestCaseName("{0}")
    public void checkClassMethodsResults(@SuppressWarnings("unused") String accessFlagName,
                                         GdAdGroupAccess adGroupAccess, ClassMethodsResults expectedResults) {
        ClassMethodsResults actualResults = new ClassMethodsResults()
                .withAdGroupInDraftAndNotArchived(isAdGroupInDraftAndNotArchived(adGroupAccess))
                .withAdGroupNotDraftAndNotArchived(isAdGroupNotDraftAndNotArchived(adGroupAccess))
                .withCanAcceptModerationConditions(canAcceptModerationConditions(adGroupAccess))
                .withCanBeSentToRemoderationConditions(canBeSentToRemoderationConditions(adGroupAccess));

        assertThat(actualResults)
                .is(matchedBy(beanDiffer(expectedResults)));
    }

    private static GdAdGroupAccess defaultAdGroupAccess() {
        return AdGroupTestDataUtils.defaultGdAdGroupAccess()
                .withMainAdStatusModerate(BannerStatusModerate.READY);
    }


    //контейнер для хранения результатов для каждого метода класса AdGroupActionConditionsUtil
    public static class ClassMethodsResults {
        private boolean isAdGroupNotDraftAndNotArchived;
        private boolean isAdGroupInDraftAndNotArchived;
        private boolean canBeSentToRemoderationConditions;
        private boolean canAcceptModerationConditions;

        static ClassMethodsResults allResultsIsFalse() {
            return new ClassMethodsResults()
                    .withAdGroupInDraftAndNotArchived(false)
                    .withAdGroupNotDraftAndNotArchived(false)
                    .withCanAcceptModerationConditions(false)
                    .withCanBeSentToRemoderationConditions(false);
        }

        static ClassMethodsResults allResultsIsTrue() {
            return new ClassMethodsResults()
                    .withAdGroupInDraftAndNotArchived(true)
                    .withAdGroupNotDraftAndNotArchived(true)
                    .withCanAcceptModerationConditions(true)
                    .withCanBeSentToRemoderationConditions(true);
        }

        public boolean isAdGroupNotDraftAndNotArchived() {
            return isAdGroupNotDraftAndNotArchived;
        }

        ClassMethodsResults withAdGroupNotDraftAndNotArchived(boolean adGroupNotDraftAndNotArchived) {
            isAdGroupNotDraftAndNotArchived = adGroupNotDraftAndNotArchived;
            return this;
        }

        public boolean isAdGroupInDraftAndNotArchived() {
            return isAdGroupInDraftAndNotArchived;
        }

        ClassMethodsResults withAdGroupInDraftAndNotArchived(boolean adGroupInDraftAndNotArchived) {
            isAdGroupInDraftAndNotArchived = adGroupInDraftAndNotArchived;
            return this;
        }

        public boolean isCanBeSentToRemoderationConditions() {
            return canBeSentToRemoderationConditions;
        }

        ClassMethodsResults withCanBeSentToRemoderationConditions(boolean canBeSentToRemoderationConditions) {
            this.canBeSentToRemoderationConditions = canBeSentToRemoderationConditions;
            return this;
        }

        public boolean isCanAcceptModerationConditions() {
            return canAcceptModerationConditions;
        }

        ClassMethodsResults withCanAcceptModerationConditions(boolean canAcceptModerationConditions) {
            this.canAcceptModerationConditions = canAcceptModerationConditions;
            return this;
        }
    }

}
