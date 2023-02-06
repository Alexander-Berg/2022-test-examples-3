package ru.yandex.market.ff.service.returns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.UnitCountType;
import ru.yandex.market.ff.dbqueue.producer.service.SendingToCorrectValidationProducerService;
import ru.yandex.market.ff.dbqueue.service.EnrichReturnRegistryProcessingService;
import ru.yandex.market.ff.model.dbqueue.EnrichReturnRegistryPayload;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitCountsInfo;
import ru.yandex.market.ff.model.entity.registry.RegistryUnitEntity;
import ru.yandex.market.ff.model.returns.ReturnItemDto;
import ru.yandex.market.ff.model.returns.ReturnRegistryEnrichmentData;
import ru.yandex.market.ff.model.returns.ReturnUnitComplexKey;
import ru.yandex.market.ff.repository.RegistryRepository;
import ru.yandex.market.ff.repository.RegistryUnitRepository;
import ru.yandex.market.ff.repository.SupplierRepository;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.RequestSubTypeService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static ru.yandex.market.ff.dbqueue.service.EnrichReturnRegistryProcessingService.SHOULD_ACCEPT_LIKE_REFUND_UNKNOWN_BOX;

public class EnrichReturnRegistryProcessingServiceTest extends IntegrationTest {
    @Autowired
    private RegistryRepository registryRepository;
    @Autowired
    private RegistryUnitRepository registryUnitRepository;
    @Autowired
    private RequestSubTypeService requestSubTypeService;
    @Autowired
    private DefaultReturnInvalidRegistryUnitsServiceImpl defaultReturnInvalidRegistryUnitsService;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private ReturnInvalidRegistryUnitsServiceImpl returnInvalidRegistryUnitsService;

    private EnrichReturnRegistryProcessingService enrichReturnRegistryProcessingService;

    private UnknownBoxReturnItemsFetchingService unknownBoxReturnItemsFetchingService =
            Mockito.mock(UnknownBoxReturnItemsFetchingService.class);

    private RussianPostalServiceAsyncReturnItemsFetchingService russianPostalServiceAsyncReturnItemsFetchingService =
            Mockito.mock(RussianPostalServiceAsyncReturnItemsFetchingService.class);

    private ConcreteEnvironmentParamService paramService = Mockito.mock(ConcreteEnvironmentParamService.class);

    @BeforeEach
    void init() {

        SendingToCorrectValidationProducerService sendingToCorrectValidationProducerService =
                Mockito.mock(SendingToCorrectValidationProducerService.class);

        enrichReturnRegistryProcessingService = new EnrichReturnRegistryProcessingService(
                registryRepository,
                registryUnitRepository,
                shopRequestFetchingService,
                shopRequestModificationService,
                sendingToCorrectValidationProducerService,
                requestSubTypeService,
                supplierRepository,
                paramService,
                List.of(unknownBoxReturnItemsFetchingService, russianPostalServiceAsyncReturnItemsFetchingService),
                List.of(defaultReturnInvalidRegistryUnitsService, returnInvalidRegistryUnitsService)
        );

    }

    @Test
    @DatabaseSetup("classpath:service/returns/enrich-return-registry-process-service/before-unknown-boxes.xml")
    @ExpectedDatabase(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "after-unknown-boxes.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void enrichItemsWithUnknownBoxReturn() {
        EnrichReturnRegistryPayload payload = new EnrichReturnRegistryPayload(1L);
        when(unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(any()))
                .thenReturn(prepareRegistryEnrichmentDataWithUnknownBoxes(Set.of(getBox("box1"))));

        enrichReturnRegistryProcessingService.processPayload(payload);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "before-for-multiple-boxes-in-order.xml")
    @ExpectedDatabase(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "after-for-multiple-boxes-in-order.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void enrichItemsWithUnknownBoxReturnForMultipleBoxes() {
        EnrichReturnRegistryPayload payload = new EnrichReturnRegistryPayload(1L);
        when(unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(any()))
                .thenReturn(prepareRegistryEnrichmentDataWithUnknownBoxes(Set.of(getBox("box1"), getBox("box2"))));

        enrichReturnRegistryProcessingService.processPayload(payload);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "before-for-boxes-without-orderId.xml")
    @ExpectedDatabase(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "after-for-boxes-without-orderId.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void enrichItemsWithBoxesWithoutOrderId() {
        EnrichReturnRegistryPayload payload = new EnrichReturnRegistryPayload(1L);
        when(unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(any()))
                .thenReturn(prepareRegistryEnrichmentDataWithUnknownBoxes(Set.of(getBox("box1"), getBox("box2"))));

        enrichReturnRegistryProcessingService.processPayload(payload);
    }

