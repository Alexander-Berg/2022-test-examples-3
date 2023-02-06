package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task.enrichment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationTask;
import ru.yandex.market.delivery.transport_manager.domain.enums.CargoType;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterUnitMapper;
import ru.yandex.market.delivery.transport_manager.service.external.mdm.dto.ItemEnrichment;
import ru.yandex.market.delivery.transport_manager.service.external.mdm.dto.ItemRequest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ItemDataEnricherTest extends AbstractContextualTest {

    @Autowired
    RegisterMapper registerMapper;

    @Autowired
    RegisterUnitMapper registerUnitMapper;

    @Mock
    ItemMdmDataConverter mdmDataConverter;

    private ItemMdmDataEnricher itemMdmDataEnricher;

    @BeforeEach
    void initMock() {
        itemMdmDataEnricher = new ItemMdmDataEnricher(mdmDataConverter, registerMapper, registerUnitMapper);

        var req1 = new ItemRequest(4000, "QT391-65Z");
        var req2 = new ItemRequest(4000, "QT391-65Z-38");
        var req3 = new ItemRequest(5000, "6700-1");
        var req4 = new ItemRequest(5000, "404");
        replyToAllThree(req1, req2, req3);
        replyToOneOfTwo(req1, req2);
        noReply(req4);
    }

    @Test
    @DatabaseSetup("/repository/register_unit/enrichment/before.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/enrichment/after_all.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testAllEnriched() {
        itemMdmDataEnricher.enrich(new TransportationTask().setRegisterId(1L));
        verify(mdmDataConverter).getMdmData(any());
    }

    @Test
    @DatabaseSetup("/repository/register_unit/enrichment/before.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/enrichment/after_some.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testSomeEnriched() {
        itemMdmDataEnricher.enrich(new TransportationTask().setRegisterId(2L));
        verify(mdmDataConverter).getMdmData(any());
    }

    @Test
    @DatabaseSetup("/repository/register_unit/enrichment/before.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/enrichment/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNoDataForEmpty() {
        itemMdmDataEnricher.enrich(new TransportationTask().setRegisterId(3L));
    }

    @Test
    @DatabaseSetup("/repository/register_unit/enrichment/before.xml")
    @ExpectedDatabase(
        value = "/repository/register_unit/enrichment/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testNoDataForNonEmpty() {
        softly.assertThatThrownBy(() -> {
            itemMdmDataEnricher.enrich(new TransportationTask().setRegisterId(4L));
        }).isInstanceOf(IllegalArgumentException.class).hasMessageMatching(
            "At least 5[.,]00 % of 1 total items must be enriched, current enrichment percentage: 0[.,]00 %");
    }

    private void noReply(ItemRequest req4) {
        when(mdmDataConverter.getMdmData(Set.of(req4))).thenReturn(Map.of());
    }

    private void replyToOneOfTwo(ItemRequest req1, ItemRequest req2) {
        when(mdmDataConverter.getMdmData(Set.of(
            req1,
            req2
        ))).thenReturn(Map.of(
            req1, new ItemEnrichment(
                "QT391-65Z",
                4000,
                new BigDecimal("1.750"),
                new BigDecimal("2.500"),
                new BigDecimal("0.750"),
                60,
                55,
                25,
                List.of(CargoType.VALUABLE)
            )
            // второе обогащение пропустили
        ));
    }

    private void replyToAllThree(ItemRequest req1, ItemRequest req2, ItemRequest req3) {
        when(mdmDataConverter.getMdmData(Set.of(
            req1,
            req2,
            req3
        ))).thenReturn(Map.of(
            req1, new ItemEnrichment(
                "QT391-65Z",
                4000,
                new BigDecimal("1.750"),
                new BigDecimal("2.500"),
                new BigDecimal("0.750"),
                60,
                55,
                25,
                List.of(CargoType.VALUABLE)
            ),
            req2, new ItemEnrichment(
                "QT391-65Z-38",
                4000,
                new BigDecimal("1.850"),
                new BigDecimal("2.600"),
                new BigDecimal("0.750"),
                60,
                57,
                25,
                List.of(CargoType.VALUABLE)
            ),
            req3, new ItemEnrichment(
                "6700-1",
                5000,
                new BigDecimal("0.250"),
                new BigDecimal("0.300"),
                new BigDecimal("0.050"),
                120,
                10,
                10,
                List.of(CargoType.FRAGILE_CARGO, CargoType.DANGEROUS_CARGO)
            )
        ));
    }
}

