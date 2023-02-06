package ru.yandex.market.delivery.transport_manager.queue.task.transportation.register;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.exception.FFWFRestrictionsBrokenException;
import ru.yandex.market.delivery.transport_manager.queue.base.exception.DbQueueTaskExecutionException;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.outbound.PutOutboundRegisterConsumer;
import ru.yandex.market.delivery.transport_manager.queue.task.transportation.register.outbound.PutOutboundRegisterDto;
import ru.yandex.market.delivery.transport_manager.util.DbQueueUtils;
import ru.yandex.market.ff.client.FulfillmentWorkflowAsyncClientApi;
import ru.yandex.market.ff.client.dto.ResourceIdDto;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCount;
import ru.yandex.market.logistic.gateway.common.model.common.UnitCountType;
import ru.yandex.market.logistic.gateway.common.model.common.UnitInfo;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.OutboundRegistry;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.RegistryItem;
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class PutOutboundRegisterConsumerTest extends AbstractContextualTest {
    public static final long UNIT_ID = 2L;

    @Autowired
    private PutOutboundRegisterConsumer consumer;

    @Autowired
    private FulfillmentWorkflowAsyncClientApi ffwfClient;

    @Test
    @DatabaseSetup({
        "/repository/register/put_outbound_register.xml",
        "/repository/register/put_outbound_register_partner_ff.xml",
    })
    @ExpectedDatabase(value = "/repository/register/after/put_outbound_register.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successfulPutOutboundRegisterFf() {
        successPutFfOutboundRegistry();
    }

    @Test
    @DatabaseSetup({
        "/repository/register/put_outbound_register.xml",
        "/repository/register/put_outbound_register_partner_dc.xml",
    })
    @ExpectedDatabase(value = "/repository/register/after/put_outbound_register.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successfulPutOutboundRegisterDc() {
        successPutFfOutboundRegistry();
    }

    private void successPutFfOutboundRegistry() {
        when(ffwfClient.putFfOutboundRegistry(any())).thenReturn(ResourceIdDto.builder().yandexId("123").build());

        TaskExecutionResult execute = consumer.execute(
            DbQueueUtils.createTask(new PutOutboundRegisterDto(UNIT_ID))
        );
        softly.assertThat(execute).isEqualTo(TaskExecutionResult.finish());

        verifyCountTypesFf();
    }

    private void verifyCountTypesFf() {
        ArgumentCaptor<OutboundRegistry> captor = ArgumentCaptor.forClass(OutboundRegistry.class);
        verify(ffwfClient).putFfOutboundRegistry(captor.capture());
        verifyCountTypes(captor.getValue().getItems().stream().map(RegistryItem::getUnitInfo));
    }

    private void verifyCountTypes(Stream<UnitInfo> unitInfoStream) {
        Set<UnitCountType> countTypes = unitInfoStream
            .map(UnitInfo::getCounts)
            .flatMap(Collection::stream)
            .map(UnitCount::getCountType)
            .collect(Collectors.toSet());
        softly.assertThat(countTypes).containsAll(Set.of(UnitCountType.FIT, UnitCountType.DEFECT));
    }

    @Test
    @DatabaseSetup({
        "/repository/register/put_outbound_register.xml",
        "/repository/register/put_outbound_register_partner_dropship.xml",
    })
    @ExpectedDatabase(value = "/repository/register/after/put_outbound_register.xml", assertionMode =
        DatabaseAssertionMode.NON_STRICT_UNORDERED)
    void successfulPutOutboundRegisterDropship() {
        when(ffwfClient.putFfOutboundRegistry(any())).thenReturn(ResourceIdDto.builder().yandexId("123").build());

        TaskExecutionResult execute = consumer.execute(
            DbQueueUtils.createTask(new PutOutboundRegisterDto(UNIT_ID))
        );
        softly.assertThat(execute).isEqualTo(TaskExecutionResult.finish());

        verifyCountTypesFf();
    }

    @Test
    @DatabaseSetup({
        "/repository/register/put_outbound_register_incorrect_status.xml",
        "/repository/register/put_outbound_register_partner_dropship.xml",
    })
    @ExpectedDatabase(
        value = "/repository/register/put_outbound_register_incorrect_status.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void incorrectStatusPutOutboundRegisterDropship() {
        when(ffwfClient.putFfOutboundRegistry(any())).thenReturn(ResourceIdDto.builder().yandexId("123").build());

        softly.assertThatThrownBy(() -> consumer.execute(
                DbQueueUtils.createTask(new PutOutboundRegisterDto(UNIT_ID))
            ))
            .isInstanceOf(DbQueueTaskExecutionException.class)
            .hasCauseExactlyInstanceOf(FFWFRestrictionsBrokenException.class);

        verifyNoMoreInteractions(ffwfClient);
    }
}
