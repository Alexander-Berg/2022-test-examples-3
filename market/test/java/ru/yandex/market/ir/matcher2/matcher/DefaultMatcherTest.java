package ru.yandex.market.ir.matcher2.matcher;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.ir.io.StatJsonKnowledge;
import ru.yandex.ir.io.StatShardKnowledge;
import ru.yandex.market.ir.matcher2.matcher.alternate.impl.product.Match;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.protobuf.ProtoCategoryDao;
import ru.yandex.market.ir.matcher2.matcher.alternate.load.protobuf.ProtoOfferUtils;
import ru.yandex.market.ir.matcher2.matcher.alternate.xml.XmlBooksDao;
import ru.yandex.market.http.application.CommonAppConfig;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.matcher2.matcher.be.OfferCopy;
import ru.yandex.market.ir.matcher2.matcher.deep.DeepMatcherKnowledge;
import ru.yandex.market.ir.matcher2.matcher.deep.TsvCategoryDao;
import ru.yandex.market.ir.matcher2.matcher.knowledge.MatcherKnowledge;
import ru.yandex.market.ir.matcher2.matcher.utils.FileUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author inenakhov
 */
@RunWith(MockitoJUnitRunner.StrictStubs.class)
public class DefaultMatcherTest {
    private int hid = 90602;
    private DefaultMatcher defaultMatcher;
    private StatShardKnowledge statShardKnowledge;

    @Mock
    private XmlBooksDao xmlBooksDao;

    @Mock
    private CommonAppConfig commonAppConfig;

    private Matcher.Offer exampleOffer = Matcher.Offer.newBuilder()
        .setHid(hid)
        .setTitle("Встраиваемая пароварка Smeg SF4120V")
        .setDescription("|Описание: Встраиваемая пароварка Smeg SF4120V изготовлена из серебристого стекла" +
                            " Stopsol и нержавеющей стали. Имеет большой LCD-дисплей. Поворотные переключатели " +
                            "из поликарбоната с подсветкой. Функции очистки: паровая очистка, программа " +
                            "декальцинирования. Дополнительные функции: залив воды, слив воды, " +
                            "размораживание по времени, размораживание по весу, поднятие теста," +
                            "ECO light. Электронный контроль температуры (30-100С).")
        .build();

    @Before
    public void setUp() throws Exception {
        defaultMatcher = new DefaultMatcher();
        statShardKnowledge = new StatShardKnowledge(false,
                StatShardKnowledge.DEFAULT_SHARD_ID, shardId -> {
                    throw new UnsupportedOperationException();
                });

        StatJsonKnowledge deepMatcherStatJsonKnowledge = new StatJsonKnowledge();
        deepMatcherStatJsonKnowledge.setDataGetterDir(FileUtil.getAbsolutePath("/deep"));
        deepMatcherStatJsonKnowledge.setStatShardKnowledge(statShardKnowledge);
        TsvCategoryDao dao = new TsvCategoryDao();
        dao.setDirPath(FileUtil.getAbsolutePath("/deep"));
        dao.setStatShardKnowledge(statShardKnowledge);

        StatJsonKnowledge statJsonKnowledge = new StatJsonKnowledge();
        statJsonKnowledge.setDataGetterDir(FileUtil.getAbsolutePath("/proto_dump"));
        statJsonKnowledge.setStatShardKnowledge(statShardKnowledge);

        ProtoCategoryDao categoryDao = new ProtoCategoryDao();
        categoryDao.setStatShardKnowledge(statShardKnowledge);
        categoryDao.setBooksSupported(false);
        categoryDao.setDirPath(FileUtil.getAbsolutePath("/proto_dump"));
        categoryDao.setMboHttpExporterUrl("");
        categoryDao.setDownloadDirPath("");
        categoryDao.setStatJsonKnowledge(statJsonKnowledge);

        DeepMatcherKnowledge deepMatcherKnowledge = new DeepMatcherKnowledge();
        deepMatcherKnowledge.setDao(dao);
        deepMatcherKnowledge.setStatJsonKnowledge(deepMatcherStatJsonKnowledge);
        deepMatcherKnowledge.setNumberOfInitialLoadingThreads(1);
        deepMatcherKnowledge.setNumberOfUpdateThreads(1);
        deepMatcherKnowledge.setExcludedCategories("");
        deepMatcherKnowledge.setProtoCategoryDao(categoryDao);
        deepMatcherKnowledge.afterPropertiesSet();

        MatcherKnowledge matcherKnowledge = new MatcherKnowledge();
        matcherKnowledge.setKnowledgeLoadThreadPoolSize(1);
        matcherKnowledge.setAutoReloadPeriod(0);
        matcherKnowledge.setDao(categoryDao);
        matcherKnowledge.setBooksDao(xmlBooksDao);
        matcherKnowledge.setStatJsonKnowledge(statJsonKnowledge);
        matcherKnowledge.setDeepMatcherKnowledge(deepMatcherKnowledge);
        matcherKnowledge.setCommonAppConfig(commonAppConfig);
        matcherKnowledge.setDeepMatcherEnabled(true);
        matcherKnowledge.setDumpSessionIdFile(FileUtil.getAbsolutePath("/last_dump_session_id"));

        when(commonAppConfig.getCpuCount()).thenReturn(1);

        matcherKnowledge.afterPropertiesSet();
        matcherKnowledge.refreshCategoriesAndDeepMatcher(true, false);

        defaultMatcher.setKnowledge(matcherKnowledge);
    }

