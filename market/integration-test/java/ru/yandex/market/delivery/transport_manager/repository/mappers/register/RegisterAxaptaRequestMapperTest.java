package ru.yandex.market.delivery.transport_manager.repository.mappers.register;

import java.util.List;

import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaRequest;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaRequestPayload;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaResponse;
import ru.yandex.market.delivery.transport_manager.domain.entity.axapta.register.RegisterAxaptaResponseUnit;
import ru.yandex.market.delivery.transport_manager.domain.enums.CountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterAxaptaRequestStatus;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterAxaptaRequestType;
import ru.yandex.market.delivery.transport_manager.dto.Stock;

@SuppressWarnings("checkstyle:ParameterNumber")
@DatabaseSetup("/repository/register/register.xml")
class RegisterAxaptaRequestMapperTest extends AbstractContextualTest {
    @Autowired
    private RegisterAxaptaRequestMapper mapper;

    @ExpectedDatabase(
        value = "/repository/register/after/register_axapta_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @Test
    void persist() {
        mapper.persist(new RegisterAxaptaRequest()
            .setStatus(RegisterAxaptaRequestStatus.SENT)
            .setType(RegisterAxaptaRequestType.CHECK_AVAILABLE_QUANTITY)
            .setRegisterId(2L)
            .setExternalId(4L)
            .setPartnerId(1L)
            .setPayload(new RegisterAxaptaRequestPayload().setRegisterUnitIds(List.of(1001L, 1002L, 1003L))));
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request.xml")
    void getStatus() {
        softly.assertThat(mapper.getStatus(1001L)).isEqualTo(RegisterAxaptaRequestStatus.NEW);
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_axapta_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCount() {
        softly.assertThat(mapper.switchStatusReturningCount(
                1001L,
                RegisterAxaptaRequestStatus.NEW,
                RegisterAxaptaRequestStatus.SENT
            ))
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/register_axapta_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountWithoutPreviousStatusCheck() {
        softly.assertThat(mapper.switchStatusReturningCount(
                1001L,
                null,
                RegisterAxaptaRequestStatus.SENT
            ))
            .isEqualTo(1L);
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request.xml")
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void switchStatusReturningCountSkip() {
        softly.assertThat(mapper.switchStatusReturningCount(
                1002L,
                RegisterAxaptaRequestStatus.NEW,
                RegisterAxaptaRequestStatus.SENT
            ))
            .isEqualTo(0L);
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request_with_response.xml")
    void getById() {
        softly
            .assertThat(mapper.getById(1001L))
            .isEqualTo(
                new RegisterAxaptaRequest()
                    .setId(1001L)
                    .setStatus(RegisterAxaptaRequestStatus.RECEIVED)
                    .setType(RegisterAxaptaRequestType.CHECK_AVAILABLE_QUANTITY)
                    .setRegisterId(2L)
                    .setExternalId(4L)
                    .setPartnerId(1L)
                    .setPayload(new RegisterAxaptaRequestPayload().setRegisterUnitIds(List.of(1001L, 1002L, 1003L)))
                    .setResponse(new RegisterAxaptaResponse().setUnits(List.of(
                        unit("ssku1", 1L, null, 1, CountType.FIT, 100, null, null),
                        unit("ssku1", 1L, null, 1, CountType.DEFECT, 100, false, null),
                        unit("ssku2", 2L, null, 1, CountType.FIT, 100, false, 50),
                        unit("123.ssku3", 3L, "123", 1, CountType.FIT, 5, true, null),
                        unit("ssku3", 3L, null, 1, CountType.DEFECT, 0, true, 10)

                    )))
            );
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request.xml")
    void getByIdNullResponse() {
        softly
            .assertThat(mapper.getById(1001L))
            .isEqualTo(
                new RegisterAxaptaRequest()
                    .setId(1001L)
                    .setStatus(RegisterAxaptaRequestStatus.NEW)
                    .setType(RegisterAxaptaRequestType.CHECK_AVAILABLE_QUANTITY)
                    .setRegisterId(2L)
                    .setExternalId(4L)
                    .setPartnerId(1L)
                    .setPayload(new RegisterAxaptaRequestPayload().setRegisterUnitIds(List.of(1001L, 1002L, 1003L)))
                    .setResponse(null)
            );
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request_received.xml")
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request_with_response.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setResponse() {
        mapper.setResponse(1001L, new RegisterAxaptaResponse().setUnits(List.of(
            unit("ssku1", 1L, null, 1, CountType.FIT, 100, null, null),
            unit("ssku1", 1L, null, 1, CountType.DEFECT, 100, false, null),
            unit("ssku2", 2L, null, 1, CountType.FIT, 100, false, 50),
            unit("123.ssku3", 3L, "123", 1, CountType.FIT, 5, true, null),
            unit("ssku3", 3L, null, 1, CountType.DEFECT, 0, true, 10)
        )));
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request_no_external_id.xml")
    @ExpectedDatabase(
        value = "/repository/register/register_axapta_request.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void setExternalId() {
        mapper.setExternalId(1001L, 4L);
    }

    @Test
    @DatabaseSetup("/repository/register/register_axapta_request_with_transportation_task.xml")
    void getByTransportationTask() {
        softly.assertThat(mapper.getByTransportationTask(10001L))
            .containsExactly(1001L);
    }

    private RegisterAxaptaResponseUnit unit(
        String ssku,
        long merchantId,
        @Nullable String realMerchantId,
        int partnerId,
        CountType type,
        int availPhysicalQty,
        Boolean isMercury,
        Integer availMercuryQty

    ) {
        return new RegisterAxaptaResponseUnit(
            ssku,
            merchantId,
            realMerchantId,
            new Stock(partnerId, type),
            availPhysicalQty,
            isMercury,
            availMercuryQty
        );
    }
}
