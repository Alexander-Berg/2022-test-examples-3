package ru.yandex.market.mbo.gurulight;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Hex;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.MboLiteIntegrationTestBase;
import ru.yandex.market.mbo.common.mbi.ShopsProvider;
import ru.yandex.market.mbo.core.dashboard.GenerationsDao;
import ru.yandex.market.mbo.core.saas.SaasActiveServiceRouter;
import ru.yandex.market.mbo.core.saas.SaasClient;
import ru.yandex.market.mbo.db.TovarTreeForVisualService;
import ru.yandex.market.mbo.gurulight.saas.GenerationDataIndexOffer;
import ru.yandex.market.mbo.gurulight.saas.GenerationDataSaasDocumentUtils;
import ru.yandex.market.mbo.http.OfferStorageService;
import ru.yandex.market.saas.indexer.SaasIndexerService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore
@SuppressWarnings("checkstyle:magicnumber")
public class MbologsServiceTest extends MboLiteIntegrationTestBase {
    private MbologsService mbologsService;
    private SaasActiveServiceRouter saas;

    @Resource(name = "primarySaasClient")
    private SaasClient saasClient;

    private int prefix;
    private List<String> offerIds = new ArrayList<>();

    /**
     * Пока он не нужен в рамках mbo-lite, создаётся тут руками, чтобы не тянуть свойства в датасорсы.
     */
    private SaasIndexerService indexerService;

    @Value("${mbo.saas.indexer.host}")
    private String indexerHost;

    @Value("${mbo.saas.indexer.port}")
    private int indexerPort;

    @Value("${mbo.saas.indexer.service}")
    private String indexerServiceName;

    @Resource
    private TovarTreeForVisualService tovarTreeForVisualService;

    @Resource
    private NamedParameterJdbcTemplate glStatClickhouseJdbcTemplate;

    @Resource
    private NamedParameterJdbcTemplate namedContentJdbcTemplate;

    @Resource
    private OfferStorageService offerStorageService;

    @Resource
    private ShopsProvider shopsProvider;

    @Resource
    private GenerationsDao generationsDao;

    @Resource
    private MbologsOffersWithHypothesesProvider mbologsOffersWithHypothesesProvider;


    @Before
    public void setup() {
        // Тут именно random, чтобы разные тесты работали с разными префиксами.
        prefix = ThreadLocalRandom.current().nextInt(100_000, 1_000_000);
        saas = Mockito.mock(SaasActiveServiceRouter.class);
        Mockito.when(saas.getActiveClient()).thenReturn(saasClient);
        Mockito.when(saas.getActivePrefix()).thenReturn(prefix);

        mbologsService = createService(saas, mbologsOffersWithHypothesesProvider);
        indexerService = new SaasIndexerService(indexerHost, indexerServiceName, indexerPort, true,  1);
    }

    @After
    public void shutdown() {
        if (prefix > 0) {
            offerIds.forEach(it -> indexerService.delete(it, false, prefix));
            offerIds.clear();
        }
    }

    private MbologsSearchFilter filter() {
        return new MbologsSearchFilter();
    }

    @Test
    public void testQueries() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        List<GenerationDataIndexOffer> offers = mapper.readValue(
            getClass().getResourceAsStream("/mbo-lite/saas/offers.json"),
            new TypeReference<List<GenerationDataIndexOffer>>() {
            });

        for (GenerationDataIndexOffer offer : offers) {
            offerIds.add(offer.getOffer());
            indexerService.modify(GenerationDataSaasDocumentUtils.generateSaasDocument(offer), true, prefix);
        }

        int offersCount = mbologsService.getOffersCount(filter());
        List<OfferData> datas = mbologsService.getOfferDatas(0, filter());
        assertEquals(6, offersCount);
        assertEquals(6, datas.size());

        offersCount = mbologsService.getOffersCount(filter().setHid(91498L));
        datas = mbologsService.getOfferDatas(0, filter().setHid(91498L));
        assertEquals(2, offersCount);
        assertEquals(2, datas.size());

        // Combined filter
        offersCount = mbologsService.getOffersCount(filter().setHid(91498L).setShopId(367499L));
        datas = mbologsService.getOfferDatas(0, filter().setHid(91498L).setShopId(367499L));
        assertEquals(1, offersCount);
        assertEquals(1, datas.size());

        // Text search and wrong combination
        offersCount = mbologsService.getOffersCount(filter().setHid(91498L)
            .setShopId(367499L).setTextPart("MobilStyle"));
        datas = mbologsService.getOfferDatas(0, filter().setHid(91498L)
            .setShopId(367499L).setTextPart("MobilStyle"));
        assertEquals(0, offersCount);
        assertEquals(0, datas.size());

