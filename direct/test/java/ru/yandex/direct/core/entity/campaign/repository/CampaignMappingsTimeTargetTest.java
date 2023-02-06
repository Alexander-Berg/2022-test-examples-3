package ru.yandex.direct.core.entity.campaign.repository;

import org.junit.Test;

import ru.yandex.direct.libs.timetarget.HoursCoef;
import ru.yandex.direct.libs.timetarget.TimeTarget;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.core.entity.campaign.repository.CampaignMappings.timeTargetFromDb;
import static ru.yandex.direct.libs.timetarget.TimeTarget.PredefinedCoefs.USUAL;
import static ru.yandex.direct.libs.timetarget.TimeTarget.PredefinedCoefs.ZERO;
import static ru.yandex.direct.libs.timetarget.TimeTargetUtils.defaultTimeTarget;
import static ru.yandex.direct.libs.timetarget.WeekdayType.MONDAY;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

public class CampaignMappingsTimeTargetTest {

    @Test
    public void timeTargetFromDb_success() {
        TimeTarget timeTarget = timeTargetFromDb("1A");
        assertThat(timeTarget).isNotNull();
        assertThat(timeTarget.getWeekdayCoefs().get(MONDAY).getCoefForHour(0)).isEqualTo(USUAL.getValue());
        assertThat(timeTarget.getWeekdayCoefs().get(MONDAY).getCoefForHour(1)).isEqualTo(ZERO.getValue());
    }

    @Test
    public void timeTargetFromDb_nullString() {
        TimeTarget timeTarget = timeTargetFromDb(null);
        assertThat(timeTarget).is(matchedBy(beanDiffer(defaultTimeTarget())));
    }

    @Test
    public void timeTargetToDb_success() {
        HoursCoef coef = new HoursCoef();
        coef.setCoef(0, USUAL.getValue());
        TimeTarget timeTarget = new TimeTarget();
        timeTarget.setWeekdayCoef(MONDAY, coef);

        String toDb = CampaignMappings.timeTargetToDb(timeTarget);

        assertThat(toDb).isEqualTo("1A");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void timeTargetToDb_nullTimeTarget() {
        String toDb = CampaignMappings.timeTargetToDb(null);
        assertThat(toDb).isNull();
    }

    @Test
    public void timeTargetToDb_defaultTimeTarget() {
        String toDb = CampaignMappings.timeTargetToDb(defaultTimeTarget());
        assertThat(toDb).isNull();
    }

    @Test
    public void timeTargetToDb_noPresets() {
        String toDb = CampaignMappings.timeTargetToDb(new TimeTarget());
        assertThat(toDb).doesNotContain(";");
    }
}
