package ru.yandex.market.wms.servicebus.async.service;

import java.util.Optional;

import com.yandex.disk.rest.json.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.wms.servicebus.api.external.startrek.dto.IssueDto;
import ru.yandex.market.wms.servicebus.api.external.startrek.dto.Warehouse;
import ru.yandex.market.wms.servicebus.configuration.StarTrekClientProperties;
import ru.yandex.market.wms.servicebus.exception.StartrekTooMuchTasksCreatedException;
import ru.yandex.market.wms.servicebus.model.enums.IssueType;
import ru.yandex.market.wms.servicebus.service.StartrekService;
import ru.yandex.market.wms.servicebus.service.YandexDiskService;
import ru.yandex.misc.algo.ht.examples.OpenHashMap;
import ru.yandex.startrek.client.model.Issue;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnomaliesStartrekTaskServiceTest {


    private final StartrekService startrekService = mock(StartrekService.class);
    private final StarTrekClientProperties starTrekClientProperties = new StarTrekClientProperties();
    private final YandexDiskService yandexDiskService = mock(YandexDiskService.class);
    private final AnomaliesStartrekTaskService anomaliesStartrekTaskService =
            new AnomaliesStartrekTaskService(startrekService, starTrekClientProperties);


    AnomaliesStartrekTaskServiceTest() {
        starTrekClientProperties.setQueue1P(queueProperties(1L));
        starTrekClientProperties.setQueue3P(queueProperties(2L));
        starTrekClientProperties.setWarehouseName(Warehouse.SOFINO);
        ReflectionTestUtils.setField(anomaliesStartrekTaskService, "initiated", true);
    }

    @Test
    void createTask() throws StartrekTooMuchTasksCreatedException {
        //given
        when(startrekService.getOpenIssueByReceiptKey(any(), any(), eq(true))).thenReturn(Optional.empty());
        when(startrekService.create(any())).thenReturn(new Issue("id", null, "key", "summary", 1L,
                new OpenHashMap<>(), null));
        Resource resource = mock(Resource.class);
        when(resource.getPublicUrl()).thenReturn("https://ya.disk/weifqoiwuegf");
        when(yandexDiskService.createPublicDirectory(any())).thenReturn(resource);

        //when
        IssueDto issueDto = new IssueDto();
        issueDto.setOneP(true);
        anomaliesStartrekTaskService.createOrUpdateTask(issueDto);

        //then
        ArgumentCaptor<IssueDto> issueDtoCaptor = ArgumentCaptor.forClass(IssueDto.class);
        verify(startrekService, times(1)).create(issueDtoCaptor.capture());
        IssueDto value = issueDtoCaptor.getValue();
        assertThat("1p queue selected", value.getQueue().getComponentId(), equalTo(1L));
    }

    @Test
    void updateTask() throws StartrekTooMuchTasksCreatedException {
        //given
        when(startrekService.getOpenIssueByReceiptKey(any(), any(), eq(false))).thenReturn(Optional.of(issue()));
        when(startrekService.create(any())).thenReturn(new Issue("id", null, "key", "summary", 1L,
                new OpenHashMap<>(), null));
        Resource resource = mock(Resource.class);
        when(resource.getPublicUrl()).thenReturn("https://ya.disk/weifqoiwuegf");
        when(yandexDiskService.createPublicDirectory(any())).thenReturn(resource);

        //when
        ArgumentCaptor<IssueDto> issueDtoCaptor = ArgumentCaptor.forClass(IssueDto.class);
        anomaliesStartrekTaskService.createOrUpdateTask(new IssueDto());

        //then
        verify(startrekService, times(1)).update(any(), any(), issueDtoCaptor.capture());
        IssueDto dtoCaptorValue = issueDtoCaptor.getValue();
        StarTrekClientProperties.QueueProperties queue = dtoCaptorValue.getQueue();
        assertThat("issue type", queue.getIssueType(), equalTo(IssueType.TASK));
        assertThat("queue name", queue.getName(), equalTo("name"));
        assertThat("queue tags", queue.getTags(), hasItem("tag1." + queue.getComponentId()));
        assertThat("queue tags", queue.getTags(), hasItem("tag2." + queue.getComponentId()));
        assertThat("delivery number field", queue.getDeliveryNumberField(), equalTo("deliveryNumberField"));
        assertThat("component", queue.getComponentId(), equalTo(2L));
        assertThat("warehouse field", queue.getWarehouseField(), equalTo("warehouseField"));
        assertThat("quantityDivergence Field", queue.getQuantityDivergenceField(), equalTo("quantityDivergenceField"));
        assertThat("price field", queue.getAnomaliesPriceField(), equalTo("anomaliesPriceField"));
        assertThat("merchant field", queue.getMerchantField(), equalTo("merchantField"));
        assertThat("xdoc field", queue.getXDocField(), equalTo("xDocField"));
        assertThat("anomalies quantity field", queue.getAnomaliesQuantityField(), equalTo("anomaliesQuantityField"));
        assertThat("expected quantity field", queue.getExpectedQuantityField(), equalTo("expectedQuantityField"));
        assertThat("orderNumber field", queue.getOrderNumberField(), equalTo("orderNumberField"));
        assertThat("is 3P", dtoCaptorValue.isOneP(), equalTo(false));
        assertThat("tags", dtoCaptorValue.getTags(), hasItem("tag1." + queue.getComponentId()));
        assertThat("tags", dtoCaptorValue.getWarehouse(), equalTo(Warehouse.SOFINO));
    }

    private StarTrekClientProperties.QueueProperties queueProperties(long componentId) {
        StarTrekClientProperties.QueueProperties queueProperties = new StarTrekClientProperties.QueueProperties();
        queueProperties.setName("name");
        queueProperties.setIssueType(IssueType.TASK);
        queueProperties.setComponentId(componentId);
        queueProperties.setWarehouseField("warehouseField");
        queueProperties.setQuantityDivergenceField("quantityDivergenceField");
        queueProperties.setAnomaliesPriceField("anomaliesPriceField");
        queueProperties.setDeliveryNumberField("deliveryNumberField");
        queueProperties.setMerchantField("merchantField");
        queueProperties.setXDocField("xDocField");
        queueProperties.setAnomaliesQuantityField("anomaliesQuantityField");
        queueProperties.setExpectedQuantityField("expectedQuantityField");
        queueProperties.setOrderNumberField("orderNumberField");
        queueProperties.setTags(newArrayList("tag1." + componentId, "tag2." + componentId));
        return queueProperties;
    }

    private Issue issue() {
        return new Issue("id", null, "key", "summary", 1L, new OpenHashMap<>(), null);
    }
}
