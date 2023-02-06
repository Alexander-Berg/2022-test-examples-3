package ru.yandex.market.core.program.partner.status;

import java.util.Map;
import java.util.Optional;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.core.program.partner.calculator.marketplace.ProgramStatusResolverType;
import ru.yandex.market.core.program.partner.model.ProgramStatus;
import ru.yandex.market.core.program.partner.model.ProgramSubStatus;
import ru.yandex.market.core.program.partner.model.Status;
import ru.yandex.market.mbi.partner.status.client.model.NeedTestingState;
import ru.yandex.market.mbi.partner.status.client.model.PartnerStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.PartnerSubStatusInfo;
import ru.yandex.market.mbi.partner.status.client.model.StatusResolverType;
import ru.yandex.market.mbi.partner.status.client.model.WizardStepStatus;
import ru.yandex.market.partner.test.context.FunctionalTest;

/**
 * Тесты для {@link PartnerStatusResolverMapper}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
public class PartnerStatusResolverMapperTest extends FunctionalTest {

    @Autowired
    private PartnerStatusResolverMapper partnerStatusResolverMapper;

    @ParameterizedTest
    @CsvSource({
            "FBS_SORTING_CENTER,FBS_SORTING_CENTER",
            "FEED_INDEXING_RESULTS,FEED_INDEXING_RESULTS",
            "FULFILLMENT_SUPPLY,FULFILLMENT_SUPPLY",
            "LIMIT_ORDERS,LIMIT_ORDERS",
            "NO_LOADED_OFFERS,NO_LOADED_OFFERS",
            "PUSH_API_ERRORS,PUSH_API_ERRORS",
            "REQUEST,REQUEST"
    })
    void testResolverTypes(StatusResolverType input, ProgramStatusResolverType output) {
        Assertions.assertThat(partnerStatusResolverMapper.mapFromPartnerStatusType(input))
                .isEqualTo(output);
    }

    @Test
    @DisplayName("Маппинг полного тела ответа")
    void testResponseMapping() {
        PartnerStatusInfo response = new PartnerStatusInfo()
                .partnerId(100L)
                .status(WizardStepStatus.FULL)
                .enabled(true)
                .addSubStatusesItem(new PartnerSubStatusInfo().code("code_1").putParamsItem("key_1", "val_1"))
                .needTestingState(NeedTestingState.NOT_REQUIRED)
                .newbie(false)
                .messageId(123L);
        Optional<ProgramStatus.Builder> actual = partnerStatusResolverMapper.map(response);

        ProgramStatus.Builder expected = ProgramStatus.builder()
                .status(Status.FULL)
                .enabled(true)
                .addSubStatus(new ProgramSubStatus("code_1", Map.of("key_1", "val_1")))
                .needTestingState(ru.yandex.market.core.program.partner.model.NeedTestingState.NOT_REQUIRED)
                .newbie(false)
                .messageId(123L);

        Assertions.assertThat(actual)
                .get()
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("Если нет статуса - конвертируем в null")
    void testEmptyResponseMapping() {
        PartnerStatusInfo response = new PartnerStatusInfo()
                .partnerId(100L)
                .enabled(true)
                .addSubStatusesItem(new PartnerSubStatusInfo().code("code_1").putParamsItem("key_1", "val_1"))
                .needTestingState(NeedTestingState.NOT_REQUIRED)
                .newbie(false)
                .messageId(123L);
        Optional<ProgramStatus.Builder> actual = partnerStatusResolverMapper.map(response);

        Assertions.assertThat(actual)
                .isEmpty();
    }
}
