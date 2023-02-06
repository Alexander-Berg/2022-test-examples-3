package ru.yandex.direct.api.v5.entity.retargetinglists.converter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.qatools.allure.annotations.Description;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.MAX_ID_FOR_PREFIX;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.SHORT_TERM_PREFIX;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.getExternalId;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.getInterestType;
import static ru.yandex.direct.api.v5.entity.retargetinglists.converter.GoalInterestsTypeConverter.getInternalId;
import static ru.yandex.direct.core.entity.retargeting.model.CryptaInterestType.short_term;

@Api5Test
@RunWith(SpringRunner.class)
@Description("Конвертация идентификатора цели")
public class GoalInterestsTypeConverterTest {

    private static final Long INTERESTS_ID = 2_499_002_000L;
    private static final Long SHORT_TERM_EXTERNAL_INTERESTS_ID = SHORT_TERM_PREFIX * MAX_ID_FOR_PREFIX + INTERESTS_ID;
    private static final Long OTHER_ID = 2499000122L;

    @Test
    public void getInternalId_forInterestsTest() {
        assertEquals(INTERESTS_ID, getInternalId(SHORT_TERM_EXTERNAL_INTERESTS_ID));
    }

    @Test
    public void getInternalId_forOtherTypesTest() {
        assertEquals(OTHER_ID, getInternalId(OTHER_ID));
    }

    @Test
    public void getExternalId_forInterestsTest() {
        assertEquals(SHORT_TERM_EXTERNAL_INTERESTS_ID, getExternalId(INTERESTS_ID, short_term));
    }

    @Test
    public void getExternalId_forOtherTypesTest() {
        assertEquals(OTHER_ID, getExternalId(OTHER_ID, short_term));
    }

    @Test
    public void getInterestTypeTest() {
        assertEquals(short_term, getInterestType(SHORT_TERM_EXTERNAL_INTERESTS_ID));
    }
}
