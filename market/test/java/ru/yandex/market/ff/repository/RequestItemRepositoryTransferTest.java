package ru.yandex.market.ff.repository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.RequestItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Интеграционные тесты для {@link RequestItemRepository}.
 */
class RequestItemRepositoryTransferTest extends IntegrationTest {

    @Autowired
    private RequestItemRepository repository;

    @Test
    @DatabaseSetup("classpath:repository/request-transfer-item/before.xml")
    void findAllByRequestId() {
        List<RequestItem> items = repository.findAllByRequestIdOrderById(7L);
        List<Long> itemIds = items.stream().map(RequestItem::getId).collect(Collectors.toList());

        assertThat(items.size(), equalTo(2));
        assertEquals(itemIds, Arrays.asList(1L, 2L));

        Optional<RequestItem> itemOptional = items.stream().filter(z -> z.getId().equals(1L)).findFirst();
        assertTrue(itemOptional.isPresent());
        RequestItem item = itemOptional.get();
        assertEquals(item.getRequestId(), Long.valueOf(7L));
        assertEquals(item.getCount(), 3);
        assertEquals(item.getFactCount(), Integer.valueOf(3));
        assertEquals(item.getSku(), Long.valueOf(190L));
    }

}
