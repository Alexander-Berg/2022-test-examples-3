package ru.yandex.market.pipelinetests.tests.lms_lom.redis;

import java.util.List;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.pipelinetests.tests.lms_lom.AbstractGetPartnerApiSettingsMethodsTest;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.PartnerApiSettingsMethodsCompareUtils;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
@DisplayName("Синхронизация данных LMS в redis")
public class GetPartnerSettingsMethodsFromRedisTest extends AbstractGetPartnerApiSettingsMethodsTest {

    @Test
    @DisplayName("REDIS: поиск метода партнёра")
    public void getPartnerExternalParamsByType() {
        List<SettingsMethodDto> lmsMethods = LMS_STEPS.searchPartnerSettingsMethods(FILTER).unwrap();
        List<SettingsMethodDto> redisMethods = LOM_REDIS_STEPS.searchPartnerSettingsMethods(FILTER);

        PartnerApiSettingsMethodsCompareUtils.comparePartnerSettingsMethods(softly, lmsMethods, redisMethods);
    }
}
