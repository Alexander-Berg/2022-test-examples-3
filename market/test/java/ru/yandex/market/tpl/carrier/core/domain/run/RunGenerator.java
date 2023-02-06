package ru.yandex.market.tpl.carrier.core.domain.run;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.Value;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import ru.yandex.market.tpl.carrier.core.domain.company.Company;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementCommand;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementGenerator;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementSubtype;
import ru.yandex.market.tpl.carrier.core.domain.movement.MovementType;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator.RunGenerateParam.RunGenerateParamBuilder;
import ru.yandex.market.tpl.carrier.core.domain.run.RunGenerator.RunItemGenerateParam.RunItemGenerateParamBuilder;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunItemData;
import ru.yandex.market.tpl.carrier.core.domain.run.commands.RunCommand;
import ru.yandex.market.tpl.carrier.core.domain.shift.TestUserHelper;
import ru.yandex.market.tpl.carrier.core.domain.warehouse.OrderWarehouseGenerator;

@RequiredArgsConstructor

@Service
public class RunGenerator {
    private final MovementGenerator movementGenerator;

    private final RunCommandService runCommandService;
    private final OrderWarehouseGenerator orderWarehouseGenerator;
    private final TestUserHelper testUserHelper;


    public Run generate() {
        return generate(b -> b);
    }

    public Run generate(Function<RunGenerateParamBuilder, RunGenerateParamBuilder> tuner) {
        return generate(tuner, List.of(Pair.of(Function.identity(), Function.identity())));
    }

    public Run generate(Function<RunGenerateParamBuilder, RunGenerateParamBuilder> runTuner,
                        List<
                            Pair<
                                Function<RunItemGenerateParamBuilder, RunItemGenerateParamBuilder>,
                                Function<MovementCommand.Create.CreateBuilder, MovementCommand.Create.CreateBuilder>
                                >
                            > itemsTuners) {
        AtomicInteger index = new AtomicInteger();
        List<RunItemGenerateParam> runItems = StreamEx.of(itemsTuners)
                .map(tuners -> tuners.getKey().apply(RunItemGenerateParam.builder()
                        .movement(tuners.getValue().apply(MovementCommand.Create.builder()
                                        .orderWarehouse(orderWarehouseGenerator.generateWarehouse())
                                        .orderWarehouseTo(orderWarehouseGenerator.generateWarehouse()))
                                .build())
                        .orderNumber(index.incrementAndGet())
                        .fromIndex(null)
                        .toIndex(null))
                .build()).collect(Collectors.toList());

        RunGenerateParamBuilder builder = RunGenerateParam.builder()
                        .externalId(RandomStringUtils.randomNumeric(7, 9))
                        .deliveryServiceId(123L)
                        .campaignId(testUserHelper.findOrCreateCompany(Company.DEFAULT_COMPANY_NAME).getCampaignId())
                        .runDate(LocalDate.now())
                        .name(null)
                        .price(10_000L)
                        .runSubtype(RunSubtype.MAIN)
                        .items(runItems);

        return generate(runTuner.apply(builder).build());
    }

    public Run generate(RunGenerateParam param) {
        List<RunItemData> newItemData = StreamEx.of(param.getItems())
                .map(i -> new RunItemData(
                        movementGenerator.generate(i.getMovement()),
                        i.getOrderNumber(),
                        null,
                        i.getFromIndex(),
                        i.getToIndex()
                ))
                .toList();

        return runCommandService.create(RunCommand.Create.builder()
                .externalId(param.getExternalId())
                .deliveryServiceId(param.getDeliveryServiceId())
                .campaignId(param.getCampaignId())
                .runDate(param.getRunDate())
                .name(param.getName())
                .items(newItemData)
                .templateId(param.getRunTemplateId())
                .totalCount(param.getTotalCount())
                .price(param.getPrice())
                .subtype(param.getRunSubtype())
                .type(param.getRunType())
                .build());
    }


    @Value
    @Builder
    public static class RunGenerateParam {
        String externalId;
        Long deliveryServiceId;
        Long campaignId;
        LocalDate runDate;
        String name;
        Long runTemplateId;
        Integer totalCount;
        @Singular
        List<RunItemGenerateParam> items;
        Long price;
        RunSubtype runSubtype;
        RunType runType;
    }

    @Value
    @Builder
    @AllArgsConstructor
    public static class RunItemGenerateParam {
        MovementCommand.Create movement;
        int orderNumber;
        Integer fromIndex;
        Integer toIndex;
    }
}
