package ru.yandex.market.pers.tms.rss;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ru.yandex.common.framework.filter.SimpleQueryFilter;
import ru.yandex.common.framework.user.UserInfoField;
import ru.yandex.common.framework.user.blackbox.BlackBoxService;
import ru.yandex.common.framework.user.blackbox.BlackBoxUserInfo;
import ru.yandex.common.util.StringUtils;
import ru.yandex.common.util.XPathUtils;
import ru.yandex.market.common.mds.s3.client.content.provider.FileContentProvider;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.pers.grade.client.model.Anonymity;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.core.GradeCreator;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.rss.model.FreshYtGradeData;
import ru.yandex.market.pers.tms.rss.model.RssItem;
import ru.yandex.market.pers.tms.userinfo.client.BlackboxLiteClient;
import ru.yandex.market.pers.tms.yt.dumper.dumper.YtExportHelper;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.report.ReportService;
import ru.yandex.market.report.model.Category;
import ru.yandex.market.report.model.Model;
import ru.yandex.market.report.model.ProductType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static org.apache.commons.lang3.StringUtils.join;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author vvolokh
 * 06.09.2018
 */
public class DailyGradesRssDumperTest extends MockedPersTmsTest {
    public static final long UID = 3000062912L;

    @Autowired
    @Qualifier("report")
    private ReportService reportService;

    @Autowired
    private BlackBoxService blackboxService;

    @Autowired
    BlackboxLiteClient blackboxLiteClient;

    @Value("${pers.rss.mobile.market.url}")
    private String mobileMarketUrl;

    @Value("${pers.rss.desktop.market.url}")
    private String desktopMarketUrl;

    @Autowired
    private GradeCreator gradeCreator;

    @Autowired
    private DailyGradesRssDumper rssDumper;

    @Autowired
    private MdsS3Client mockedMdsClient;

    @Autowired
    private YtExportHelper ytExportHelper;

    @Autowired
    private DbGradeService dbGradeService;

    private static final Long TEST_YESTERDAY_AND_HALF_DAY_MODEL_ID_FOR_RSS = 122L;
    private static final Long TEST_MODEL_ID_FOR_RSS = 123L;
    private static final Long TEST_MODEL_ID_FOR_RSS_2 = 124L;
    private static final Long TEST_MODEL_ID_FOR_RSS_3 = 125L;
    private static final long SBER_ID = (1L << 61) - 1L;

    private static final int RSS_COMMENT_LENGTH_THRESHOLD = DailyGradesRssDumper.REVIEW_LENGTH_FOR_BODY;
    public static final String TEXT_LONG_ENOUGH_FOR_RSS =
        StringUtils.multiply("1", RSS_COMMENT_LENGTH_THRESHOLD + 1, "");
    public static final String TEXT_TOO_SMALL_FOR_RSS = StringUtils.multiply("1", RSS_COMMENT_LENGTH_THRESHOLD, "");

    private static int createdModels = 0;

    @Test
    public void testFeedCreation() throws Exception {
        mockBlackbox(1L, "Козьма Прутков");
        mockBlackbox(2L, "Козьма Прутков");
        mockBlackbox(4L, "Козьма Прутков");

        //given:
        List<ModelGrade> modelGrades = new ArrayList<>();
        //old case, non-anonymous with big enough short_text
        modelGrades.add(createModelGradeWithDelay(TEST_MODEL_ID_FOR_RSS, 1L, TEXT_LONG_ENOUGH_FOR_RSS, "", "", Anonymity.NONE));
        //anonymous
        modelGrades.add(createModelGradeWithDelay(TEST_MODEL_ID_FOR_RSS_2, 2L, TEXT_LONG_ENOUGH_FOR_RSS, "", "", Anonymity.HIDE_NAME));
        //empty short_text, but pro+contra is big enough
        modelGrades.add(createModelGradeWithDelay(TEST_MODEL_ID_FOR_RSS_3, 4L, "",
                StringUtils.multiply("1", (RSS_COMMENT_LENGTH_THRESHOLD / 2) + 1, ""),
                StringUtils.multiply("1", (RSS_COMMENT_LENGTH_THRESHOLD / 2) + 1, ""), Anonymity.NONE));
        // СберИД
        ModelGrade grade = createModelGradeWithDelay(-7L, SBER_ID, TEXT_LONG_ENOUGH_FOR_RSS, "", "", Anonymity.NONE);
        modelGrades.add(grade);
        createIrrelevantGrades();

        initRssDumper();
        initYtClient();

        //when:
        rssDumper.generateRssFeed();

        //then:
        ArgumentCaptor<FileContentProvider> contentProviderCaptor = ArgumentCaptor.forClass(FileContentProvider.class);
        //verify mds upload was called on local temp file
        verify(mockedMdsClient).upload(any(), contentProviderCaptor.capture());

        //verify temp file is XML-parseable and contains right number of RSS items
        FileContentProvider xmlProvider = contentProviderCaptor.getValue();

        try (InputStream inputStream = xmlProvider.getInputStream()) {
            String actualXml = IOUtils.toString(inputStream, UTF_8);
            String expectedXml = IOUtils.toString(getClass().getResourceAsStream("/testdata/rss_example.xml"), UTF_8);

            for (int idx = 0; idx < modelGrades.size(); idx++) {
                ModelGrade modelGrade = modelGrades.get(idx);
                expectedXml = expectedXml
                    .replaceAll("_GRADE_ID_" + idx + "_", modelGrade.getId().toString())
                    .replaceAll("_GRADE_DT_" + idx + "_", RssItem.RFC822_FORMAT.format(modelGrade.getCreated()));
            }

            assertEquals(expectedXml, actualXml + "\n");
        }

        NodeList rssItems = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(xmlProvider.getFile())
            .getElementsByTagName("item");
        assertEquals(modelGrades.size(), rssItems.getLength());

        //verify contents of RSS items, they should be sorted by grade cr_time desc
        modelGrades.sort(Comparator.comparing(AbstractGrade::getCreated).reversed());
        for (int i = 0; i < rssItems.getLength(); i++) {
            Node singleItem = rssItems.item(i);
            verifyRssItem(modelGrades.get(i), singleItem);
        }
    }

