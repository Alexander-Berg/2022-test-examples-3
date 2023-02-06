package ru.yandex.market.tsup.repository.mappers;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.tsup.AbstractContextualTest;
import ru.yandex.market.tsup.domain.entity.user_log.UserActivityLog;
import ru.yandex.market.tsup.service.internal.AddressSource;

public class UserActivityLogMapperTest extends AbstractContextualTest {
    @Autowired
    private UserActivityLogMapper logMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @ExpectedDatabase(
        value = "/repository/user_acivity_log/after_insert.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void insert() throws IOException {

        UserActivityLog log = new UserActivityLog()
            .setLogin("aidenne")
            .setMethod("GET")
            .setAddress("127.0.0.1")
            .setParams(objectMapper.readTree("{\"id\":1}"))
            .setBody(objectMapper.readTree("{\"body\":\"info\"}"))
            .setHandler("HandlerPath")
            .setRequestPath("trips")
            .setAddressSource(AddressSource.REMOTE_ADDRESS)
            .setStatus(200);

        logMapper.insert(log);
    }
}
