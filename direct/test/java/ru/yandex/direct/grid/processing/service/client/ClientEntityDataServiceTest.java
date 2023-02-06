package ru.yandex.direct.grid.processing.service.client;

import java.util.Collections;
import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.vcard.model.Vcard;
import ru.yandex.direct.core.entity.vcard.service.VcardService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdVcardFilter;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.GdVcardsContainer;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddAddress;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddPhone;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddVcard;
import ru.yandex.direct.grid.processing.model.cliententity.vcard.mutation.GdAddVcards;
import ru.yandex.direct.grid.processing.service.client.converter.VcardDataConverter;
import ru.yandex.direct.grid.processing.service.client.validation.ClientEntityValidationService;
import ru.yandex.direct.grid.processing.service.validation.GridValidationService;
import ru.yandex.direct.grid.processing.util.ContextHelper;
import ru.yandex.direct.multitype.entity.LimitOffset;
import ru.yandex.direct.result.MassResult;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.grid.processing.service.client.ClientEntityDataService.CLIENT_ENTITY_FETCH_LIMIT;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class ClientEntityDataServiceTest {

    private static GridGraphQLContext gridGraphQLContext;
    private static GdClientInfo clientInfo;
    private static User operator;
    private static ClientId clientId;

    @Mock
    private GridValidationService gridValidationService;

    @Mock
    private ClientEntityValidationService clientEntityCalloutValidationService;

    @Mock
    private VcardService vcardService;

    @InjectMocks
    private ClientEntityDataService clientEntityDataService;

    @BeforeClass
    public static void beforeClass() {
        gridGraphQLContext = ContextHelper.buildDefaultContext();
        clientInfo = gridGraphQLContext.getQueriedClient();
        clientId = ClientId.fromLong(clientInfo.getId());
        operator = gridGraphQLContext.getOperator();
    }

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void getVcardsTest() {
        GdVcardsContainer input = new GdVcardsContainer()
                .withFilter(new GdVcardFilter()
                        .withVcardIdIn(Collections.singleton(RandomNumberUtils.nextPositiveLong()))
                );

        clientEntityDataService.getVcards(operator.getUid(), clientInfo, input);

        verify(vcardService)
                .getVcards(operator.getUid(), clientId, input.getFilter().getVcardIdIn(),
                        LimitOffset.limited(CLIENT_ENTITY_FETCH_LIMIT));
    }

    @Test
    public void addVcardsTest() {
        doReturn(MassResult.emptyMassAction())
                .when(vcardService).addVcardsPartial(anyList(), eq(operator.getUid()), eq(clientId));
        GdAddVcards input = new GdAddVcards()
                .withVcardAddItems(Collections.singletonList(
                        new GdAddVcard()
                                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                                .withWorkTimes(Collections.emptyList())
                                .withPhone(new GdAddPhone())
                                .withAddress(new GdAddAddress())
                        )
                );

        clientEntityDataService.addVcards(operator.getUid(), clientId, input);

        List<Vcard> expectedVcards = mapList(input.getVcardAddItems(), VcardDataConverter::toVcard);
        verify(vcardService)
                .addVcardsPartial(expectedVcards, operator.getUid(), clientId);
        verify(gridValidationService)
                .getValidationResult(any(), eq(path(field(GdAddVcards.VCARD_ADD_ITEMS))));
    }

}
