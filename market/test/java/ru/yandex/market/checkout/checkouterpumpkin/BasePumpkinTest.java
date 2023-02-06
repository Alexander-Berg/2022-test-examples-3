package ru.yandex.market.checkout.checkouterpumpkin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.yandex.ydb.auth.tvm.TvmAuthContext;
import com.yandex.ydb.table.result.ResultSetReader;
import com.yandex.ydb.table.values.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.inside.passport.tvm2.TvmClientCredentials;
import ru.yandex.market.checkout.checkouterpumpkin.config.SpringApplicationTestConfig;
import ru.yandex.market.checkout.checkouterpumpkin.config.YdbProperties;
import ru.yandex.market.checkout.checkouterpumpkin.model.CheckoutRequestStatus;
import ru.yandex.market.checkout.checkouterpumpkin.storage.CheckoutQueueDao;
import ru.yandex.market.checkout.checkouterpumpkin.storage.CheckoutRequestDao;
import ru.yandex.market.checkout.common.rest.TvmTicketProvider;
import ru.yandex.market.ydb.integration.YdbTemplate;
import ru.yandex.market.ydb.integration.context.initializer.YdbContainerContextInitializer;
import ru.yandex.market.ydb.integration.model.Field;
import ru.yandex.market.ydb.integration.model.TableDescription;
import ru.yandex.market.ydb.integration.query.YdbInsert;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static ru.yandex.market.ydb.integration.YdbTemplate.txRead;
import static ru.yandex.market.ydb.integration.query.YdbSelect.selectFrom;

@ActiveProfiles("test")
@SpringBootTest(classes = SpringApplicationTestConfig.class)
@ContextConfiguration(initializers = BasePumpkinTest.PumpkinYdbContainerContextInitializer.class)
@TestPropertySource(locations = "classpath:test-application.properties")
@AutoConfigureMockMvc(addFilters = false)
@ExtendWith(SpringExtension.class)
@MockBean(classes = {TvmClientCredentials.class, TvmAuthContext.class, Tvm2.class, TvmTicketProvider.class})
public abstract class BasePumpkinTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected TestableClock testableClock;

    @Autowired
    protected YdbTemplate ydbTemplate;

    @Autowired
    protected CheckoutRequestDao.Table requestTable;

    @Autowired
    protected CheckoutQueueDao.Table queueTable;

    @AfterEach
    public void cleanUp() {
        testableClock.clearFixed();
        ydbTemplate.truncateTable(requestTable, queueTable);
    }

    protected String loadResourceAsString(String path) throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(path);
        assertNotNull(stream, path + " not found");
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    protected List<Map<Field<?, ?>, Value<?>>> readTableData(TableDescription table) {
        return ydbTemplate.selectList(selectFrom(table), txRead(), queryResult -> {
            ResultSetReader rs = queryResult.getResultSet(0);
            List<Map<Field<?, ?>, Value<?>>> result = new ArrayList<>();
            while (rs.next()) {
                result.add(table.fields().stream()
                        .filter(field -> rs.getColumn(field.alias()).isOptionalItemPresent())
                        .collect(Collectors.toMap(
                                Function.identity(),
                                field -> (Value<?>) rs.getColumn(field.alias()).getValue().asOptional().get()
                        ))
                );
            }
            return result;
        });
    }

    protected void addCheckoutRequest(UUID id, Map<String, List<String>> headers,
                                      Map<String, List<String>> parameters, String bodyJson) {
        LocalDateTime now = LocalDateTime.now(testableClock);
        ydbTemplate.update(YdbInsert
                        .insert(requestTable, requestTable.getId(), requestTable.getHeaders(),
                                requestTable.getParameters(), requestTable.getBody(), requestTable.getCreatedAt(),
                                requestTable.getStatus())
                        .row(id, headers, parameters, bodyJson, now, CheckoutRequestStatus.IN_QUEUE),
                YdbTemplate.txWrite()
        );
        ydbTemplate.update(YdbInsert
                        .insert(queueTable, queueTable.getCreatedAt(), queueTable.getRequestId())
                        .row(now, id),
                YdbTemplate.txWrite()
        );
    }

    public static class PumpkinYdbContainerContextInitializer extends YdbContainerContextInitializer {

        static {
            prefix = YdbProperties.PREFIX;
        }
    }
}