    @Test
    public void alias() {
        OfferCopy offerCopy = ProtoOfferUtils.buildOfferCopy(exampleOffer, 0, hid);
        Match match = defaultMatcher.match(offerCopy);
        assertEquals(MatchType.MODEL_OK_MATCH, match.getMatchedType());
        assertEquals("SF4120V", match.getHierarchy()[1].getName());
        assertEquals("smeg", match.getHierarchy()[0].getName());
    }

    @Test
    public void multiMatchNoDeepMatch() {
        Matcher.Offer offerInIndex = Matcher.Offer.newBuilder(exampleOffer)
            .setGoodId("6d76c10ba8c40201591a78f16396b000")
            .build();
        OfferCopy offerCopy = ProtoOfferUtils.buildOfferCopy(offerInIndex, 0, hid);
        List<Match> matches = defaultMatcher.multiMatch(offerCopy);
        Match match = matches.get(0);
        assertEquals(MatchType.MODEL_OK_MATCH, match.getMatchedType());
        assertEquals("SF4120V", match.getHierarchy()[1].getName());
        assertEquals("smeg", match.getHierarchy()[0].getName());
    }

    @Test
    public void multiMatchDeepMatchOnly() {
        Matcher.Offer offerInIndex = Matcher.Offer.newBuilder()
            .setHid(hid)
            .setGoodId("6d76c10ba8c40201591a78f16396b000")
            .build();
        OfferCopy offerCopy = ProtoOfferUtils.buildOfferCopy(offerInIndex, 0, hid);
        List<Match> matches = defaultMatcher.multiMatch(offerCopy);
        Match match = matches.get(0);
        assertEquals(MatchType.DEEP_MATCH, match.getMatchedType());
        assertTrue(match.getDeepMatchInfo().isDeepMatch());
    }

    @Test
    public void multiMatchDeepMatchOverVendorMatch() {
        Matcher.Offer offer = Matcher.Offer.newBuilder()
            .setHid(hid)
            .setGoodId("6d76c10ba8c40201591a78f16396b000")
            .setTitle("Встраиваемая пароварка Smeg")
            .setDescription("")
            .build();
        OfferCopy offerCopy = ProtoOfferUtils.buildOfferCopy(offer, 0, hid);
        List<Match> matches = defaultMatcher.multiMatch(offerCopy);
        assertEquals(2, matches.size());
        Match match = matches.get(0);
        assertEquals(MatchType.DEEP_MATCH, match.getMatchedType());
        assertTrue(match.getDeepMatchInfo().isDeepMatch());
        Match vendorMatch = matches.get(1);
        assertEquals(MatchType.VENDOR_MATCH, vendorMatch.getMatchedType());
    }

    @Test
    public void multiMatchVendorMatchNoDeepMatch() {
        Matcher.Offer offer = Matcher.Offer.newBuilder()
            .setHid(hid)
            .setGoodId("6d76c10ba8c40201591a78f16396b")
            .setTitle("Встраиваемая пароварка Smeg")
            .setDescription("")
            .build();

        OfferCopy offerCopy = ProtoOfferUtils.buildOfferCopy(offer, 0, hid);
        List<Match> matches = defaultMatcher.multiMatch(offerCopy);
        assertEquals(1, matches.size());
        Match match = matches.get(0);
        assertEquals(MatchType.VENDOR_MATCH, match.getMatchedType());
    }

