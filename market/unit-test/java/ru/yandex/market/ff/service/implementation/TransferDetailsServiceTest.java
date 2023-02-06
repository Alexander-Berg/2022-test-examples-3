package ru.yandex.market.ff.service.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.dto.TransferWithCisesDTO;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.model.converter.ShopRequestConverter;
import ru.yandex.market.ff.model.converter.TransferConverter;
import ru.yandex.market.ff.model.converter.TransferStatusHistoryConverter;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.Identifier;
import ru.yandex.market.ff.model.entity.RequestItem;
import ru.yandex.market.ff.model.entity.ShopRequest;
import ru.yandex.market.ff.model.enums.IdentifierType;
import ru.yandex.market.ff.repository.RequestStatusHistoryRepository;
import ru.yandex.market.ff.repository.ShopRequestRepository;
import ru.yandex.market.ff.service.RequestItemService;
import ru.yandex.market.ff.service.ShopRequestFetchingService;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TransferDetailsServiceTest {

    private TransferDetailsServiceImpl transferService;
    private ShopRequestFetchingService shopRequestFetchingService;

    @Before
    public void init() {
        ShopRequestRepository shopRequestRepository = Mockito.mock(ShopRequestRepository.class);
        RequestStatusHistoryRepository requestStatusHistoryRepository =
                Mockito.mock(RequestStatusHistoryRepository.class);
        ShopRequestConverter shopRequestConverter = Mockito.mock(ShopRequestConverter.class);
        TransferConverter transferConverter = Mockito.mock(TransferConverter.class);
        shopRequestFetchingService = Mockito.mock(ShopRequestFetchingService.class);
        TransferStatusHistoryConverter transferStatusHistoryConverter =
                Mockito.mock(TransferStatusHistoryConverter.class);
        RequestItemService requestItemService = Mockito.mock(RequestItemService.class);
        this.transferService = new TransferDetailsServiceImpl(shopRequestRepository, requestStatusHistoryRepository,
                shopRequestConverter, transferConverter, shopRequestFetchingService, transferStatusHistoryConverter,
                requestItemService);
    }

    @Test
    void getTransferWithCises() {
        init();
        ShopRequest shopRequest = new ShopRequest();
        RequestItem requestItem = new RequestItem();
        requestItem.setArticle("111");
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DECLARED);
        UnitPartialId unitPartialId = new UnitPartialId(RegistryUnitIdType.CIS, "111");
        HashSet<UnitPartialId> unitPartialIds = new HashSet<>();
        unitPartialIds.add(unitPartialId);
        RegistryUnitId registryUnitId = new RegistryUnitId(unitPartialIds);
        identifier.setIdentifiers(registryUnitId);
        HashSet<Identifier> identifiers = new HashSet<>();
        identifiers.add(identifier);
        requestItem.setRequestItemIdentifiers(identifiers);
        shopRequest.setItems(Collections.singleton(requestItem));
        Mockito.when(shopRequestFetchingService.findAllByInboundIdAndSupplierId(1, 1,
                Arrays.asList(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CANCELLED,
                        RequestStatus.INVALID)))
                .thenReturn(Collections.singleton(shopRequest));
        List<TransferWithCisesDTO> transferWithCises =
                transferService.getTransferWithCises(1, 1, "111", Collections.singleton("111"));
        assertEquals(transferWithCises.size(), 1);
    }

    @Test
    void filterTransferwithNoCisesType() {
        init();
        ShopRequest shopRequest = new ShopRequest();
        RequestItem requestItem = new RequestItem();
        requestItem.setArticle("111");
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DECLARED);
        UnitPartialId unitPartialId = new UnitPartialId(RegistryUnitIdType.IMEI, "111");
        HashSet<UnitPartialId> unitPartialIds = new HashSet<>();
        unitPartialIds.add(unitPartialId);
        RegistryUnitId registryUnitId = new RegistryUnitId(unitPartialIds);
        identifier.setIdentifiers(registryUnitId);
        HashSet<Identifier> identifiers = new HashSet<>();
        identifiers.add(identifier);
        requestItem.setRequestItemIdentifiers(identifiers);
        shopRequest.setItems(Collections.singleton(requestItem));
        Mockito.when(shopRequestFetchingService.findAllByInboundIdAndSupplierId(1, 1,
                Arrays.asList(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CANCELLED,
                        RequestStatus.INVALID)))
                .thenReturn(Collections.singleton(shopRequest));
        List<TransferWithCisesDTO> transferWithCises =
                transferService.getTransferWithCises(1, 1, "111", Collections.singleton("111"));
        assertEquals(transferWithCises.size(), 0);
    }

    @Test
    void shouldFind2CisesInOneShopReq() {
        init();
        ShopRequest shopRequest = new ShopRequest();
        RequestItem requestItem = new RequestItem();
        requestItem.setArticle("111");
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DECLARED);
        UnitPartialId unitPartialId2 = new UnitPartialId(RegistryUnitIdType.CIS, "222");
        UnitPartialId unitPartialId = new UnitPartialId(RegistryUnitIdType.CIS, "111");
        HashSet<UnitPartialId> unitPartialIds = new HashSet<>();
        unitPartialIds.add(unitPartialId);
        unitPartialIds.add(unitPartialId2);
        RegistryUnitId registryUnitId = new RegistryUnitId(unitPartialIds);
        identifier.setIdentifiers(registryUnitId);
        HashSet<Identifier> identifiers = new HashSet<>();
        identifiers.add(identifier);
        requestItem.setRequestItemIdentifiers(identifiers);
        shopRequest.setItems(Collections.singleton(requestItem));
        Mockito.when(shopRequestFetchingService.findAllByInboundIdAndSupplierId(1, 1,
                Arrays.asList(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CANCELLED,
                        RequestStatus.INVALID)))
                .thenReturn(Collections.singleton(shopRequest));
        List<TransferWithCisesDTO> transferWithCises =
                transferService.getTransferWithCises(1, 1, "111", new HashSet<>(Arrays.asList("111", "222")));
        assertEquals(transferWithCises.size(), 1);
        assertEquals(transferWithCises.get(0).getCises().size(), 2);
    }

    @Test
    void shouldFindCisesInDifferentShopReq() {
        init();
        ShopRequest shopRequest = new ShopRequest();
        shopRequest.setId(1L);
        ShopRequest shopRequest2 = new ShopRequest();
        shopRequest.setId(2L);
        RequestItem requestItem = new RequestItem();
        requestItem.setArticle("111");
        Identifier identifier = new Identifier();
        identifier.setType(IdentifierType.DECLARED);
        UnitPartialId unitPartialId2 = new UnitPartialId(RegistryUnitIdType.CIS, "222");
        UnitPartialId unitPartialId = new UnitPartialId(RegistryUnitIdType.CIS, "111");
        HashSet<UnitPartialId> unitPartialIds = new HashSet<>();
        unitPartialIds.add(unitPartialId);
        unitPartialIds.add(unitPartialId2);
        RegistryUnitId registryUnitId = new RegistryUnitId(unitPartialIds);
        identifier.setIdentifiers(registryUnitId);
        HashSet<Identifier> identifiers = new HashSet<>();
        identifiers.add(identifier);
        requestItem.setRequestItemIdentifiers(identifiers);
        shopRequest.setItems(Collections.singleton(requestItem));
        shopRequest2.setItems(Collections.singleton(requestItem));
        Mockito.when(shopRequestFetchingService.findAllByInboundIdAndSupplierId(1, 1,
                Arrays.asList(RequestStatus.REJECTED_BY_SERVICE, RequestStatus.CANCELLED,
                        RequestStatus.INVALID)))
                .thenReturn(Arrays.asList(shopRequest, shopRequest2));
        List<TransferWithCisesDTO> transferWithCises =
                transferService.getTransferWithCises(1, 1, "111", new HashSet<>(Arrays.asList("111", "222")));
        assertEquals(transferWithCises.size(), 2);
        assertEquals(transferWithCises.get(0).getCises().size(), 2);
        assertEquals(transferWithCises.get(1).getCises().size(), 2);
    }
}
