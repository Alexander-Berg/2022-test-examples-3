package ru.yandex.market.wms.receiving.service.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.model.enums.ReceivingContainerType;
import ru.yandex.market.wms.common.spring.enums.ContainerIdType;
import ru.yandex.market.wms.common.spring.pojo.ReceivingContainer;
import ru.yandex.market.wms.common.spring.service.ReceivingContainerService;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT;

public class ReceivingContainerServiceIntegrationTest extends ReceivingIntegrationTest {
    private static final String USER = "receiving";

    @Autowired
    private ReceivingContainerService receivingContainerService;

    @Test
    @DatabaseSetup("/service/receiving-container/before-drop-container-created.xml")
    @ExpectedDatabase(value = "/service/receiving-container/after-drop-container-created.xml",
            assertionMode = NON_STRICT)
    public void createDropContainer() {
        receivingContainerService.createContainer(ContainerIdType.DROP, Map.of("CARRIER", "DPD"), USER);
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/before.xml", assertionMode = NON_STRICT)
    public void getByLocation() {
        List<ReceivingContainer> containers = receivingContainerService.getReceivingContainersByLocation("STAGE01");
        List<ReceivingContainer> expected = new ArrayList<>(3);
        expected.add(ReceivingContainer.builder().locationId("STAGE01").containerId("CART002")
                .type(ReceivingContainerType.STOCK).build());
        expected.add(ReceivingContainer.builder().locationId("STAGE01").containerId("CART003")
                .type(ReceivingContainerType.STOCK).build());
        expected.add(ReceivingContainer.builder().locationId("STAGE01").containerId("CART001")
                .type(ReceivingContainerType.STOCK).build());
        // порядок по adddate
        assertions.assertThat(containers).containsExactlyElementsOf(expected);
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/before.xml", assertionMode = NON_STRICT)
    public void getByContainerId() {
        Optional<ReceivingContainer> container = receivingContainerService.findContainerById("CART003");
        ReceivingContainer expected = ReceivingContainer.builder().locationId("STAGE01").containerId("CART003")
                .type(ReceivingContainerType.STOCK).build();
        assertions.assertThat(container).isPresent();
        assertions.assertThat(container.get()).isEqualTo(expected);
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/after-link-container.xml", assertionMode = NON_STRICT)
    public void linkContainer() {
        receivingContainerService.linkContainer("CART005", ReceivingContainerType.STOCK, "STAGE02", USER,
                ReceiptType.DEFAULT);
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/after-unlink-container.xml", assertionMode = NON_STRICT)
    public void unlinkContainer() {
        receivingContainerService.unlinkContainer("CART002");
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/after-relink-container.xml", assertionMode = NON_STRICT)
    public void relinkContainer() {
        receivingContainerService.linkContainer("CART004", ReceivingContainerType.STOCK, "STAGE01", USER,
                ReceiptType.DEFAULT);
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/after-link-damage-container.xml",
            assertionMode = NON_STRICT)
    public void linkContainerWithDamaged() {
        receivingContainerService.linkContainer("CART005", ReceivingContainerType.HOLD, "STAGE02", USER,
                ReceiptType.DEFAULT);
    }

    @Test
    @DatabaseSetup("/service/receiving-container/before.xml")
    @ExpectedDatabase(value = "/service/receiving-container/after-link-measure-container.xml",
            assertionMode = NON_STRICT)
    public void linkContainerWithUnmeasured() {
        receivingContainerService.linkContainer("CART006", ReceivingContainerType.MEASURE, "STAGE02", USER,
                ReceiptType.DEFAULT);
    }

    @Test
    @DatabaseSetup("/service/validate-container/immutable.xml")
    void shouldPassContainerIdValidation() {
        receivingContainerService.getOrCreateReceivingContainerById("CART006", USER, ReceivingContainerType.MEASURE,
                ReceiptType.DEFAULT);
    }

    @Test
    @DatabaseSetup("/service/validate-container/immutable.xml")
    void shouldPassAnomalyContainerIdValidation() {
        receivingContainerService.getOrCreateReceivingContainerById("AN00000062", USER, ReceivingContainerType.ANOMALY,
                ReceiptType.DEFAULT);
    }

    @Test
    @DatabaseSetup("/service/validate-container/immutable.xml")
    void shouldFailAnomalyContainerIdValidation() {
        assertions.assertThatThrownBy(
                        () -> receivingContainerService.getOrCreateReceivingContainerById("NOT_AN00000062", USER,
                                ReceivingContainerType.ANOMALY, ReceiptType.DEFAULT))
                .hasMessage("Container id NOT_AN00000062 does not match regex ^(AN)\\d+$");
    }
}
