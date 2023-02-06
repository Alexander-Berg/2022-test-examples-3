package ru.yandex.market.logistics.werewolf.util;

import java.math.BigDecimal;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.context.IContext;

import ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils;
import ru.yandex.market.logistics.werewolf.AbstractTest;
import ru.yandex.market.logistics.werewolf.dto.document.TemplateEngineInput;
import ru.yandex.market.logistics.werewolf.model.entity.OrderLabels;
import ru.yandex.market.logistics.werewolf.model.entity.RtaOrdersData;
import ru.yandex.market.logistics.werewolf.testutils.RtaOrdersDataFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("ContextCreator")
class ContextCreatorTest extends AbstractTest {
    @Autowired
    private ContextCreator contextCreator;

    @Test
    @DisplayName("Контекст для АПП - нет колонки со стоимостью товаров")
    void attachedDocsNoItemsSumColumn() {
        RtaOrdersData rtaOrdersData = RtaOrdersDataFactory.rtaOrdersDataBuilder(null).build();
        String barcodePng = IntegrationTestUtils.extractFileContent("util/barcodeBase64.txt")
            .replace("\n", "");
        String barcodePngSender = IntegrationTestUtils.extractFileContent("util/barcodeBase64Sender.txt")
            .replace("\n", "");

        TemplateEngineInput engineInput = contextCreator.receptionTransferAct(rtaOrdersData);

        assertThat(engineInput.getTemplateName())
            .isNotBlank();

        assertThat(getVariablesFromContext(engineInput.getRenderContext()))
            .containsOnly(
                entry("request", rtaOrdersData),
                entry("barcodePng", barcodePng),
                entry("barcodePngSender", barcodePngSender),
                entry("actId", rtaOrdersData.getShipmentId()),
                entry("hasItemsSumColumn", false)
            );
    }

    @Test
    @DisplayName("Контекст для АПП - есть колонка со стоимостью товаров")
    void attachedDocsHasItemsSumColumn() {
        RtaOrdersData rtaOrdersData = RtaOrdersDataFactory.rtaOrdersDataBuilder(BigDecimal.ZERO).build();
        String barcodePng = IntegrationTestUtils.extractFileContent("util/barcodeBase64.txt")
            .replace("\n", "");
        String barcodePngSender = IntegrationTestUtils.extractFileContent("util/barcodeBase64Sender.txt")
            .replace("\n", "");

        TemplateEngineInput engineInput = contextCreator.receptionTransferAct(rtaOrdersData);

        assertThat(engineInput.getTemplateName())
            .isNotBlank();

        assertThat(getVariablesFromContext(engineInput.getRenderContext()))
            .containsOnly(
                entry("request", rtaOrdersData),
                entry("barcodePng", barcodePng),
                entry("barcodePngSender", barcodePngSender),
                entry("actId", rtaOrdersData.getShipmentId()),
                entry("hasItemsSumColumn", true)
            );
    }

    @Test
    @DisplayName("Контекст для ярлыков")
    void labels() {
        TemplateEngineInput engineInput = contextCreator.labels(OrderLabels.builder().build());

        assertThat(engineInput.getTemplateName())
            .isNotBlank();

        assertThat(getVariablesFromContext(engineInput.getRenderContext()))
            .containsOnlyKeys(
                "source",
                "barcodeGenerator",
                "logoSupplier"
            );
    }

    @Nonnull
    private Map<String, Object> getVariablesFromContext(IContext context) {
        return context.getVariableNames().stream()
            .collect(Collectors.toMap(Function.identity(), context::getVariable));
    }
}
