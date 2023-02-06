package ru.yandex.market.ff.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.ListUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.ff.client.dto.CreateTransferForm;
import ru.yandex.market.ff.client.dto.CreateTransferItemForm;
import ru.yandex.market.ff.client.dto.RegistryUnitIdDTO;
import ru.yandex.market.ff.client.enums.CisReturnInboundInfoStatus;
import ru.yandex.market.ff.client.enums.RegistryUnitIdType;
import ru.yandex.market.ff.client.enums.StockType;
import ru.yandex.market.ff.client.enums.TransferCreationType;
import ru.yandex.market.ff.exception.http.BadRequestException;
import ru.yandex.market.ff.model.converter.IdentifierConverter;
import ru.yandex.market.ff.model.converter.RegistryUnitDTOConverter;
import ru.yandex.market.ff.model.dto.registry.RegistryUnitId;
import ru.yandex.market.ff.model.dto.registry.UnitPartialId;
import ru.yandex.market.ff.model.entity.CisReturnInboundInfo;
import ru.yandex.market.ff.model.entity.CisReturnInboundInfoItem;
import ru.yandex.market.ff.repository.CisReturnInboundInfoRepository;
import ru.yandex.market.ff.service.implementation.cisTransfer.RemoveCisFromAutoTransferService;

import static org.assertj.core.api.Assertions.assertThat;

class RemoveCisFromAutoTransferServiceTest {

    private final CisReturnInboundInfoRepository repository = Mockito.mock(CisReturnInboundInfoRepository.class);
    private final IdentifierConverter converter = new IdentifierConverter(new RegistryUnitDTOConverter());
    private final RemoveCisFromAutoTransferService service =
            new RemoveCisFromAutoTransferService(repository, converter);


    @BeforeEach
    public void init() {
        Mockito.when(repository.findByStatusAndSupplierIdAndInboundId(CisReturnInboundInfoStatus.NEW, 100L, 10L))
                .thenReturn(List.of(getCisReturnInboundInfo(getUnitPartialIds())));
        Mockito.when(repository.findByStatusAndSupplierIdAndInboundId(CisReturnInboundInfoStatus.NEW, 100L, 20L))
                .thenReturn(Collections.emptyList());
        Mockito.when(repository.findByStatusAndSupplierIdAndInboundId(CisReturnInboundInfoStatus.NEW, 100L, 30L))
                .thenReturn(List.of(
                        getCisReturnInboundInfo(getUnitPartialIds()),
                        getCisReturnInboundInfo(getUnitPartialIds()))
                );
        Mockito.when(repository.findByStatusAndSupplierIdAndInboundId(CisReturnInboundInfoStatus.NEW, 100L, 40L))
                .thenReturn(List.of(getCisReturnInboundInfo(
                        ListUtils.union(
                                getCisReturnInboundInfoItems(getUnitPartialIds()),
                                getCisReturnInboundInfoItems(getSingleUnitPartialId("cis22"))
                        ))
                ));
    }

    @Test
    void removeSuccess() {
        service.remove(getTransferRequest("cis1"));

        ArgumentCaptor<CisReturnInboundInfo> captor = ArgumentCaptor.forClass(CisReturnInboundInfo.class);
        Mockito.verify(repository).saveAndFlush(captor.capture());

        CisReturnInboundInfo actualSavedValue = captor.getValue();
        Set<UnitPartialId> actualParts = getActualParts(actualSavedValue);
        Assertions.assertEquals(1, actualParts.size());
        Assertions.assertEquals("cis2", actualParts.iterator().next().getValue());
    }

    @Test
    void removeWithCancelAutoTransfer() {
        Mockito.when(repository.findByStatusAndSupplierIdAndInboundId(CisReturnInboundInfoStatus.NEW, 100L, 10L))
                .thenReturn(getCisReturnInboundInfo());

        CreateTransferForm transferRequest = getTransferRequest("cis1");
        service.remove(transferRequest);

        ArgumentCaptor<CisReturnInboundInfo> captor = ArgumentCaptor.forClass(CisReturnInboundInfo.class);
        Mockito.verify(repository).saveAndFlush(captor.capture());

        CisReturnInboundInfo actualSavedValue = captor.getValue();
        Assertions.assertEquals(CisReturnInboundInfoStatus.CANCELED, actualSavedValue.getStatus());
    }

    @Test
    void requestContainsUnknownCis() {
        BadRequestException exception = Assertions.assertThrows(BadRequestException.class,
                () -> service.remove(getTransferRequest("cis100")));
        Assertions.assertTrue(
                exception.getMessage().contains(" does not contain next cises: [UnitPartialId(value=cis100, type=CIS)]"
                ));
    }

