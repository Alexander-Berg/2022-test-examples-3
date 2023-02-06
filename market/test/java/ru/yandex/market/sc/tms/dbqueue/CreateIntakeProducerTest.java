package ru.yandex.market.sc.tms.dbqueue;


import java.nio.charset.StandardCharsets;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.sc.core.dbqueue.ScQueueType;
import ru.yandex.market.sc.core.dbqueue.ff.CreateIntakePayload;
import ru.yandex.market.sc.core.dbqueue.ff.CreateIntakeProducer;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.tms.test.EmbeddedDbTmsTest;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static ru.yandex.market.tpl.common.util.TplObjectMappers.TPL_DB_OBJECT_MAPPER;

@EmbeddedDbTmsTest
public class CreateIntakeProducerTest {
    @Autowired
    CreateIntakeProducer createIntakeProducer;

    @Autowired
    TestFactory testFactory;

    @Autowired
    DbQueueTestUtil dbQueueTestUtil;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockBean
    RestTemplate restTemplate;

    @Test
    @Disabled
    @SneakyThrows
    void createIntakeProducerTest() {
        var sortingCenter = testFactory.storedSortingCenter();
        var deliveryService = testFactory.storedDeliveryService();
        jdbcTemplate.update("insert into  sc_ds_partner_mapping values (?, ?, ?, ?)",
                sortingCenter.getId(),
                deliveryService.getId(),
                sortingCenter.getToken(),
                "ds_secret_token");
        String rawInput = IOUtils.toString(this.getClass().getResourceAsStream("create_intake.json"), StandardCharsets.UTF_8);

        CreateIntakePayload payload = TPL_DB_OBJECT_MAPPER.readValue(rawInput, CreateIntakePayload.class);
        createIntakeProducer.produceSingle(payload);
        dbQueueTestUtil.executeSingleQueueItem(ScQueueType.CREATE_INTAKE);

        Mockito.when(restTemplate.exchange(any(), any(), any(), (Class<Object>) any()))
                .thenReturn(new ResponseEntity<Object>(HttpStatus.OK));

        InOrder inOrder = Mockito.inOrder(restTemplate);
        inOrder.verify(restTemplate, times(1)).getMessageConverters();
        inOrder.verify(restTemplate, times(1)).exchange(
                anyString(),
                any(),
                any(),
                eq(String.class));
    }
 }
