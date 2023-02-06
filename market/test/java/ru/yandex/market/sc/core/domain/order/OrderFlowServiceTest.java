package ru.yandex.market.sc.core.domain.order;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.EmbeddedDbTest;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.tpl.common.db.configuration.ConfigurationService;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.sc.core.domain.order.OrderFlowService.MisdeliveryProcessingProperties;
import static ru.yandex.market.sc.core.domain.order.OrderFlowService.MisdeliveryReturnDirection;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@EmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OrderFlowServiceTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    OrderFlowService orderFlowService;
    @Autowired
    ConfigurationService configurationService;

    Map<Long, MisdeliveryProcessingProperties> misdeliveryProcessingScs;

    @BeforeEach
    void init() throws JsonProcessingException {

        testFactory.storedSortingCenter(75001L);
        testFactory.storedSortingCenter(101366L);
        testFactory.storedSortingCenter(49781L);
        testFactory.storedSortingCenter(49784L);

        testFactory.storedWarehouse("10001700279");
        testFactory.storedWarehouse("10001804390");
        testFactory.storedWarehouse("10000010736");
        testFactory.storedWarehouse("10001671678");

        misdeliveryProcessingScs = Map.of(
                75001L,
                new MisdeliveryProcessingProperties("MSK", 1,
                        MisdeliveryReturnDirection.SORTING_CENTER, "10001700279", List.of()
                ),
                101366L,
                new MisdeliveryProcessingProperties("MSK", 2,
                        MisdeliveryReturnDirection.SORTING_CENTER, "10001804390", List.of()
                ),
                49781L,
                new MisdeliveryProcessingProperties("MSK", 3,
                        MisdeliveryReturnDirection.WAREHOUSE_FROM_LIST, null, List.of("10000010736", "10001671678")
                ),
                49784L,
                new MisdeliveryProcessingProperties("MSK", 4,
                        MisdeliveryReturnDirection.WAREHOUSE_FROM_LIST, null, List.of("10000010736", "10001671678")
                ),
                77777L,
                new MisdeliveryProcessingProperties("SPB", 1,
                        MisdeliveryReturnDirection.WAREHOUSE_FROM_LIST, null, List.of("77777")
                )
        );

        configurationService.insertValue(
                ConfigurationProperties.MISDELIVERY_RETURNS_MAPPINGS,
                new ObjectMapper().writeValueAsString(misdeliveryProcessingScs)
        );
    }

    @Test
    public void checkOrderFromTarniyMisdeliveryProcessing() {
        checkMisdeliveryReturnOrder(101366L, 75001L, "10001700279");
        checkMisdeliveryReturnOrder(49781L, 75001L, "10001700279");
        checkMisdeliveryReturnOrder(49784L, 75001L, "10001700279");
    }

    @Test
    public void checkOrderFromDzerjinskiyMisdeliveryProcessing() {
        checkMisdeliveryReturnOrder(75001L, 101366L, "10001804390");
        checkMisdeliveryReturnOrder(49781L, 101366L, "10001804390");
        checkMisdeliveryReturnOrder(49784L, 101366L, "10001804390");
    }

    @Test
    public void checkOrderFromZapadMisdeliveryProcessing() {
        checkMisdeliveryReturnOrder(75001L, 49781L, "10001671678");
        checkMisdeliveryReturnOrder(101366L, 49781L, "10000010736");
        checkMisdeliveryReturnOrder(49784L, 49781L, "10001671678");
    }

    @Test
    public void checkOrderFromSeverMisdeliveryProcessing() {
        checkMisdeliveryReturnOrder(75001L, 49784L, "10000010736");
        checkMisdeliveryReturnOrder(101366L, 49784L, "10001671678");
        checkMisdeliveryReturnOrder(49781L, 49784L, "10000010736");
    }

    @Test
    public void checkMisdeliveryGroupsIsolation() {
        assertThat(findWarehouseForMisdelivery(49784L, 77777L, "77777")).isEmpty();
    }

    @Test
    public void checkOrderFromDzerjinskiyMisdeliveryProcessingOrder() {
        String dzerjinskiyWarehouseYandexId = "10001804390";
        var scOrderInSCZapad = testFactory.createOrder(
                order(testFactory.storedSortingCenter(49781L), String.valueOf(System.nanoTime())).build()
        ).get();
        var scOrderInSCSever = testFactory.createOrder(
                order(testFactory.storedSortingCenter(49784L), String.valueOf(System.nanoTime())).build()
        ).get();
        assertThat(scOrderInSCZapad.getWarehouseReturnYandexId().get()).isNotEqualTo(dzerjinskiyWarehouseYandexId);
        assertThat(scOrderInSCSever.getWarehouseReturnYandexId().get()).isNotEqualTo(dzerjinskiyWarehouseYandexId);
        checkMisdeliveryReturnOrder(75001L, 101366L, dzerjinskiyWarehouseYandexId);
    }

    private void checkMisdeliveryReturnOrder(Long currentSc, Long originalSc,
                                             String expectedReturnWarehouseYandexId) {
        Optional<Warehouse> warehouseOptional = findWarehouseForMisdelivery(currentSc, originalSc,
                expectedReturnWarehouseYandexId);
        assertThat(warehouseOptional).isPresent();
        assertThat(warehouseOptional.get().getYandexId()).isEqualTo(expectedReturnWarehouseYandexId);
    }

    private Optional<Warehouse> findWarehouseForMisdelivery(Long currentScId, Long originalScId,
                                                            String expectedReturnWarehouseYandexId) {
        var currentSortingCenter = testFactory.storedSortingCenter(currentScId);
        var sourceSortingCenter = testFactory.storedSortingCenter(originalScId);
        var scOrderExternalId = testFactory.createForToday(
                order(sourceSortingCenter, String.valueOf(System.nanoTime()))
                        .warehouseReturnId(expectedReturnWarehouseYandexId)
                        .build()
        ).accept().sort().ship().makeReturn().get().getExternalId();
        return orderFlowService.lookForMisdeliveryReturnWarehouse(currentSortingCenter, scOrderExternalId);
    }

}
