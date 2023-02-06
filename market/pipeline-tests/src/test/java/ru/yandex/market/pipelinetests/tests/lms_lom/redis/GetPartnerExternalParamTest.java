package ru.yandex.market.pipelinetests.tests.lms_lom.redis;

import java.util.List;

import io.qameta.allure.Epic;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;
import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractLmsLomTest;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.GetPartnerExternalParamsResponseCompareUtils;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в redis")
public class GetPartnerExternalParamTest extends AbstractLmsLomTest {

    @Test
    @DisplayName("Поиск параметров партнеров по типу")
    @SneakyThrows
    public void getPartnerExternalParamsByType() {
        List<PartnerExternalParamGroup> lmsPartners =
            LMS_STEPS.getPartnerExternalParam(EXTERNAL_PARAM_TYPES).getEntities();
        List<PartnerExternalParamGroup> redisPartners =
            LOM_REDIS_STEPS.getPartnerExternalParamValues(EXTERNAL_PARAM_TYPES);
        GetPartnerExternalParamsResponseCompareUtils.comparePartnerExternalParamGroups(
            softly,
            lmsPartners,
            redisPartners
        );
    }
}