    public DailyGradesRssDumper initRssDumper() {
        mockReportService();

        return rssDumper;
    }

    public void initYtClient() {
        Set<Long> expectedModels = Set.of(TEST_MODEL_ID_FOR_RSS, TEST_MODEL_ID_FOR_RSS_2, TEST_MODEL_ID_FOR_RSS_3, -7L);

        List<AbstractGrade> grades = dbGradeService.findGrades(new SimpleQueryFilter(), null);
        YtClient mockYtClient = ytExportHelper.getHahnYtClient();
        when(mockYtClient.read(any(), any())).thenReturn(
            grades.stream()
                .map(grade -> {
                    return new FreshYtGradeData(
                        grade.getId(),
                        grade.getAuthorUid(),
                        grade.getResourceId(),
                        grade.getText(),
                        grade.getCreated().getTime(),
                        grade.getAnonymous().value(),
                        grade.getPro(),
                        grade.getContra()
                    );
                })
                .filter(grade -> expectedModels.contains(grade.getResourceId()))
                .sorted(Comparator.comparing(FreshYtGradeData::getAuthorId))
                .collect(Collectors.toList())
        );
    }

    public void createIrrelevantGrades() {
        //grades with too short text
        createYesterdayAndHalfDayModelGrade(TEST_YESTERDAY_AND_HALF_DAY_MODEL_ID_FOR_RSS, 1L, TEXT_LONG_ENOUGH_FOR_RSS, "", "", Anonymity.NONE);
        createModelGradeWithDelay(-1L, 3L, TEXT_TOO_SMALL_FOR_RSS, "", "", Anonymity.NONE);
        createModelGradeWithDelay(-2L, 3L, "", TEXT_TOO_SMALL_FOR_RSS, "", Anonymity.NONE);
        createModelGradeWithDelay(-3L, 3L, "", "", TEXT_TOO_SMALL_FOR_RSS, Anonymity.NONE);

        //too recent grade
        createModelGrade(-5L, 3L, StringUtils.multiply("1", RSS_COMMENT_LENGTH_THRESHOLD + 1, ""), "", "",
            Anonymity.NONE, new Date());

        //from inappropriate category
        ModelGrade grade = createModelGradeWithDelay(-6L, 3L, TEXT_LONG_ENOUGH_FOR_RSS, "", "", Anonymity.NONE);

        grade = createModelGradeWithDelay(-8L, 4L, TEXT_LONG_ENOUGH_FOR_RSS, "", "", Anonymity.NONE);

        //too old grades
        createModelGrade(-4L, 3L, StringUtils.multiply("1", RSS_COMMENT_LENGTH_THRESHOLD + 1, ""), "", "",
            Anonymity.NONE, new Date(now().minus(3, ChronoUnit.DAYS).toEpochMilli()));
    }

