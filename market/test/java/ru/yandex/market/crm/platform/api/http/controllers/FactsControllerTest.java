package ru.yandex.market.crm.platform.api.http.controllers;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.crm.platform.services.config.ConfigRepository;
import ru.yandex.market.crm.platform.api.GetFactsRequest;
import ru.yandex.market.crm.platform.api.GetGenericFactsRequest;
import ru.yandex.market.crm.platform.api.Period;
import ru.yandex.market.crm.platform.api.test.AbstractControllerTest;
import ru.yandex.market.crm.platform.api.test.kv.MappingTableSetter;
import ru.yandex.market.crm.platform.api.test.kv.YandexIds;
import ru.yandex.market.crm.platform.common.Uids;
import ru.yandex.market.crm.platform.commons.Response;
import ru.yandex.market.crm.platform.commons.UidType;
import ru.yandex.market.crm.platform.commons.UserIds;
import ru.yandex.market.crm.platform.config.FactConfig;
import ru.yandex.market.crm.platform.config.InboxSourceConfig;
import ru.yandex.market.crm.platform.config.Model;
import ru.yandex.market.crm.platform.config.StorageConfig;
import ru.yandex.market.crm.platform.config.raw.StorageType;
import ru.yandex.market.crm.platform.models.MinimalExample;
import ru.yandex.market.crm.platform.models.NoTimeExample;
import ru.yandex.market.crm.platform.models.OrderDvk;
import ru.yandex.market.crm.platform.profiles.Facts;
import ru.yandex.market.crm.platform.test.utils.YtSchemaTestUtils;
import ru.yandex.market.crm.platform.test.yt.FactsSaver;
import ru.yandex.market.mcrm.http.HttpRequest;
import ru.yandex.market.mcrm.http.ResponseBuilder;
import ru.yandex.market.mcrm.http.Service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.crm.platform.api.test.config.TestConfigRepositoryConfig.MAXIMAL_EXAMPLE;
import static ru.yandex.market.crm.platform.api.test.config.TestConfigRepositoryConfig.MINIMAL_EXAMPLE;
import static ru.yandex.market.crm.platform.api.test.config.TestConfigRepositoryConfig.NO_TIME_EXAMPLE;
import static ru.yandex.market.crm.platform.api.test.config.TestConfigRepositoryConfig.ORDER_DVK;

public class FactsControllerTest extends AbstractControllerTest {

    @Inject
    private MappingTableSetter mappingTableSetter;

    @Inject
    private FactsSaver factsSaver;

    @Inject
    private ConfigRepository configRepository;

    @Inject
    private YtSchemaTestUtils ytSchemaTestUtils;

    private FactConfig minimalExampleConfig;
    private FactConfig maximalExampleConfig;

    private static StorageConfig store() {
        return new StorageConfig(null, StorageType.HDD);
    }

    @Before
    public void setUp() {
        httpEnvironment.when(
                HttpRequest.get(Service.CRYPTA_API, "/ext/lilucrm/graph")
                        .param("yandexuid", "111")
        ).then(
                ResponseBuilder.newBuilder()
                        .code(404)
                        .body("Graph not found")
                        .build()
        );
        minimalExampleConfig = configRepository.getFact(MINIMAL_EXAMPLE);
        maximalExampleConfig = configRepository.getFact(MAXIMAL_EXAMPLE);

        configRepository.getFacts().values()
                .forEach(ytSchemaTestUtils::prepareFactTable);

        mappingTableSetter.addSberToYandex("iddqd", new YandexIds().addYandexuid("111"));
        mappingTableSetter.addYandexToSber(UidType.YANDEXUID, "111", Collections.singleton("iddqd"));
    }

