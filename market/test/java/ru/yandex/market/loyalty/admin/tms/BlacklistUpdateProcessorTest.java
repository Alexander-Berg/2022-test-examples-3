package ru.yandex.market.loyalty.admin.tms;

import java.io.IOException;
import java.util.HashSet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.config.Antifraud;
import ru.yandex.market.loyalty.core.dao.UserBlackListDao;
import ru.yandex.market.loyalty.core.model.BlacklistRecord;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class BlacklistUpdateProcessorTest extends MarketLoyaltyAdminMockedDbTest {
    @Autowired
    private BlacklistUpdateProcessor blacklistUpdateProcessor;
    @Autowired
    @Antifraud
    @Qualifier("slowAntifraudRestTemplate")
    private RestTemplate restTemplate;
    @Autowired
    private UserBlackListDao userBlackListDao;
    @Autowired
    @MarketLoyaltyAdmin
    private ObjectMapper objectMapper;

    @SuppressWarnings("unchecked")
    @Test
    public void shouldUpdateAntifraudRules() {
        when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(),
                any(ParameterizedTypeReference.class)))
                .thenReturn(new ResponseEntity<>(new HashSet<>(singletonList(
                        new BlacklistRecord.Uid(111L))), HttpStatus.OK));

        blacklistUpdateProcessor.updateBlacklist();

        assertThat(userBlackListDao.getAllRecords(), contains(
                new BlacklistRecord.Uid(111L)
        ));
    }

    @Test
    public void shouldParseJson() throws IOException {
        BlacklistRecord record = objectMapper.readValue(
                "{\"type\":\"UID\", \"value\":\"123\"}", BlacklistRecord.class);

        assertThat(record, instanceOf(BlacklistRecord.Uid.class));
        assertEquals(123L, record.getValue());
    }

    @Test
    public void shouldParseUnknownJson() throws IOException {
        BlacklistRecord record = objectMapper.readValue(
                "{\"type\":\"SOME\", \"value\":\"123\"}", BlacklistRecord.class);

        assertThat(record, instanceOf(BlacklistRecord.Default.class));
        assertEquals("SOME", record.getType());
        assertEquals("123", record.getValue());
    }
}
