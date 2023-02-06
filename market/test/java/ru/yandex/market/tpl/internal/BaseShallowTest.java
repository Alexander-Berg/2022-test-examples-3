package ru.yandex.market.tpl.internal;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalFindApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalRetrieveApi;
import ru.yandex.market.tpl.common.personal.client.api.DefaultPersonalStoreApi;
import ru.yandex.market.tpl.common.personal.client.tpl.EnrichPersonalDataService;
import ru.yandex.market.tpl.common.personal.client.tpl.PersonalExternalService;
import ru.yandex.market.tpl.common.personal.client.tpl.StorePersonalDataService;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxClient;
import ru.yandex.market.tpl.common.web.blackbox.BlackboxUser;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;
import ru.yandex.market.tpl.core.external.boxbot.LockerApi;
import ru.yandex.market.tpl.core.mvc.PartnerCompanyHandler;
import ru.yandex.market.tpl.core.mvc.ServiceTicketRequestHandler;
import ru.yandex.market.tpl.core.service.company.CompanyCachingService;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author kukabara
 */
public abstract class BaseShallowTest {

    @Autowired
    protected MockMvc mockMvc;
    @MockBean
    protected BlackboxClient blackboxClient;
    @MockBean
    protected PartnerCompanyHandler partnerCompanyHandler;
    @MockBean
    protected ServiceTicketRequestHandler serviceTicketRequestHandler;
    @MockBean
    protected ConfigurationProviderAdapter configurationProviderAdapter;
    @MockBean
    protected LockerApi lockerApi;
    @MockBean
    protected CompanyCachingService companyCachingService;
    @SpyBean
    protected StorePersonalDataService storePersonalDataService;
    @SpyBean
    protected EnrichPersonalDataService enrichPersonalDataService;
    @SpyBean
    protected PersonalExternalService personalExternalService;
    @MockBean
    protected DefaultPersonalStoreApi personalStoreApi;
    @MockBean
    protected DefaultPersonalRetrieveApi personalRetrieveApi;
    @MockBean
    protected DefaultPersonalFindApi personalFindApi;
    
    @BeforeEach
    public void beforeEach() {
        BlackboxUser blackboxUser = new BlackboxUser();
        blackboxUser.setLogin("login");
        when(blackboxClient.invokeUserinfo(any())).thenReturn(blackboxUser);
    }

    @SneakyThrows
    protected String getFileContent(String filename) {
        return IOUtils.toString(Objects.requireNonNull(getSystemResourceAsStream(filename)), StandardCharsets.UTF_8);
    }

}
