package ru.yandex.market.tpl.carrier.core.domain.run.price_control;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.tpl.carrier.core.CoreTestV2;
import ru.yandex.market.tpl.carrier.core.domain.run.Run;
import ru.yandex.market.tpl.carrier.core.domain.run.RunCommandService;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator;
import ru.yandex.market.tpl.carrier.core.domain.run.RunPriceControl;
import ru.yandex.market.tpl.carrier.core.domain.run.RunRepository;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.NewPriceControlData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;

@RequiredArgsConstructor(onConstructor_ = @Autowired)
@CoreTestV2
public class RunModifyTest {

    private final RunRepository runRepository;

    private final RunPriceControlQueryService priceControlQueryService;
    private final RunCommandService runCommandService;

    private final RunGenerator runGenerator;

    private final TransactionTemplate transactionTemplate;

    private Run run;

    @BeforeEach
    void setUp() {
        run = runGenerator.generate();
    }

    @Test
    void saveAndGetPriceControl() {
        NewPriceControlData priceControl = NewPriceControlData.builder()
                .type(PriceControlType.AUTO_DELAY)
                .cent(-3000_00)
                .comment("Auto penalty, 1 hour")
                .author("robot")
                .status(PriceControlStatus.NEED_CONFIRMATION)
                .build();
        runCommandService.addPriceControl(new RunCommand.AddPriceControl(run.getId(), priceControl));
        var result = priceControlQueryService.loadPriceControls(run);
        Assertions.assertEquals(
                PriceControlType.AUTO_DELAY,
                result.stream().map(RunPriceControl::getType).findAny().get()
        );
    }

    @Test
    void saveAndGetPriceStatus() {
        // сначала выставим статус из которого есть возможность перейти в CONFIRMED
        runCommandService.updatePriceStatus(run.getId(), RunPriceStatus.WARNING);
        runCommandService.updatePriceStatus(run.getId(), RunPriceStatus.CONFIRMED);
        transactionTemplate.execute(tc -> {
            var resultRun = runRepository.findById(run.getId());
            var resultProperty = resultRun.map(Run::getProperties).orElseThrow().stream()
                    .filter(rp -> "RUN_PRICE_STATUS".equals(rp.getPropertyName()))
                    .findAny().orElseThrow();

            Assertions.assertEquals(
                   "RUN_PRICE_STATUS",
                    resultProperty.getPropertyName()
            );
            Assertions.assertEquals(
                    "CONFIRMED",
                    resultProperty.getValue()
            );
            return null;
        });
    }

}
