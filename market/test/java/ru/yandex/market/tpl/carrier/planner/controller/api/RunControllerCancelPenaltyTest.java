package ru.yandex.market.tpl.carrier.planner.controller.api;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.carrier.core.domain.configuration.ConfigurationProperties;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunPriceControl;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.price_control.PriceControlType;
import ru.yandex.market.tpl.carrier.planner.controller.BasePlannerWebTest;
import ru.yandex.mj.generated.server.model.RunCancelStatusDto;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RequiredArgsConstructor(onConstructor_=@Autowired)
public class RunControllerCancelPenaltyTest extends BasePlannerWebTest {

    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final RunGenerator runGenerator;
    private final RunRepository runRepository;
    private final TransactionTemplate transactionTemplate;

    private Run run;

    @SneakyThrows
    @Test
    void shouldCreatePenaltyIfCancelledByCarrier() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.CANCELLED_BY_CARRIER_PRICE_CONTROL_CENTS, -100_00);
        run = runGenerator.generate();
        cancelForActions(RunCancelStatusDto.CANCELLED_BY_CARRIER)
                .andExpect(status().isOk());


        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(run.getPriceControls())
                    .hasSize(1);

            RunPriceControl priceControl = run.getPriceControls().get(0);
            Assertions.assertThat(priceControl.getType())
                    .isEqualTo(PriceControlType.AUTO_CANCELLED_BY_CARRIER);
            Assertions.assertThat(priceControl.getCent())
                    .isEqualTo(-100_00);
            return null;
        });
    }

    @SneakyThrows
    @Test
    void shouldCreateOverpayIfCancelledByCarrier() {
        configurationServiceAdapter.mergeValue(ConfigurationProperties.CANCELLED_BY_MARKET_PRICE_CONTROL_CENTS, 100_00);
        run = runGenerator.generate();
        cancelForActions(RunCancelStatusDto.CANCELLED_BY_MARKET)
                .andExpect(status().isOk());


        transactionTemplate.execute(tc -> {
            run = runRepository.findByIdOrThrow(run.getId());
            Assertions.assertThat(run.getPriceControls())
                    .hasSize(1);

            RunPriceControl priceControl = run.getPriceControls().get(0);
            Assertions.assertThat(priceControl.getType())
                    .isEqualTo(PriceControlType.AUTO_CANCELLED_BY_MARKET);
            Assertions.assertThat(priceControl.getCent())
                    .isEqualTo(100_00);
            return null;
        });
    }

    private ResultActions cancelForActions(RunCancelStatusDto statusDto) throws Exception {
        return mockMvc.perform(post("/internal/runs/{id}/cancel", run.getId())
                .param("status", statusDto.name()));
    }
}
