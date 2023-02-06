package ru.yandex.market.pipelinetests.tests.lms_lom.yt;

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
@DisplayName("Синхронизация данных LMS в YT")
public class GetPartnerSettingsMethodsFromYtTest extends AbstractGetPartnerApiSettingsMethodsTest {

    @Test
    @DisplayName("YT: поиск метода партнёра")
    public void getPartnerExternalParamsByType() {
        List<SettingsMethodDto> lmsMethods = LMS_STEPS.searchPartnerSettingsMethods(FILTER).unwrap();
        List<SettingsMethodDto> ytMethods = LOM_LMS_YT_STEPS.searchPartnerSettingsMethods(FILTER);

        PartnerApiSettingsMethodsCompareUtils.comparePartnerSettingsMethods(softly, lmsMethods, ytMethods);
    }
}
