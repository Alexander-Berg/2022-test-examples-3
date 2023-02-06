package ru.yandex.market.logistics.lms.client.yt;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.lms.client.utils.PartnerExternalParamsDataUtils;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;
import ru.yandex.market.logistics.lom.utils.YtUtils;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamGroup;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ParametersAreNonnullByDefault
@DisplayName("Получение параметров партнёров в клиенте yt для lms")
class LmsLomYtClientGetPartnerExternalParamsTest extends LmsLomYtAbstractTest {

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();

        YtUtils.mockSelectRowsFromYt(
            ytTables,
            List.of(),
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
        );
    }

    @Test
    @DisplayName("Получение параметров партнёров из yt выключено")
    void getPartnerExternalParamsFromYtDisabled() {
        softly.assertThat(getPartnerExternalParams())
            .isEmpty();
    }

    @Test
    @DisplayName("Параметры партнёров не найдены")
    @DatabaseSetup("/lms/client/yt/get_partner_external_params_from_yt_enabled.xml")
    void noParamsInYt() {
        softly.assertThat(getPartnerExternalParams())
            .isEmpty();

        verifyYtCalling();
    }

    @Test
    @DisplayName("Успешное получение параметров партнёров")
    @DatabaseSetup("/lms/client/yt/get_partner_external_params_from_yt_enabled.xml")
    void successGetPartnerExternalParams() {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            PartnerExternalParamsDataUtils.partnerExternalParams(),
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
        );

        softly.assertThat(getPartnerExternalParams())
            .isEqualTo(PartnerExternalParamsDataUtils.partnerExternalParams());

        verifyYtCalling();
    }

    @Test
    @DisplayName("Ошибка при обращении в yt")
    @DatabaseSetup("/lms/client/yt/get_partner_external_params_from_yt_enabled.xml")
    void errorInYt() {
        YtUtils.mockExceptionCallingYt(
            ytTables,
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY,
            new RuntimeException("Some yt exception")
        );

        softly.assertThatCode(this::getPartnerExternalParams)
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Some yt exception");

        verifyYtCalling();
    }

    @Nonnull
    private List<PartnerExternalParamGroup> getPartnerExternalParams() {
        return lmsLomYtClient.getPartnerExternalParams(PartnerExternalParamsDataUtils.PARTNER_EXTERNAL_PARAM_TYPES_SET);
    }

    private void verifyYtCalling() {
        verify(hahnYt, times(2)).tables();

        YtLmsVersionsUtils.verifyYtVersionTableInteractions(ytTables, lmsYtProperties);
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            PartnerExternalParamsDataUtils.GET_PARTNER_EXTERNAL_PARAMS_QUERY
        );
    }
}
