package ru.yandex.market.pipelinetests.tests.lms_lom;

import java.util.List;
import java.util.Set;

import io.qameta.allure.Epic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;
import ru.yandex.market.pipelinetests.tests.lms_lom.utils.PartnerApiSettingsMethodsCompareUtils;

import static toolkit.FileUtil.bodyStringFromFile;
import static toolkit.Mapper.mapLmsResponse;

@Epic("Lms Lom Redis")
@Tag("LmsLomRedisSyncTest")
public abstract class AbstractGetPartnerApiSettingsMethodsTest extends AbstractLmsLomTest {
    protected static final SettingsMethodFilter FILTER = SettingsMethodFilter.newBuilder()
        .partnerIds(Set.of(FILLED_PARTNER_ID))
        .methodTypes(Set.of("createOrder"))
        .build();

    private static final List<SettingsMethodDto> EXPECTED_LMS_METHOD = List.of(mapLmsResponse(
        bodyStringFromFile("lms_lom_redis/lms_response/partner_settings_methods/method.json"),
        SettingsMethodDto.class
    ));

    @Test
    @DisplayName("Методы партнёра в ЛМС совпадают с ожидаемыми методами")
    public void checkPreConditionForPartnerExternalParamsByType() {
        List<SettingsMethodDto> lmsMethods = LMS_STEPS.searchPartnerSettingsMethods(FILTER).unwrap();
        PartnerApiSettingsMethodsCompareUtils.comparePartnerSettingsMethods(softly, EXPECTED_LMS_METHOD, lmsMethods);
    }
}