    @Test
    public void deep() {
        Matcher.Offer offerInIndex = Matcher.Offer.newBuilder()
            .setHid(hid)
            .setGoodId("6d76c10ba8c40201591a78f16396b000")
            .build();
        OfferCopy offerInIndexCopy = ProtoOfferUtils.buildOfferCopy(offerInIndex, 0, hid);

        Match firstMatch = defaultMatcher.match(offerInIndexCopy);
        assertEquals(MatchType.DEEP_MATCH, firstMatch.getMatchedType());
        assertEquals(13991100, firstMatch.getDeepMatchInfo().getDeepMatchedId());
        assertTrue(firstMatch.getDeepMatchInfo().getDeepMatchConfidence() > 0);
        assertTrue(firstMatch.getDeepMatchInfo().isDeepMatch());

        Matcher.Offer offerNotInIndex = Matcher.Offer.newBuilder()
            .setHid(hid)
            .setGoodId("6d76c10ba8c40201591a78f16000000")
            .build();
        OfferCopy offerNotInIndexCopy = ProtoOfferUtils.buildOfferCopy(offerNotInIndex, 0, hid);

        Match secondMatch = defaultMatcher.match(offerNotInIndexCopy);
        assertEquals(MatchType.NO_MATCH, secondMatch.getMatchedType());
        if (secondMatch.getDeepMatchInfo() == null) {
            assertTrue(true);
        } else {
            assertTrue(secondMatch.getDeepMatchInfo().getDeepMatchConfidence() == 0);
            assertTrue(secondMatch.getDeepMatchInfo().getDeepMatchTrashScore() == 0);
            assertTrue(secondMatch.getDeepMatchInfo().getTargetType() == Matcher.SMTargetType.PSKU);
            assertFalse(secondMatch.getDeepMatchInfo().isDeepMatch());
        }

        Matcher.Offer offerWithDeepMatchProblem = Matcher.Offer.newBuilder()
            .setHid(hid)
            .setGoodId("6d76c10ba8c40201591a78f16396b111")
            .build();

        OfferCopy offerWithDeepMatchProblemCopy =
            ProtoOfferUtils.buildOfferCopy(offerWithDeepMatchProblem, 0, hid);
        Match thirdMatch = defaultMatcher.match(offerWithDeepMatchProblemCopy);
        assertEquals(MatchType.NO_MATCH, thirdMatch.getMatchedType());
        assertTrue(thirdMatch.getDeepMatchInfo() != null);
        assertTrue(thirdMatch.getDeepMatchInfo().getDeepMatchConfidence() == 0.5228402878113174);
        assertFalse(thirdMatch.getDeepMatchInfo().isDeepMatch());
        assertEquals(1, thirdMatch.getDeepMatchInfo().getDeepMatchProblems().size());
        assertEquals(thirdMatch.getDeepMatchInfo().getDeepMatchProblems().get(0).getProblemType(),
                     Matcher.DeepMatchProblem.DeepMatchProblemType.NO_SUCH_MODEL_IN_CATEGORY);
    }

    @Test
    public void manualMatching() {
        String title = exampleOffer.getTitle();
        String description = exampleOffer.getDescription();
        String goodId = "a7bc55fedf967ae7f696046b064c1fd3";
        int matchTarget = 13342762;

        Matcher.Offer goodOffer = Matcher.Offer.newBuilder()
            .setGoodId(goodId)
            .setTitle(title)
            .setDescription(description)
            .setModelId(matchTarget)
            .build();

        Matcher.Offer badOfferNoGoodId = Matcher.Offer.newBuilder()
            .setTitle(title)
            .setDescription(description)
            .setModelId(matchTarget)
            .build();

        Matcher.Offer badOfferUnknownMatchTarget = Matcher.Offer.newBuilder()
            .setGoodId(goodId)
            .setTitle(title)
            .setDescription(description)
            .setModelId(-matchTarget)
            .build();

        Match firstMatch = defaultMatcher.match(ProtoOfferUtils.buildOfferCopy(goodOffer, 0, hid));
        assertEquals(MatchType.GOOD_ID_MATCH, firstMatch.getMatchedType());
        assertEquals(Matcher.MatchTarget.PUBLISHED_MODEL, firstMatch.getMatchTarget());
        assertEquals(matchTarget, firstMatch.getHierarchy()[1].getMatchedId());
        assertEquals("SF4120V", firstMatch.getHierarchy()[1].getName());
        assertEquals("smeg", firstMatch.getHierarchy()[0].getName());

        Match secondMatch = defaultMatcher.match(ProtoOfferUtils.buildOfferCopy(badOfferNoGoodId, 0, hid));
        assertEquals(MatchType.MODEL_OK_MATCH, secondMatch.getMatchedType());

        Match thirdMatch = defaultMatcher.match(ProtoOfferUtils.buildOfferCopy(badOfferUnknownMatchTarget, 0, hid));
        assertEquals(MatchType.NO_MATCH, thirdMatch.getMatchedType());
        assertEquals(Matcher.MatchTarget.NOTHING, thirdMatch.getMatchTarget());

        Match fourthMatch = defaultMatcher.match(ProtoOfferUtils.buildOfferCopy(goodOffer, 0, 0));
        assertEquals(MatchType.GOOD_ID_MATCH, fourthMatch.getMatchedType());
        assertEquals(hid, fourthMatch.getHid());
        assertEquals(Match.MatchMethod.MODEL_ID, fourthMatch.getMatchMethod());

        Match fifthMatch = defaultMatcher.match(ProtoOfferUtils.buildOfferCopy(badOfferUnknownMatchTarget, 0, 0));
        assertEquals(MatchType.NO_MATCH, fifthMatch.getMatchedType());
        assertEquals(Matcher.MatchTarget.NOTHING, fifthMatch.getMatchTarget());
    }
}