    @Test
    void noCisReturnInboundInfoFound() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> service.remove(getTransferRequest("cis1", 20L)));
        Assertions.assertEquals("There is no cisReturnInboundInfo by supplierId=100, inboundId=20",
                exception.getMessage());
    }

    @Test
    void foundMoreThanOneCisReturnInboundInfo() {
        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> service.remove(getTransferRequest("cis1", 30L)));
        Assertions.assertEquals("There are more than one cisReturnInboundInfo by supplierId=100, inboundId=30",
                exception.getMessage());
    }

    @Test
    void autoTransferHasMoreThanOneItemWithCis() {
        service.remove(getTransferRequest("cis1", 40L));

        List<CisReturnInboundInfoItem> expectedItems = ListUtils.union(
                getCisReturnInboundInfoItems(getSingleUnitPartialId("cis2")),
                getCisReturnInboundInfoItems(getSingleUnitPartialId("cis22"))
        );

        ArgumentCaptor<CisReturnInboundInfo> captor = ArgumentCaptor.forClass(CisReturnInboundInfo.class);
        Mockito.verify(repository).saveAndFlush(captor.capture());

        CisReturnInboundInfo actualSavedValue = captor.getValue();
        Assertions.assertEquals(CisReturnInboundInfoStatus.NEW, actualSavedValue.getStatus());
        assertThat(actualSavedValue.getItems()).usingFieldByFieldElementComparator().containsAll(expectedItems);
    }

    @Test
    void autoTransferRemoveAllCisInOneItem() {
        service.remove(getTransferRequest("cis22", 40L));
        CisReturnInboundInfoItem itemWithoutCis = new CisReturnInboundInfoItem();
        itemWithoutCis.setArticle("sku1");

        List<CisReturnInboundInfoItem> expectedItems = ListUtils.union(
                getCisReturnInboundInfoItems(getUnitPartialIds()),
                List.of(itemWithoutCis)
        );

        ArgumentCaptor<CisReturnInboundInfo> captor = ArgumentCaptor.forClass(CisReturnInboundInfo.class);
        Mockito.verify(repository).saveAndFlush(captor.capture());

        CisReturnInboundInfo actualSavedValue = captor.getValue();
        Assertions.assertEquals(CisReturnInboundInfoStatus.NEW, actualSavedValue.getStatus());
        assertThat(actualSavedValue.getItems()).usingFieldByFieldElementComparator().containsAll(expectedItems);
    }

    private ArrayList<CisReturnInboundInfo> getCisReturnInboundInfo() {
        ArrayList<CisReturnInboundInfo> cisReturnInboundInfos = new ArrayList<>();
        cisReturnInboundInfos.add(getCisReturnInboundInfo(getSingleUnitPartialId("cis1")));
        return cisReturnInboundInfos;
    }

    private Set<UnitPartialId> getActualParts(CisReturnInboundInfo actualSavedValue) {
        return actualSavedValue.getItems().get(0)
                .getIdentifiers()
                .getParts();
    }

    private CisReturnInboundInfo getCisReturnInboundInfo(Set<UnitPartialId> unitPartialIds) {
        return getCisReturnInboundInfo(getCisReturnInboundInfoItems(unitPartialIds));
    }

    private CisReturnInboundInfo getCisReturnInboundInfo(List<CisReturnInboundInfoItem> items) {
        CisReturnInboundInfo cisReturnInboundInfo = new CisReturnInboundInfo();
        cisReturnInboundInfo.setStatus(CisReturnInboundInfoStatus.NEW);
        cisReturnInboundInfo.setInboundId(10L);
        cisReturnInboundInfo.setSupplierId(100L);
        cisReturnInboundInfo.setItems(items);
        return cisReturnInboundInfo;
    }

    private List<CisReturnInboundInfoItem> getCisReturnInboundInfoItems(Set<UnitPartialId> unitPartialIds) {
        CisReturnInboundInfoItem item = new CisReturnInboundInfoItem();
        item.setArticle("sku1");
        item.setIdentifiers(new RegistryUnitId(unitPartialIds));

        return List.of(item);
    }

    private Set<UnitPartialId> getUnitPartialIds() {
        return Set.of(
                UnitPartialId.builder()
                        .type(RegistryUnitIdType.CIS)
                        .value("cis1")
                        .build(),
                UnitPartialId.builder()
                        .type(RegistryUnitIdType.CIS)
                        .value("cis2")
                        .build()
        );
    }

    private Set<UnitPartialId> getSingleUnitPartialId(String value) {
        return Set.of(
                UnitPartialId.builder()
                        .type(RegistryUnitIdType.CIS)
                        .value(value)
                        .build()
        );
    }

    private CreateTransferForm getTransferRequest(String cis) {
        CreateTransferForm request = new CreateTransferForm();
        request.setServiceId(1L);
        request.setStockTypeFrom(StockType.CIS_QUARANTINE);
        request.setStockTypeTo(StockType.DEFECT);
        request.setInboundId(10L);
        request.setTransferCreationType(TransferCreationType.CIS_TRANSFER);
        request.setSupplierId(100L);
        request.setItems(getItems(cis));
        return request;
    }


    private CreateTransferForm getTransferRequest(String cis, long inboundId) {
        CreateTransferForm request = new CreateTransferForm();
        request.setServiceId(1L);
        request.setStockTypeFrom(StockType.CIS_QUARANTINE);
        request.setStockTypeTo(StockType.DEFECT);
        request.setInboundId(inboundId);
        request.setTransferCreationType(TransferCreationType.CIS_TRANSFER);
        request.setSupplierId(100L);
        request.setItems(getItems(cis));
        return request;
    }

    private List<CreateTransferItemForm> getItems(String cis) {
        return List.of(getItem(cis));
    }

    private CreateTransferItemForm getItem(String cis) {
        return new CreateTransferItemForm("sku1", 5, RegistryUnitIdDTO.of(RegistryUnitIdType.CIS, cis));
    }
}
