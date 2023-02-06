package ru.yandex.market.partner.reports;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.model.ModelService;
import ru.yandex.market.core.security.Campaignable;
import ru.yandex.market.core.security.model.DualUidable;
import ru.yandex.market.partner.reports.mc.ShowTopModelsClicksServantlet;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

public class ShowTopModelsClicksServantletTest extends FunctionalTest {
    //test data
    private static final long USER_ID = 123L;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    public void smokeTest() {
        ServResponse servResponse = spy(MockServResponse.class);
        var request = mock(ServRequest.class, withSettings().extraInterfaces(Campaignable.class));
        Campaignable campaignable = (Campaignable) request;
        doReturn(1L).when(campaignable).getCampaignId();
        doReturn(false).when(servResponse).hasErrors();
        when(((DualUidable) request).getEffectiveUid()).thenReturn(USER_ID);
        var servantlet = new ShowTopModelsClicksServantlet(
                mock(ModelService.class)
        );
        servantlet.setJdbcTemplate(jdbcTemplate);
        servantlet.processWithParams(request, servResponse);
    }
}
