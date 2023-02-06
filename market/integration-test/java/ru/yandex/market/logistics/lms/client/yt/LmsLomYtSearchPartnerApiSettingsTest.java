package ru.yandex.market.logistics.lms.client.yt;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.request.settings.SettingsMethodFilter;
import ru.yandex.market.logistics.management.entity.response.settings.methods.SettingsMethodDto;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("Получение API методов партнера по идентификатору")
class LmsLomYtSearchPartnerApiSettingsTest extends LmsLomYtAbstractTest {

    private static final String GET_PARTNER_API_SETTINGS_QUERY =
        "* FROM [//home/2022-03-02T08:05:24Z/partner_api_settings_dyn] " +
            "WHERE partner_id = 1 AND method = 'methodName'";
    private static final SettingsMethodFilter FILTER = SettingsMethodFilter.newBuilder()
        .partnerIds(Set.of(1L))
        .methodTypes(Set.of("methodName"))
        .build();

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        mockYtPartnerApiSettingsQueryResponse();
    }

    @Test
    @DisplayName("Успешное получение методов партнера из yt")
    @DatabaseSetup("/lms/client/yt/get_partner_api_settings_from_yt_enabled.xml")
    void successGetScheduleDayById() {
        softly.assertThat(lmsLomYtClient.searchPartnerApiSettingsMethods(FILTER))
            .isEqualTo(List.of(partnerApiSettings()));

        verifyYtCalling();
    }

    @Test
    @DisplayName("Флаг получения методов партнера выключен, клиент не идет в yt")
    void goingToYtDisabled() {
        softly.assertThat(lmsLomYtClient.searchPartnerApiSettingsMethods(FILTER))
            .isEmpty();
    }

    private void verifyYtCalling() {
        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(ytTables, GET_PARTNER_API_SETTINGS_QUERY);
        verify(hahnYt, times(2)).tables();
    }

    private void mockYtPartnerApiSettingsQueryResponse() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(partnerApiSettings()),
            GET_PARTNER_API_SETTINGS_QUERY
        );
    }

    @Nonnull
    private SettingsMethodDto partnerApiSettings() {
        return SettingsMethodDto.newBuilder()
            .partnerId(1L)
            .method("methodName")
            .active(true)
            .build();
    }
}