    private void verifyRssItem(ModelGrade modelGrade, Node singleItem) {
        assertEquals("Test model name - отзыв покупателя",
            XPathUtils.queryString("title", singleItem));
        if (modelGrade.getAnonymous() == Anonymity.NONE && modelGrade.getAuthorUid() != SBER_ID) {
            assertEquals("Козьма Прутков", XPathUtils.queryString("author", singleItem));
        } else {
            assertEquals("Пользователь скрыл свои данные", XPathUtils.queryString("author", singleItem));
        }
        assertEquals(join(Arrays.asList(modelGrade.getText(), modelGrade.getPro(), modelGrade.getContra()), "\n"),
            extractTextWithNs("content:encoded", singleItem));
        assertEquals("https://avatars.mds.yandex.net/very_plausible_picture_url",
            XPathUtils.queryElement("enclosure", singleItem).getAttribute("url"));
        assertEquals(new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z").format(modelGrade.getCreated()),
            XPathUtils.queryString("pubDate", singleItem));
        assertEquals(
            desktopMarketUrl + "/product/" + modelGrade.getModelId() + "/reviews?firstReviewId=" +
                modelGrade.getId() + "&from=zen_market_reviews",
            XPathUtils.queryString("link", singleItem));
        assertEquals(mobileMarketUrl + "/product/" + modelGrade.getModelId() + "/reviews?firstReviewId=" + modelGrade.getId()
                + "&from=zen_market_reviews",
            XPathUtils.queryString("pdaLink", singleItem));
    }

    private String extractTextWithNs(String tag, Node element){
        // special case for elements with namespace prefix
        Node tagNode = ((Element) element).getElementsByTagName(tag).item(0);
        return XPathUtils.queryString(".", tagNode);
    }

    private void mockBlackbox(long uid, String publicName) {
        BlackBoxUserInfo info = new BlackBoxUserInfo(uid);
        info.addField(UserInfoField.PARAM_PUBLIC_NAME, publicName);

        when(blackboxService.getUserInfo(eq(uid), any())).thenReturn(info);
    }

    private void mockReportService() {
        Map<Long, Model> reportAnswer = new HashMap<>();
        reportAnswer.put(TEST_YESTERDAY_AND_HALF_DAY_MODEL_ID_FOR_RSS, generateModel(TEST_YESTERDAY_AND_HALF_DAY_MODEL_ID_FOR_RSS));
        reportAnswer.put(TEST_MODEL_ID_FOR_RSS, generateModel(TEST_MODEL_ID_FOR_RSS));
        reportAnswer.put(TEST_MODEL_ID_FOR_RSS_2, generateModel(TEST_MODEL_ID_FOR_RSS_2));
        reportAnswer.put(TEST_MODEL_ID_FOR_RSS_3, generateModel(TEST_MODEL_ID_FOR_RSS_3));
        LongStream.iterate(-1, operand -> operand - 1).limit(8).forEach(value -> reportAnswer.put(value, generateModel(value)));
        when(reportService.getModelsByIds(anyList())).thenReturn(reportAnswer);
    }

    private ModelGrade createYesterdayAndHalfDayModelGrade(Long modelId, Long authorId, String shortText, String pro,
                                                           String contra, Anonymity anonymity) {
        ModelGrade modelGrade = createModelGrade(modelId, authorId, shortText, pro, contra, anonymity, new Date(now().minus(1, ChronoUnit.DAYS).minus(12, ChronoUnit.HOURS).toEpochMilli()));
        modelGrade.setCreated(
            pgJdbcTemplate.queryForObject("SELECT cr_time FROM GRADE WHERE id=?", Date.class, modelGrade.getId()));
        return modelGrade;
    }

    private ModelGrade createModelGradeWithDelay(Long modelId, Long authorId, String shortText, String pro,
        String contra, Anonymity anonymity) {
        ModelGrade modelGrade = createModelGrade(modelId, authorId, shortText, pro, contra, anonymity, new Date(now().minus(5, ChronoUnit.HOURS).minus(createdModels++, ChronoUnit.MINUTES).toEpochMilli()));
        modelGrade.setCreated(
            pgJdbcTemplate.queryForObject("SELECT cr_time FROM GRADE WHERE id=?", Date.class, modelGrade.getId()));
        return modelGrade;
    }

    private ModelGrade createModelGrade(Long modelId, Long authorId, String shortText, String pro, String contra,
        Anonymity anonymity, Date date) {
        ModelGrade modelGrade = GradeCreator.constructModelGrade(modelId, authorId);
        modelGrade.setText(shortText);
        modelGrade.setPro(pro);
        modelGrade.setContra(contra);
        modelGrade.setModState(ModState.APPROVED);
        modelGrade.setAnonymous(anonymity);
        modelGrade.setCreated(date);
        modelGrade.setAverageGrade(1);
        long id = gradeCreator.createGrade(modelGrade);
        modelGrade.setId(id);
        return modelGrade;
    }

    private static Model generateModel(Long modelId) {
        Model result = new Model();
        result.setId(modelId);
        result.setName("Test model name");
        result.setType(ProductType.MODEL);
        Category category = new Category(1L, "category");
        result.setCategory(category);
        result.setPictureUrl("//avatars.mds.yandex.net/very_plausible_picture_url");
        return result;
    }
}
