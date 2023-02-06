package ru.yandex.market.orders.resupply.enrich;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.order.resupply.ResupplyOrderDao;
import ru.yandex.market.ff.client.FulfillmentWorkflowClientApi;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.dto.RegistryUnitPartialIdDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTO;
import ru.yandex.market.ff.client.dto.RequestItemDTOContainer;
import ru.yandex.market.ff.client.dto.RequestItemFilterDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTO;
import ru.yandex.market.ff.client.dto.ShopRequestDTOContainer;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Тест на {@link ResuppliesFFEnrichService}
 */
@DbUnitDataBaseConfig(@DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"))
public class ResuppliesFFEnrichServiceTest extends FunctionalTest {

    @Autowired
    private EnvironmentService environmentService;

    @Autowired
    private ResupplyOrderDao resupplyOrderDao;

    private FulfillmentWorkflowClientApi fulfillmentWorkflowClientApi;

    private ResuppliesFFEnrichService resuppliesFFEnrichService;

    @BeforeEach
    void init() {
        fulfillmentWorkflowClientApi = mock(FulfillmentWorkflowClientApi.class);
        resupplyOrderDao = spy(resupplyOrderDao);
        resuppliesFFEnrichService = new ResuppliesFFEnrichService(
                environmentService,
                resupplyOrderDao,
                fulfillmentWorkflowClientApi
        );
    }

    @Test
    @DbUnitDataSet(
            before = "ResuppliesFFEnrichServiceTest.before.csv",
            after = "ResuppliesFFEnrichServiceTest.after.csv")
    void resupplyFFEnrichTest() {
        ShopRequestDTOContainer container = mock(ShopRequestDTOContainer.class);
        List<ShopRequestDTO> requests = new ArrayList<>();

        requests.add(createRequest(120L, LocalDateTime.of(3, 3, 3, 3, 3)));
        requests.add(createRequest(121L, LocalDateTime.of(4, 4, 4, 4, 4)));
        requests.add(createRequest(127L, LocalDateTime.of(6, 6, 6, 6, 6)));
        requests.add(createRequest(122L, LocalDateTime.of(2, 2, 2, 2, 2)));

        when(fulfillmentWorkflowClientApi
                .getRequests(argThat(t -> t != null && CollectionUtils.isEqualCollection(
                        List.of("129", "120", "121", "122", "125", "127"),
                        t.getRequestIds()))))
                .thenReturn(container);
        when(container.getRequests())
                .thenReturn(requests);

        doAnswer(ans -> {
            RequestItemFilterDTO filter = ans.getArgument(0);
            if (filter.getPage() != 0) {
                return null;
            }

            if (filter.getRequestId() == 127) {
                return createRequestItemContainer(List.of(
                        createRequestItem(
                                "000128.82382",
                                List.of(),
                                List.of())));
            } else if (filter.getRequestId() == 121) {
                return createRequestItemContainer(List.of(
                        createRequestItem(
                                "000362.20129",
                                List.of("12"),
                                List.of())));
            } else if (filter.getRequestId() == 122) {
                return createRequestItemContainer(List.of(
                        createRequestItem(
                                "000362.20126",
                                List.of(),
                                List.of("34"))));
            } else if (filter.getRequestId() == 120) {
                throw new Exception();
            } else {
                return null;
            }
        }).when(fulfillmentWorkflowClientApi)
                .getRequestItems(any(RequestItemFilterDTO.class));
        resuppliesFFEnrichService.resupplyFFEnrich();
    }

    private RequestItemDTOContainer createRequestItemContainer(List<RequestItemDTO> items) {
        RequestItemDTOContainer requestItems = new RequestItemDTOContainer();
        for (RequestItemDTO item : items) {
            requestItems.addItem(item);
        }
        return requestItems;
    }

    private RequestItemDTO createRequestItem(
            String ssku,
            List<String> fitCis,
            List<String> defectCis
    ) {
        RequestItemDTO requestItem = new RequestItemDTO();
        requestItem.setArticle(ssku);

        RegistryUnitIdDTO fitUnit = new RegistryUnitIdDTO();
        fitUnit.setParts(fitCis.stream().map(c -> {
            RegistryUnitPartialIdDTO unitId = new RegistryUnitPartialIdDTO();
            unitId.setType(RegistryUnitIdType.CIS);
            unitId.setValue(c);
            return unitId;
        }).collect(Collectors.toSet()));

        RegistryUnitIdDTO defectUnit = new RegistryUnitIdDTO();
        defectUnit.setParts(defectCis.stream().map(c -> {
            RegistryUnitPartialIdDTO unitId = new RegistryUnitPartialIdDTO();
            unitId.setType(RegistryUnitIdType.CIS);
            unitId.setValue(c);
            return unitId;
        }).collect(Collectors.toSet()));

        requestItem.setFactUnitId(fitUnit);
        requestItem.setFactUnfitUnitId(defectUnit);
        return requestItem;
    }

    private ShopRequestDTO createRequest(Long id,
                                         LocalDateTime dateTime) {
        ShopRequestDTO request = new ShopRequestDTO();
        request.setId(id);
        request.setStatus(RequestStatus.FINISHED);
        request.setUpdatedAt(dateTime);
        request.setServiceId(1234L);
        return request;
    }
}
