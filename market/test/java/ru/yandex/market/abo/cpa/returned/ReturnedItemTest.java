package ru.yandex.market.abo.cpa.returned;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author imelnikov
 */
public class ReturnedItemTest extends EmptyTest {

    @Autowired
    private ReturnedRepo.ReturnedItemListRepo listRepo;
    @Autowired
    ReturnedRepo.ReturnedItemRepo itemRepo;
    @Autowired
    ReturnedRepo.ReturnedItemInfoRepo itemInfoRepo;

    @Test
    public void dao() {
        Long orderId = 1L;

        ReturnedItem item = new ReturnedItem();
        item.setOrderId(orderId);
        item.setTicketId("marchrut ticket");
        item.setPackaging("Целая");

        ReturnedItemList list = new ReturnedItemList();
        list.setItems(Arrays.asList(item));
        list.setCreated(new Date());

        long id = listRepo.save(list).getId();
        assertEquals(orderId, listRepo.findByIdOrNull(id).getItems().get(0).getOrderId());

        list = listRepo.findAll().get(0);
        list.getItems().stream().allMatch(it -> it.getItemInfo() != null);
    }

    @Test
    public void emptyInfo() {
        itemRepo.save(new ReturnedItem());
        itemRepo.save(new ReturnedItem());
        itemRepo.save(new ReturnedItem());

        List<ReturnedItemInfo> list = itemRepo.findAll().stream()
                .map(ReturnedItem::getItemInfo)
                .peek(info -> info.setOrderReturnId(1l))
                .collect(Collectors.toList());

        itemInfoRepo.saveAll(list);

        itemRepo.findAll().stream().allMatch(it -> it.getItemInfo() != null);
    }
}
