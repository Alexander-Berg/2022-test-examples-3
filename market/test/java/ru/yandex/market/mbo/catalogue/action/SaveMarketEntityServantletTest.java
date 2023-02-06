package ru.yandex.market.mbo.catalogue.action;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.common.framework.core.AbstractServRequest;
import ru.yandex.common.framework.core.MockServResponse;
import ru.yandex.common.framework.core.ServRequest;
import ru.yandex.common.framework.core.ServResponse;
import ru.yandex.market.mbo.catalogue.MarketDepotService;
import ru.yandex.market.mbo.catalogue.action.savemarketentity.SaveMarketEntityServantlet;
import ru.yandex.market.mbo.core.kdepot.api.KnownEntityTypes;
import ru.yandex.market.mbo.core.ui.util.XmlAdapterImpl;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.utils.web.MarketServRequest;
import ru.yandex.market.mbo.utils.web.MarketServResponse;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.when;

/**
 * Created by annaalkh on 11.04.17.
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
@SuppressWarnings("checkstyle:MagicNumber")
public class SaveMarketEntityServantletTest {

    private static final long GURU_ID = 2;
    private static final long HYPER_ID = 3;
    private static final long USER_ID = 4;

    @Mock
    private MarketDepotService marketDepotServiceMock;

    @Mock
    private GuruService guruServiceMock;

    @Mock
    private TovarTreeService tovarTreeServiceMock;

    private SaveMarketEntityServantlet saveMarketEntityServantlet;

    @Before
    public void setUp() throws Exception {

        when(marketDepotServiceMock.createEntity(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(), any(), any()))
                .thenAnswer(i -> {
                    Consumer<Long> createdCategoryHandler = i.getArgument(6);
                    createdCategoryHandler.accept(GURU_ID);
                    return GURU_ID;
                });

        saveMarketEntityServantlet = new SaveMarketEntityServantlet(
            marketDepotServiceMock,
            guruServiceMock,
            null,
            null,
            null,
            tovarTreeServiceMock,
            null,
            null
        );
    }

    @Test
    public void linkGuruAndTovarCategoryByHid() {
        ServRequest rq = new AbstractServRequest(null, null, null) { };
        rq.setParam("entity-type-id", String.valueOf(KnownEntityTypes.MARKET_CATEGORY));
        rq.setParam("hyper-id", String.valueOf(HYPER_ID));

        ReflectionTestUtils.setField(rq, "userId", USER_ID);

        ServResponse resp = new MockServResponse();

        ArgumentCaptor<Long> tovarCategoryCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<Map> categoryCreationAttrCaptor = ArgumentCaptor.forClass(Map.class);

        saveMarketEntityServantlet.doProcess(new MarketServRequest<>(rq),
            new MarketServResponse(new XmlAdapterImpl(), resp));

        Mockito.verify(tovarTreeServiceMock)
                .setGuruCategoryId(anyLong(), tovarCategoryCaptor.capture(), anyLong());
        Mockito.verify(marketDepotServiceMock)
                .createEntity(anyLong(), anyLong(), anyLong(), anyLong(), anyLong(),
                    categoryCreationAttrCaptor.capture(), any());

        Assert.assertEquals(tovarCategoryCaptor.getValue().longValue(), GURU_ID);

        List<String> tovarCategoryIds = (List<String>) categoryCreationAttrCaptor.getValue().get("hyper-id");
        Assert.assertEquals(tovarCategoryIds.size(), 1);
        Assert.assertEquals(tovarCategoryIds.get(0), String.valueOf(HYPER_ID));

    }
}
