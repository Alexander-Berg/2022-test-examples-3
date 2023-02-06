package ru.yandex.market.crm.platform.api.http.controllers;

import java.time.Instant;
import java.util.List;

import javax.inject.Inject;

import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.crm.platform.services.config.ConfigRepository;
import ru.yandex.market.crm.platform.api.test.AbstractControllerTest;
import ru.yandex.market.crm.platform.common.FactContainer;
import ru.yandex.market.crm.platform.common.Query;
import ru.yandex.market.crm.platform.common.UidQuery;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.common.UserId;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.models.TestCartEvent;
import ru.yandex.market.crm.platform.services.facts.StorageService;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.util.Randoms;

import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.platform.api.test.config.TestConfigRepositoryConfig.CART_EVENT_FACT;

public class FactsAddControllerTest extends AbstractControllerTest {

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private YtSchemaTestUtils ytSchemaTestUtils;

    @Inject
    private StorageService storageService;

    @Before
    public void setUp() {
        configRepository.getFacts().values()
                .forEach(ytSchemaTestUtils::prepareFactTable);
    }

    @Test
    public void addFact400OnInvalidProtobufAsFact() throws Exception {
        request(
                post("/facts/{facts}", CART_EVENT_FACT)
                    .content(new byte[] {111, 101, 122})
        ).andExpect(status().isBadRequest());
    }

    @Test
    public void addFact404OnUnknownFact() throws Exception {
        postFact("unknown", createFact())
                .andExpect(status().isNotFound());
    }

    @Test
    public void addFact422OnValidationFail() throws Exception {
        postFact(
                CART_EVENT_FACT,
                TestCartEvent.getDefaultInstance()
        ).andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void addFactOk() throws Exception {
        TestCartEvent fact = createFact();

        postFact(CART_EVENT_FACT.toLowerCase(), fact)
                .andExpect(status().isNoContent());

        FactConfig config = configRepository.getFact(CART_EVENT_FACT);

        Query query = new UidQuery(true, false)
                .add(UserId.from(
                        Uids.create(UidType.YANDEXUID, fact.getUserIds().getYandexuid())
                ));

        List<FactContainer> containers = storageService.select(config, query).join();
        assertEquals(1, containers.size());
    }

    private TestCartEvent createFact() {
        UserIds.Builder uids = UserIds.newBuilder()
                .setYandexuid(Randoms.stringNumber());

        return TestCartEvent.newBuilder()
                .setUserIds(uids)
                .setTimestamp(Instant.now().toEpochMilli())
                .build();
    }

    private ResultActions postFact(String factName, Message fact) throws Exception {
        return request(post("/facts/{facts}", factName)
                .content(fact.toByteArray()));
    }
}
