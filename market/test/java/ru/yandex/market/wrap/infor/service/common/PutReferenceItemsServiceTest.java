package ru.yandex.market.wrap.infor.service.common;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import ru.yandex.market.logistic.api.model.fulfillment.Contractor;
import ru.yandex.market.logistic.api.model.fulfillment.Item;
import ru.yandex.market.logistic.api.model.fulfillment.UnitId;
import ru.yandex.market.wrap.infor.client.WrappedInforClient;
import ru.yandex.market.wrap.infor.client.model.BatchStorerDTO;
import ru.yandex.market.wrap.infor.client.model.StorerDTO;
import ru.yandex.market.wrap.infor.service.common.upsert.UpsertSkusService;
import ru.yandex.market.wrap.infor.service.inbound.converter.BatchStorerDTOConverter;
import ru.yandex.market.wrap.infor.service.inbound.converter.ContractorToBatchStorerDtoConverter;
import ru.yandex.market.wrap.infor.service.item.ItemMetaMappingService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PutReferenceItemsServiceTest {

    private final WrappedInforClient inforClient = mock(WrappedInforClient.class);
    private final UpsertSkusService upsertSkusService = mock(UpsertSkusService.class);
    private final ItemMetaMappingService itemMetaMappingService = mock(ItemMetaMappingService.class);
    private final PutReferenceItemsService putReferenceItemsService =
        new PutReferenceItemsService(
            new BatchStorerDTOConverter(),
            new ContractorToBatchStorerDtoConverter(),
            inforClient,
            upsertSkusService,
            itemMetaMappingService, 10);

    @Test
    void putReferenceItemsContainsDuplicatesItems() {
        ArrayList<Item> items = new ArrayList<>();
        items.add(getDuplicateItem());
        items.add(getDuplicateItem());

        putReferenceItemsService.putReferenceItems(items, false);

        verify(inforClient).upsertStorerList(any());
    }

    @Test
    void putReferenceItemsTooLongContractorNameItems() {
        ArrayList<Item> items = new ArrayList<>();
        items.add(getItemWithTooLongContractorName());
        ArgumentCaptor<BatchStorerDTO> batchStorerDtoCaptor = ArgumentCaptor.forClass(BatchStorerDTO.class);

        putReferenceItemsService.putReferenceItems(items, false);

        verify(inforClient, times(2)).upsertStorerList(batchStorerDtoCaptor.capture());
        List<BatchStorerDTO> batchStorerDtos = batchStorerDtoCaptor.getAllValues();
        BatchStorerDTO batchStorerDto = batchStorerDtos.get(1);
        assertFalse(batchStorerDto.getStorerList().isEmpty());
        StorerDTO storerDTO = batchStorerDto.getStorerList().get(0);
        assertEquals("TOO_LOOOOOOOOOOOONG_NAME_SHOULD_BE_CROP_TO_45", storerDTO.getCompany());
    }

    private Item getDuplicateItem() {
        return new Item.ItemBuilder("duplicate", 1, BigDecimal.ONE)
            .setUnitId(new UnitId("1", 1L, "1"))
            .setArticle("1")
            .build();
    }

    private Item getItemWithTooLongContractorName() {
        return new Item.ItemBuilder("duplicate", 1, BigDecimal.ONE)
            .setUnitId(new UnitId("1", 1L, "1"))
            .setArticle("1")
            .setContractor(new Contractor("1", "TOO_LOOOOOOOOOOOONG_NAME_SHOULD_BE_CROP_TO_45_SYMBOLS"))
            .build();
    }
}
