package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.assertj.core.api.junit.jupiter.InjectSoftAssertions;
import org.assertj.core.api.junit.jupiter.SoftAssertionsExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import step.LmsSteps;
import step.LomLmsYtSteps;
import step.LomRedisSteps;

import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.type.DeliveryType;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;
import ru.yandex.market.pipelinetests.tests.AbstractTest;

@ExtendWith(SoftAssertionsExtension.class)
public class AbstractLmsLomTest extends AbstractTest {

    protected static final LmsSteps LMS_STEPS = new LmsSteps();
    protected static final LomRedisSteps LOM_REDIS_STEPS = new LomRedisSteps();
    protected static final LomLmsYtSteps LOM_LMS_YT_STEPS = new LomLmsYtSteps();

    protected static final Long FILLED_LOGISTICS_POINT_ID = 10001020220L;
    protected static final Long EMPTY_LOGISTICS_POINT_ID = 10001020081L;
    protected static final Long LOGISTICS_POINT_WITH_EMPTY_SCHEDULE_ID = 10001020446L;
    protected static final Long FILLED_PARTNER_ID = 56203L;
    protected static final Long EMPTY_PARTNER_ID = 56212L;
    protected static final LogisticsPointFilter LOGISTICS_POINTS_IDS_FILTER = LogisticsPointFilter.newBuilder()
        .ids(Set.of(FILLED_LOGISTICS_POINT_ID, EMPTY_LOGISTICS_POINT_ID, LOGISTICS_POINT_WITH_EMPTY_SCHEDULE_ID))
        .build();
    protected static final LogisticsPointFilter LOGISTICS_POINTS_PARTNER_IDS_FILTER = LogisticsPointFilter.newBuilder()
        .ids(Set.of(FILLED_PARTNER_ID, EMPTY_PARTNER_ID))
        .build();
    protected static final Long PARTNER_RELATION_FROM_ID = 56308L;
    protected static final Long PARTNER_RELATION_TO_ID = 56315L;
    protected static final Long EMPTY_CUTOFFS_PARTNER_RELATION_FROM_ID = 56330L;
    protected static final Long EMPTY_CUTOFFS_PARTNER_RELATION_TO_ID = 56331L;
    protected static final Set<PartnerExternalParamType> EXTERNAL_PARAM_TYPES =
        Set.of(PartnerExternalParamType.ASSESSED_VALUE_TOTAL_CHECK, PartnerExternalParamType.SERVICE_EMAILS);
    protected static final Long INBOUND_SCHEDULE_PARTNER_FROM = 56751L;
    protected static final Long INBOUND_SCHEDULE_PARTNER_TO = 56752L;
    protected static final DeliveryType FILLED_INBOUND_SCHEDULE_DELIVERY_TYPE = DeliveryType.COURIER;
    protected static final DeliveryType EMPTY_INBOUND_SCHEDULE_DELIVERY_TYPE = DeliveryType.PICKUP;
    protected static final Long FILLED_SCHEDULE_DAY_ID = 270387652L;
    protected static final Long EMPTY_SCHEDULE_DAY_ID = 270387653L;

    @InjectSoftAssertions
    protected SoftAssertions softly;
}
