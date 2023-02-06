package ru.yandex.market.delivery.transport_manager.service.unit_queue;

import java.math.BigDecimal;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.unit_queue.UnitQueueStatus;
import ru.yandex.market.delivery.transport_manager.repository.mappers.UnitQueueMapper;
import ru.yandex.market.delivery.transport_manager.service.external.tpl.TplBarcodeSender;
import ru.yandex.market.tpl.client.dropoff.TplDropoffCargoClient;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoCreateCommandDto;
import ru.yandex.market.tpl.client.dropoff.dto.DropoffCargoDto;

import static org.mockito.ArgumentMatchers.eq;

@DatabaseSetup("/service/unit_queue/unit_queue_records_in_different_statuses.xml")
@DatabaseSetup(value = "/service/unit_queue/sortables.xml", type = DatabaseOperation.REFRESH)
class UnitQueueProcessorTest extends AbstractContextualTest {
    @Autowired
    private UnitQueueMapper unitQueueMapper;

    private final TplDropoffCargoClient client = Mockito.mock(TplDropoffCargoClient.class);

    private UnitQueueProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new UnitQueueProcessor(
            unitQueueMapper,
            new TplBarcodeSender(client),
            clock
        );

        Mockito.when(client.createCargo(Mockito.any())).thenReturn(
            ResponseEntity.ok(dropoffCargoDto())
        );
    }


    @ParameterizedTest
    @MethodSource("parameters")
    void processCorrect(long unitQueueId, DropoffCargoCreateCommandDto dto) {
        processor.process(unitQueueId);

        Mockito.verify(client).createCargo(eq(dto));
        softly.assertThat(unitQueueMapper.findById(unitQueueId).getStatus()).isEqualTo(UnitQueueStatus.PROCESSED);
    }

    private static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.of(6, dto(1, 2, "AAA6", null, null)),
            Arguments.of(20, dto(3, 4, "stamp_barcode_1", "lot_barcode_2", null)),
            Arguments.of(21, dto(3, 4, "stamp_barcode_1", null, BigDecimal.valueOf(1000.0))),
            Arguments.of(22, dto(1, 4, "box_barcode", "order_id", BigDecimal.valueOf(3.5)))
        );
    }

    @Test
    void processIncorrectStatus() {
        Assertions.assertThrows(RuntimeException.class, () -> processor.process(8L));
    }

    private static DropoffCargoCreateCommandDto dto(
        int from, int to, String barcode, String referenceId, BigDecimal price
    ) {
        DropoffCargoCreateCommandDto dto = new DropoffCargoCreateCommandDto();
        dto.setLogisticPointIdFrom(String.valueOf(from));
        dto.setLogisticPointIdTo(String.valueOf(to));
        dto.setBarcode(barcode);
        dto.setReferenceId(referenceId);
        dto.setDeclaredCost(price);
        return dto;
    }

    private static DropoffCargoDto dropoffCargoDto() {
        DropoffCargoDto dropoffCargoDto = new DropoffCargoDto();
        dropoffCargoDto.setBarcode("123");
        dropoffCargoDto.setStatus("wfew");

        return dropoffCargoDto;
    }
}