    @Test
    public void testGetSingleFactBySberId() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("sber_id", "iddqd")
                        .param("facts", MINIMAL_EXAMPLE),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());

        assertEquals(0, facts.getMaximalExampleCount());
    }

    @Test
    public void testGetMultipleFactsBySberId() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build(),
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(2)
                        .build()
        );

        factsSaver.save(
                maximalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(3)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("sber_id", "iddqd")
                        .param("facts", MINIMAL_EXAMPLE, MAXIMAL_EXAMPLE),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(2, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
        assertEquals(2, facts.getMinimalExample(1).getModelId());

        assertEquals(1, facts.getMaximalExampleCount());
        assertEquals(3, facts.getMaximalExample(0).getModelId());
    }

    @Test
    public void test400IfFactIsNotInGeneralProfile() throws Exception {
        String factId = "SomeFact";

        try {
            configRepository.getFacts()
                    .put(
                            factId,
                            new FactConfig(
                                    factId,
                                    factId,
                                    Collections.singletonList(InboxSourceConfig.INSTANCE),
                                    new Model(MinimalExample.parser(), MinimalExample.getDescriptor()),
                                    null,
                                    null,
                                    List.of(),
                                    Map.of("hahn", store())
                            )
                    );

            request(
                    get("/facts/weak")
                            .param("sber_id", "iddqd")
                            .param("facts", MINIMAL_EXAMPLE, factId)
            ).andExpect(status().isBadRequest());
        } finally {
            configRepository.getFacts().remove(factId);
        }
    }

    @Test
    public void testReturnAllFactsIfNoFactsAreSpecified() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build(),
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(2)
                        .build()
        );

        factsSaver.save(
                maximalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(3)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("sber_id", "iddqd"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(2, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
        assertEquals(2, facts.getMinimalExample(1).getModelId());

        assertEquals(1, facts.getMaximalExampleCount());
        assertEquals(3, facts.getMaximalExample(0).getModelId());
    }

    @Test
    public void testGetFactsForCertainTime() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(946684801100L)
                        .setModelId(1)
                        .build(),
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(946684801150L)
                        .setModelId(2)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("sber_id", "iddqd")
                        .param("ts_min", "946684801000")
                        .param("ts_max", "946684801200"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(2, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
        assertEquals(2, facts.getMinimalExample(1).getModelId());
    }

    @Test
    public void testGetFactsSinceTime() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build(),
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(2)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("sber_id", "iddqd")
                        .param("ts_min", "100000"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(2, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
        assertEquals(2, facts.getMinimalExample(1).getModelId());
    }

    @Test
    public void testGetAllNoTimeBasedFactsEvenIfTsParamIsSpecified() throws Exception {
        factsSaver.save(
                configRepository.getFact(NO_TIME_EXAMPLE),
                NoTimeExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        ).setIsActive(true)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("sber_id", "iddqd")
                        .param("ts_min", "100000"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getNoTimeExampleCount());
        assertTrue(facts.getNoTimeExample(0).getIsActive());
    }

    @Test
    public void testGetFactLinkedWithWeakEdge() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("333")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build()
        );

        httpEnvironment.when(
                HttpRequest.get(Service.CRYPTA_API, "/ext/lilucrm/graph")
                        .param("yandexuid", "111")
        ).then(
                ResponseBuilder.newBuilder()
                        .body(getClass().getResourceAsStream("graph-with-weak-edges.json"))
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("yandexuid", "111"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testGetFactForGivenIdEvenIfNoOtherIdsIsLinkedWeak() throws Exception {
        factsSaver.save(
            minimalExampleConfig,
            MinimalExample.newBuilder()
                    .setUserIds(
                            UserIds.newBuilder()
                                    .setYandexuid("111")
                    )
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setModelId(1)
                    .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("yandexuid", "111"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testGetFactsForIdsWithStrongConnectionsOnly() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setPuid(222)
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build()
        );

        httpEnvironment.when(
                HttpRequest.get(Service.CRYPTA_API, "/ext/lilucrm/graph")
                        .param("yandexuid", "111")
        ).then(
                ResponseBuilder.newBuilder()
                        .body(getClass().getResourceAsStream("graph-with-weak-edges.json"))
                        .build()
        );

        Response response = request(
                get("/facts/strong")
                        .param("yandexuid", "111"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testNoFactsIfStrongConnectionsRequestedForSberId() throws Exception {
        Response response = request(
                get("/facts/strong")
                        .param("sber_id", "random"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(0, facts.getMinimalExampleCount());
        assertEquals(0, facts.getMaximalExampleCount());
    }

    @Test
    public void testReturnFactsEvenIfNoStrongConnectionsFound() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setYandexuid("111")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build()
        );

        httpEnvironment.when(
                HttpRequest.get(Service.CRYPTA_API, "/ext/lilucrm/graph")
                        .param("yandexuid", "111")
        ).then(
                ResponseBuilder.newBuilder()
                        .body(getClass().getResourceAsStream("graph-with-single-weak-edge.json"))
                        .build()
        );

        Response response = request(
                get("/facts/strong")
                        .param("yandexuid", "111"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testGetEmailBoundFactByAnotherId() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setEmail("apershukov@yandex-team.ru")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build()
        );

        httpEnvironment.when(
                HttpRequest.get(Service.CRYPTA_API, "/ext/lilucrm/graph")
                        .param("puid", "111")
        ).then(
                ResponseBuilder.newBuilder()
                        .body(getClass().getResourceAsStream("graph-with-email.json"))
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("puid", "111"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testGetFactByEmail() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                        .setUserIds(
                                UserIds.newBuilder()
                                        .setEmail("apershukov@yandex-team.ru")
                        )
                        .setTimestamp(Instant.now().toEpochMilli())
                        .setModelId(1)
                        .build()
        );

        Response response = request(
                get("/facts/weak")
                        .param("email", "apershukov@yandex-team.ru"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testGetSelfFacts() throws Exception {
        factsSaver.save(
                minimalExampleConfig,
                MinimalExample.newBuilder()
                    .setUserIds(
                            UserIds.newBuilder().setUuid("uuid222")
                    )
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setModelId(1)
                    .build()
        );

        factsSaver.save(
            minimalExampleConfig,
            MinimalExample.newBuilder()
                    .setUserIds(
                            UserIds.newBuilder().setYandexuid("111")
                    )
                    .setTimestamp(Instant.now().toEpochMilli())
                    .setModelId(1)
                    .build()
        );

        httpEnvironment.when(
                HttpRequest.get(Service.CRYPTA_API, "/ext/lilucrm/graph")
                        .param("uuid", "uuid222")
        ).then(
                ResponseBuilder.newBuilder()
                        .body(getClass().getResourceAsStream("graph-with-weak-edges.json"))
                        .build()
        );

        Response response = request(
                get("/facts/none")
                        .param("uuid", "uuid222"),
                Response::parseFrom
        );

        assertNotNull(response);

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getMinimalExampleCount());
        assertEquals(1, facts.getMinimalExample(0).getModelId());
    }

    @Test
    public void testGetGenericFactsWithoutUid() throws Exception {
        factsSaver.save(
            configRepository.getFact(ORDER_DVK),
            OrderDvk.newBuilder()
                    .setOrderId(234)
                    .setTimestamp(System.currentTimeMillis())
                    .setFromDate("01-01-2019")
                    .setToDate("02-01-2019")
                    .build()
        );

        GetGenericFactsRequest req = GetGenericFactsRequest.newBuilder()
                .addFacts(GetFactsRequest.Fact.newBuilder().setConfig(ORDER_DVK).setId("234"))
                .addFacts(GetFactsRequest.Fact.newBuilder().setConfig(ORDER_DVK).setId("345"))
                .build();

        Response response = request(
                post("/facts/generic")
                        .content(req.toByteArray()),
                Response::parseFrom
        );

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getOrderDvkCount());
        assertEquals("01-01-2019", facts.getOrderDvk(0).getFromDate());
    }

    @Test
    public void testGetGenericFactsWithTime() throws Exception {
        factsSaver.save(
                configRepository.getFact(ORDER_DVK),
                OrderDvk.newBuilder()
                        .setOrderId(234)
                        .setTimestamp(System.currentTimeMillis())
                        .setFromDate("01-01-2019")
                        .setToDate("02-01-2019")
                        .build()
        );

        GetGenericFactsRequest req = GetGenericFactsRequest.newBuilder()
                .addFacts(GetFactsRequest.Fact.newBuilder().setConfig(ORDER_DVK).setId("234"))
                .setTime(Period.newBuilder().setStart(1000))
                .build();

        Response response = request(
                post("/facts/generic")
                        .content(req.toByteArray()),
                Response::parseFrom
        );

        Facts facts = response.getFacts();
        assertNotNull(facts);

        assertEquals(1, facts.getOrderDvkCount());
        assertEquals(234, facts.getOrderDvk(0).getOrderId());
    }

    @Test
    public void expect400WhenNonGenericIsRequested() throws Exception {
        GetFactsRequest req = GetFactsRequest.newBuilder()
                .addUid(Uids.create(UidType.YANDEXUID, "111"))
                .addFacts(GetFactsRequest.Fact.newBuilder().setConfig(MINIMAL_EXAMPLE))
                .build();

        request(
                post("/facts/generic")
                        .content(req.toByteArray())
        ).andExpect(status().isBadRequest());
    }
}
