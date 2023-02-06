package ru.yandex.market.olap2.controller;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.olap2.dao.MetadataDao;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MonitorNewTablesCreationAttemptsTest {

    private MetadataDao metadataDao = mock(MetadataDao.class);
    private MonitorNewTablesCreationAttempts controller;

    @Before
    public void setup() {
        controller = new MonitorNewTablesCreationAttempts(metadataDao);
    }

    @Test
    public void noAttemptsOk() {
        when(metadataDao.existRejectedWithNewVerticaTables()).thenReturn(false);
        ResponseEntity<String> stringResponseEntity = controller.newVerticaTables();
        assertThat(stringResponseEntity, notNullValue());
        assertTrue(stringResponseEntity.getStatusCode().is2xxSuccessful());
        assertEquals(stringResponseEntity.getBody(), JugglerConstants.OK);
    }

    @Test
    public void presenAttemptsCRIT() {
        when(metadataDao.existRejectedWithNewVerticaTables()).thenReturn(true);
        ResponseEntity<String> stringResponseEntity = controller.newVerticaTables();
        assertThat(stringResponseEntity, notNullValue());
        assertTrue(stringResponseEntity.getStatusCode().is2xxSuccessful());
        assertThat(stringResponseEntity.getBody(), startsWith(JugglerConstants.CRIT));
        assertThat(stringResponseEntity.getBody(), containsString("Attempts to load new tables to vertica are " +
                "present"));
    }
}
