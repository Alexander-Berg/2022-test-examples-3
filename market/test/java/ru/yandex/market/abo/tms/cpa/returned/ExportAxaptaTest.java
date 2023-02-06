package ru.yandex.market.abo.tms.cpa.returned;


import java.util.ArrayList;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.abo.cpa.returned.ReturnedItem;
import ru.yandex.market.abo.cpa.returned.ReturnedItemList;
import ru.yandex.market.abo.cpa.returned.ReturnedRepo;
import ru.yandex.EmptyTest;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * @author imelnikov
 */
public class ExportAxaptaTest extends EmptyTest {

    @Mock
    JdbcTemplate axaptaJdbcTemplate;

    @InjectMocks
    @Autowired
    ExportAxaptaExecutor executor;

    @Autowired
    ReturnedRepo.ReturnedItemListRepo returnedItemListRepo;


    @BeforeEach
    public void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void markExported() throws Exception {

        ReturnedItemList list = new ReturnedItemList();
        list.setInboundRequestId(1l);
        list.setCreated(new Date());
        ReturnedItem item = new ReturnedItem();
        item.setOrderId(2l);
        var items = new ArrayList<ReturnedItem>();
        items.add(item);
        list.setItems(items);
        returnedItemListRepo.save(list);

        executor.doRealJob(null);

        verify(axaptaJdbcTemplate).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        assertTrue(returnedItemListRepo.findAllByInboundRequestIdNotNullAndExportTsIsNull().isEmpty());
    }

}
