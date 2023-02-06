package ru.yandex.direct.grid.processing.service.client;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.bidmodifiers.service.BidModifierService;
import ru.yandex.direct.core.entity.campaign.repository.CampaignRepository;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.feature.service.FeatureManagingService;
import ru.yandex.direct.core.entity.page.service.PageService;
import ru.yandex.direct.core.entity.user.service.BlackboxUserService;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.sharding.ShardSupport;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.grid.processing.service.client.validation.SaveChoiceFromConversionModifiersPopupValidationService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationResultConversionService;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

@GridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ClientDataServiceGetClientIdsByLoginsAndUidsTest {
    private static final String NONEXISTENT_LOGIN = "nonexistent login";
    private static final String CLIENT_2_SECOND_LOGIN = "client_2 second login";

    @Autowired
    private Steps steps;
    @Autowired
    private ShardSupport shardSupport;
    @Autowired
    private PpcPropertiesSupport ppcPropertySupport;
    @Autowired
    private ShardHelper shardHelper;
    @Autowired
    private CampaignRepository campaignRepository;
    @Autowired
    private FeatureManagingService featureManagingService;
    @Autowired
    private CampaignTypedRepository campaignTypedRepository;
    @Autowired
    private BidModifierService bidModifierService;
    @Autowired
    private SaveChoiceFromConversionModifiersPopupValidationService
            saveChoiceFromConversionModifiersPopupValidationService;
    @Mock
    private BlackboxUserService blackboxUserService;
    @Mock
    private PageService pageService;
    @Mock
    private GridValidationResultConversionService gridValidationResultConversionService;
    private ClientDataService clientDataService;
    private ClientInfo client1;
    private ClientInfo client2;

    @Before
    public void init() {
        client1 = steps.clientSteps().createDefaultClient();
        client2 = steps.clientSteps().createDefaultClient();

        MockitoAnnotations.initMocks(this);
        doReturn(Optional.empty()).when(blackboxUserService).getUidByLogin(anyString());
        doReturn(Optional.of(client2.getUid())).when(blackboxUserService).getUidByLogin(eq(CLIENT_2_SECOND_LOGIN));

        clientDataService = createClientDataService();
    }


    @Test
    public void getWithoutPassport() {
        List<Long> expectedClientIds = getExpectedClientIds(client1, client2);
        Collection<Long> actualClientIds = getActualClientIds(client1, client2.getLogin());
        assertThat(actualClientIds, containsInAnyOrder(expectedClientIds.toArray()));
    }


    @Test
    public void foundLoginInPassport() {
        List<Long> expectedClientIds = getExpectedClientIds(client1, client2);
        Collection<Long> actualClientIds = getActualClientIds(client1, CLIENT_2_SECOND_LOGIN);
        assertThat(actualClientIds, containsInAnyOrder(expectedClientIds.toArray()));
    }


    @Test
    public void nowhereFoundLogin() {
        List<Long> expectedClientIds = getExpectedClientIds(client1);
        Collection<Long> actualClientIds = getActualClientIds(client1, NONEXISTENT_LOGIN);
        assertThat(actualClientIds, containsInAnyOrder(expectedClientIds.toArray()));
    }

    private List<Long> getExpectedClientIds(ClientInfo... clients) {
        return Arrays.stream(clients)
                .map(client -> client.getClientId().asLong())
                .collect(toList());
    }

    private Collection<Long> getActualClientIds(ClientInfo searchByUidList,
                                                String searchByLoginList) {
        return clientDataService.getClientIdsByUidsAndLogins(
                singleton(searchByUidList.getUid()),
                singleton(searchByLoginList));

    }


    private ClientDataService createClientDataService() {
        return new ClientDataService(shardHelper, shardSupport,
                null,
                null,
                null,
                null,
                null,
                null,
                campaignRepository, campaignTypedRepository, null,
                null,
                null,
                featureManagingService, null,
                null,
                null,
                null,
                null,
                null,
                null,
                blackboxUserService,
                null,
                null,
                pageService, gridValidationResultConversionService,
                null, null, ppcPropertySupport, null, null, null, null, null, null, null, null, null,
                null, null, null, bidModifierService, saveChoiceFromConversionModifiersPopupValidationService, null, null, null);
    }
}