        // Text search (shopId is required for old search logic)
        offersCount = mbologsService.getOffersCount(filter().setShopId(317377L).setTextPart("MobilStyle"));
        datas = mbologsService.getOfferDatas(0, filter().setShopId(317377L).setTextPart("MobilStyle"));
        assertEquals(1, offersCount);
        assertEquals(1, datas.size());

        // Vendor
        offersCount = mbologsService.getOffersCount(filter().setVendorId(13518830L));
        datas = mbologsService.getOfferDatas(0, filter().setVendorId(13518830L));
        assertEquals(1, offersCount);
        assertEquals(1, datas.size());

        // Param search
        offersCount = mbologsService.getOffersCount(filter().setParamId(7893318L));
        datas = mbologsService.getOfferDatas(0, filter().setParamId(7893318L));
        assertEquals(3, offersCount);
        assertEquals(3, datas.size());

        // Param & value search
        offersCount = mbologsService.getOffersCount(filter().setParamId(7893318L).setValueId(152786L));
        datas = mbologsService.getOfferDatas(0, filter().setParamId(7893318L).setValueId(152786L));
        assertEquals(1, offersCount);
        assertEquals(1, datas.size());

        // Not formalized search, i.e. no params
        offersCount = mbologsService.getOffersCount(filter().setFormalized(false));
        datas = mbologsService.getOfferDatas(0, filter().setFormalized(false));
        assertEquals(2, offersCount);
        assertEquals(2, datas.size());

        // Test get by id
        List<OfferData> offersByIds = mbologsService.getOffersByIds(
            Arrays.asList("004937d8804b19214638e74e9fb0b4b2", "004924cb8ed9bd7b166d7e9768546cdf", "bad-id"));
        assertEquals(2, offersByIds.size());
        assertTrue(offersByIds.stream().allMatch(o -> o.shopId == 266244 || o.shopId == 367499));
    }

    @Test
    public void testGetForTask() throws IOException {
        // А теперь нам нужно "много" офферов
        ObjectMapper mapper = new ObjectMapper();
        List<GenerationDataIndexOffer> offers = mapper.readValue(
            getClass().getResourceAsStream("/mbo-lite/saas/offers.json"),
            new TypeReference<List<GenerationDataIndexOffer>>() {
            });

        Random random = new Random(42L);
        Iterator<GenerationDataIndexOffer> iter = null;
        // Отберём немного гипотез, чтобы проверить это тоже
        Map<Long, List<String>> hypothesis = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            if (iter == null || !iter.hasNext()) {
                iter = offers.iterator();
            }

            GenerationDataIndexOffer offer = iter.next();
            byte[] key = new byte[16];
            random.nextBytes(key);
            offer.setOfferId(new String(Hex.encodeHex(key)));

            hypothesis.computeIfAbsent(offer.getCategoryId(), c -> new ArrayList<>()).add(offer.getOfferId());
            offerIds.add(offer.getOfferId());
            indexerService.modify(GenerationDataSaasDocumentUtils.generateSaasDocument(offer), true, prefix);
        }

        MbologsOffersWithHypothesesProvider hypothesesProvider
            = Mockito.mock(MbologsOffersWithHypothesesProvider.class);

        Mockito.when(hypothesesProvider.getOffersWithHypotheses(Mockito.anySet(), Mockito.any(), Mockito.anyInt()))
            .then(invocation -> {
                // даём по 10 оферов из категории, чтобы потестить и эти ветки
                @SuppressWarnings("unchecked")
                Long hid = ((Set<Long>) invocation.getArgument(0)).iterator().next();
                return hypothesis.get(hid).subList(0, 10);
            });

        mbologsService = createService(saas, hypothesesProvider);

        List<OfferData> saasOffers = mbologsService.getOffersForOperatorTask(filter().setHid(91498L), 20);

        // Существующая версия не особо заботится о количестве - может вернуть и больше
        assertTrue(saasOffers.size() > 20);
        assertTrue(saasOffers.stream().allMatch(o -> o.hid == 91498L));
        assertTrue(saasOffers.stream().anyMatch(o -> o.offerId.equals(hypothesis.get(91498L).get(0))));
    }

    private MbologsService createService(SaasActiveServiceRouter saas,
                                         MbologsOffersWithHypothesesProvider hypothesesProvider) {
        return new MbologsService(tovarTreeForVisualService, glStatClickhouseJdbcTemplate,
            namedContentJdbcTemplate, shopsProvider, saas, generationsDao, hypothesesProvider,
            offerStorageService,
            () -> new Random(42L));
    }
}
