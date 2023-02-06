package ru.yandex.market.pers.pay.tms;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.ConsumerReadResponse;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.pers.pay.MockUtils;
import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayEntityState;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayUserType;
import ru.yandex.market.pers.pay.model.dto.PaymentEntityStateEventDto;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.util.FormatUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.pay.model.PersPayEntityState.APPROVED;
import static ru.yandex.market.pers.pay.model.PersPayEntityState.CREATED;
import static ru.yandex.market.pers.pay.model.PersPayEntityState.READY;
import static ru.yandex.market.pers.pay.model.PersPayEntityState.REJECTED;
import static ru.yandex.market.pers.pay.model.PersPayEntityState.valueOf;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 19.03.2021
 */
public class PaymentContentStateLbConsumerExecutorTest extends PersPayTest {
    public static final long USER_ID = 234234;
    public static final long MODEL_ID = 5635634;

    @Autowired
    private JdbcTemplate tmsJdbcTemplate;

    @Autowired
    private PaymentContentStateLbConsumerExecutor executor;

    @Autowired
    private LogbrokerClientFactory clientFactory;

    @Autowired
    private TmsPaymentService paymentService;

    @Test
    public void testConsuming() throws Exception {
        MockUtils.mockConsumer(clientFactory, List.of(
            mockResponse(1, List.of(
                buildDto(USER_ID, MODEL_ID, READY, 2),
                buildDto(USER_ID, MODEL_ID, CREATED, 1),
                buildDto(USER_ID, MODEL_ID + 1, APPROVED, 1)
            )),
            mockResponse(2, List.of(
                buildDto(USER_ID, MODEL_ID + 1, REJECTED, 3),
                buildDto(USER_ID, MODEL_ID + 2, READY, 4)
            ))
        ));

        executor.consumeContentStates();

        List<PaymentEntityStateEventDto> entities = readAllStates();

        assertEquals(List.of(
            buildDto(USER_ID, MODEL_ID, READY, 2),
            buildDto(USER_ID, MODEL_ID + 1, REJECTED, 3),
            buildDto(USER_ID, MODEL_ID + 2, READY, 4)),
            entities);
    }

    @Test
    public void testOverwriteState() {
        PaymentEntityStateEventDto[] dto = {
            buildDto(USER_ID, MODEL_ID, APPROVED, 2),
            buildDto(USER_ID, MODEL_ID, READY, 1),
            buildDto(USER_ID, MODEL_ID, REJECTED, 3)
        };

        paymentService.saveContentStateChanges(List.of(dto[0]));
        assertEquals(APPROVED, readAllStates().get(0).getState());

        paymentService.saveContentStateChanges(List.of(dto[1]));
        assertEquals(APPROVED, readAllStates().get(0).getState());

        paymentService.saveContentStateChanges(List.of(dto[2]));
        assertEquals(REJECTED, readAllStates().get(0).getState());
    }

    @NotNull
    private List<PaymentEntityStateEventDto> readAllStates() {
        return tmsJdbcTemplate.query(
            "select *\n" +
                "from pay.content_state\n" +
                "order by upd_time",
            (rs, rowNum) -> {
                return new PaymentEntityStateEventDto(
                    new PersPayUser(
                        PersPayUserType.valueOf(rs.getInt("user_type")),
                        rs.getString("user_id")
                    ),
                    new PersPayEntity(
                        PersPayEntityType.valueOf(rs.getInt("entity_type")),
                        rs.getString("entity_id")
                    ),
                    valueOf(rs.getInt("state")),
                    rs.getString("content_id"),
                    rs.getTimestamp("upd_time").getTime()
                );
            }
        );
    }

    private ConsumerReadResponse mockResponse(long order, List<PaymentEntityStateEventDto> data) {
        return new ConsumerReadResponse(
            List.of(new MessageBatch("test", 1,
                data.stream()
                    .map(this::toData)
                    .collect(Collectors.toList()))),
            order
        );
    }

    private MessageData toData(PaymentEntityStateEventDto dto) {
        return new MessageData(
            FormatUtils.toJson(dto).getBytes(),
            0,
            new MessageMeta(null, 1, 0, 0, null, CompressionCodec.RAW, Map.of())
        );
    }

    private PaymentEntityStateEventDto buildDto(long userId, long modelId, PersPayEntityState state, long timestampMs) {
        return new PaymentEntityStateEventDto(
            new PersPayUser(PersPayUserType.UID, userId),
            new PersPayEntity(PersPayEntityType.MODEL_GRADE, modelId),
            state,
            "" + userId + "-" + modelId,
            timestampMs
        );
    }

}