    @Test
    @DatabaseSetup(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "before-for-order-without-returns.xml")
    @ExpectedDatabase(value = "classpath:service/returns/enrich-return-registry-process-service/" +
            "after-for-order-without-returns.xml", assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void enrichItemsWithOrderWithoutReturns() {
        EnrichReturnRegistryPayload payload = new EnrichReturnRegistryPayload(1L);
        when(unknownBoxReturnItemsFetchingService.getReturnItemsGroupedByKey(any()))
                .thenReturn(getPrepareRegistryEnrichmentDataWithOrderWithoutReturn());

        enrichReturnRegistryProcessingService.processPayload(payload);
    }

    @Test
    @DatabaseSetup("classpath:service/returns/mark-units-without-orders/before.xml")
    void markRegistryUnitsWithoutOrderIdAsNotAcceptable() {
        EnrichReturnRegistryPayload payload = new EnrichReturnRegistryPayload(1L);

        when(russianPostalServiceAsyncReturnItemsFetchingService.getReturnItemsGroupedByKey(any()))
                .thenReturn(new ReturnRegistryEnrichmentData(new HashMap<>(), null));
        requestSubTypeService.getEntityBySubtypeId(1007)
                .setInvalidRegistryUnitService("ReturnInvalidRegistryUnitsServiceImpl");

        enrichReturnRegistryProcessingService.processPayload(payload);

        List<RegistryUnitEntity> registryUnits = registryUnitRepository.findAll();

        RegistryUnitEntity actualItem = registryUnits.stream()
                .filter(unit -> unit.getId() == 101)
                .findFirst().orElse(null);

        UnitCountsInfo unitCountsInfo = actualItem.getUnitCountsInfo();

        unitCountsInfo.getUnitCounts().stream().forEach(unitCount -> {
            assertions.assertThat(UnitCountType.NOT_ACCEPTABLE).isEqualTo(unitCount.getType());
            assertions.assertThat(unitCount.getComments()).contains(SHOULD_ACCEPT_LIKE_REFUND_UNKNOWN_BOX);
        });

        assertions.assertThat(registryUnits.size()).isEqualTo(2);
    }

    private ReturnRegistryEnrichmentData getPrepareRegistryEnrichmentDataWithOrderWithoutReturn() {
        Set<ReturnUnitComplexKey> boxes = Set.of(getBox("box1"), getBox("box2", String.valueOf(-3960221), "3960221"));

        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemByComplexKey = new HashMap<>();

        returnItemByComplexKey.put(createComplexKey(), List.of(itemReturnResult()));
        returnItemByComplexKey.put(createComplexKeyForOrderWithCIS(), List.of(itemReturnWithCISResult()));
        returnItemByComplexKey.put(createComplexKey(String.valueOf(-3960221), "box2", "3960221", "10125"),
                List.of(new ReturnItemDto(
                        new ArrayList<>(),
                        null,
                        null,
                        1))
        );

        return new ReturnRegistryEnrichmentData(
                returnItemByComplexKey,
                boxes,
                Collections.emptyList()
        );
    }

    private ReturnRegistryEnrichmentData prepareRegistryEnrichmentDataWithUnknownBoxes(
            Set<ReturnUnitComplexKey> boxes) {
        Map<ReturnUnitComplexKey, List<ReturnItemDto>> returnItemByComplexKey = new HashMap<>();

        returnItemByComplexKey.put(createComplexKey(), List.of(itemReturnResult()));
        returnItemByComplexKey.put(createComplexKeyForOrderWithCIS(), List.of(itemReturnWithCISResult()));

        return new ReturnRegistryEnrichmentData(
                returnItemByComplexKey,
                boxes,
                Collections.emptyList()
        );
    }

    private ReturnUnitComplexKey getBox(String box) {
        return getBox(box, "1232131", "3960222");
    }

    private ReturnUnitComplexKey getBox(String box1, String orderReturnId, String orderId) {
        return ReturnUnitComplexKey.of(
                orderReturnId,
                box1,
                null,
                null,
                orderId
        );
    }

    private ReturnItemDto itemReturnResult() {

        ReturnItemDto returnItemDto = new ReturnItemDto(
                new ArrayList<>(),
                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                null,
                1);

        return returnItemDto;
    }

    private ReturnItemDto itemReturnWithCISResult() {
        RegistryUnitId registryUnitId = RegistryUnitId.of(
                RegistryUnitIdType.CIS,
                "010964018661011021mbg:zCaRlU%c08-cis1",
                RegistryUnitIdType.SERIAL_NUMBER,
                "32397437-item1-9324312-1");

        ReturnItemDto returnItemDto = new ReturnItemDto(
                Arrays.asList(registryUnitId),
                ru.yandex.market.ff.model.enums.ReturnReasonType.BAD_QUALITY,
                "broken",
                1);

        return returnItemDto;
    }

    private ReturnUnitComplexKey createComplexKeyForOrderWithCIS() {
        return createComplexKey("1232131", "box1", "3960222", "10124");
    }

    private ReturnUnitComplexKey createComplexKey() {
        return createComplexKey("1232131", "box1", "3960222", "10125");
    }

    private ReturnUnitComplexKey createComplexKey(String orderReturnId, String box, String orderId, String sku) {
        return ReturnUnitComplexKey.of(
                orderReturnId,
                box,
                48000L,
                sku,
                orderId
        );
    }
}
