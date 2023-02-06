package ru.yandex.market.mbo.tms.report;

import com.google.common.base.Joiner;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.thymeleaf.spring3.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import ru.yandex.inside.yt.kosher.cypress.CypressNodeType;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeMapNode;
import ru.yandex.market.mbo.db.TovarTreeService;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategoryNode;
import ru.yandex.market.mbo.gwt.models.visual.TovarTree;
import ru.yandex.market.mbo.smtp.Message;
import ru.yandex.market.mbo.smtp.SmtpSender;
import ru.yandex.market.mbo.tms.health.YtHealthMapReduceService;
import ru.yandex.market.mbo.yt.TestYt;

import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author yuramalinov
 * @created 17.08.18
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class NewSkuReportEmailExecutorTest {
    private static final long HID_ROOT = 1000L;
    private static final long HID1 = 1001L;
    private static final long HID2 = 1002L;
    private static final long GURU1 = 2001L;
    private static final long GURU2 = 2002L;
    private static final long SKU_ID1 = 3001L;
    private static final long SKU_ID2 = 3002L;
    private static final long SKU_ID3 = 3003L;
    private JdbcTemplate yqlTemplate;
    private TovarTreeService tovarTreeService;
    private NewSkuReportEmailExecutor reportExecutor;
    private SmtpSender smtpSender;
    private YtHealthMapReduceService ytHealthMapReduceService;
    private TestYt yt;
    private String dumpFileContent;

    @SuppressWarnings("unchecked")
    @Before
    public void setup() throws Exception {
        yt = new TestYt();

        yqlTemplate = Mockito.mock(JdbcTemplate.class);
        tovarTreeService = Mockito.mock(TovarTreeService.class);
        smtpSender = Mockito.mock(SmtpSender.class);
        doAnswer(i -> {
            Message message = i.getArgument(0);
            dumpFileContent = Joiner.on("\n").join(Files.readAllLines(message.getAttachements().get(0).getSecond()));
            return null;
        }).when(smtpSender).send(any(Message.class));

        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix("reports/pages/");
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode("HTML");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver);

        ytHealthMapReduceService = Mockito.mock(YtHealthMapReduceService.class);

        reportExecutor = new NewSkuReportEmailExecutor(yt, yqlTemplate, tovarTreeService, templateEngine, smtpSender,
            ytHealthMapReduceService, "http://mbo/", "//report", "//mappings", "someone@some.where");


        TovarCategoryNode root = new TovarCategoryNode(new TovarCategory("Root", HID_ROOT, 0L));
        TovarCategory c1 = new TovarCategory("Test 1", HID1, HID_ROOT);
        c1.setGuruCategoryId(GURU1);
        root.addChild(new TovarCategoryNode(c1));

        TovarCategory c2 = new TovarCategory("Test 2", HID2, HID_ROOT);
        c2.setGuruCategoryId(GURU2);
        root.addChild(new TovarCategoryNode(c2));

        TovarTree tovarTree = new TovarTree(root);
        when(tovarTreeService.loadTovarTree()).thenReturn(tovarTree);
    }

    @Test
    public void testInitialization() throws Exception {
        when(ytHealthMapReduceService.listSessionPaths())
            .thenReturn(Stream.of(
                new YtHealthMapReduceService.YPaths("20180817_1909", YPath.simple("//health/20180817_1909"),
                    YPath.simple("//health/20180817_1909/models"), YPath.simple("//health/20180817_1909/skus"),
                    YPath.simple("//health/20180817_1909/partner_skus"))));

        yt.cypress().create(YPath.simple("//health/20180817_1909/skus"), CypressNodeType.TABLE, true, false);

        reportExecutor.doRealJob(null);

        assertTrue(yt.cypress().exists(YPath.simple("//report/reported-categories")));
        assertEquals("2018-08-17T19:09",
            yt.cypress().get(YPath.simple("//report/last-processed-table")).stringValue());
        Mockito.verify(smtpSender, Mockito.never()).send(any());
        Mockito.verify(yqlTemplate).execute(Mockito.contains("insert"));
    }

    @Test
    public void testNothingToDoThisDay() throws Exception {
        when(ytHealthMapReduceService.listSessionPaths())
            .thenReturn(Stream.of(
                new YtHealthMapReduceService.YPaths("20180817_1509", YPath.simple("//health/20180817_1509"),
                    YPath.simple("//health/20180817_1509/models"), YPath.simple("//health/20180817_1509/skus"),
                    YPath.simple("//health/20180817_1509/partner_skus")),
                new YtHealthMapReduceService.YPaths("20180817_1909", YPath.simple("//health/20180817_1909"),
                    YPath.simple("//health/20180817_1909/models"), YPath.simple("//health/20180817_1909/skus"),
                    YPath.simple("//health/20180817_1909/partner_skus"))));

        yt.cypress().create(YPath.simple("//health/20180817_1509/skus"), CypressNodeType.TABLE, true, false);
        yt.cypress().create(YPath.simple("//health/20180817_1909/skus"), CypressNodeType.TABLE, true, false);
        yt.cypress().create(YPath.simple("//report/reported-categories"), CypressNodeType.TABLE, true);
        yt.cypress().set(YPath.simple("//report/last-processed-table"), "2018-08-17T15:09");

        reportExecutor.doRealJob(null);

        // Don't change diff
        assertEquals("2018-08-17T15:09",
            yt.cypress().get(YPath.simple("//report/last-processed-table")).stringValue());
        Mockito.verify(smtpSender, Mockito.never()).send(any());
        Mockito.verifyZeroInteractions(yqlTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testItSendsReport() throws Exception {
        when(ytHealthMapReduceService.listSessionPaths())
            .thenReturn(Stream.of(
                new YtHealthMapReduceService.YPaths("20180817_1509", YPath.simple("//health/20180817_1509"),
                    YPath.simple("//health/20180817_1509/models"), YPath.simple("//health/20180817_1509/skus"),
                    YPath.simple("//health/20180817_1509/partner_skus")),
                new YtHealthMapReduceService.YPaths("20180818_1909", YPath.simple("//health/20180818_1909"),
                    YPath.simple("//health/20180818_1909/models"), YPath.simple("//health/20180818_1909/skus"),
                    YPath.simple("//health/20180818_1909/partner_skus"))));

        YPath reportCategoriesTable = YPath.simple("//report/reported-categories");
        yt.cypress().create(YPath.simple("//health/20180817_1509/skus"), CypressNodeType.TABLE, true, false);
        yt.cypress().create(YPath.simple("//health/20180818_1909/skus"), CypressNodeType.TABLE, true, false);
        yt.cypress().create(reportCategoriesTable, CypressNodeType.TABLE, true);
        yt.cypress().set(YPath.simple("//report/last-processed-table"), "2018-08-17T15:09");

        when(yqlTemplate.query(Mockito.contains("select"), (RowMapper<Object>) any()))
            .thenReturn(Arrays.asList(
                new NewSkuReportEmailExecutor.SkuAndHid(SKU_ID1, HID1),
                new NewSkuReportEmailExecutor.SkuAndHid(SKU_ID2, HID1),
                new NewSkuReportEmailExecutor.SkuAndHid(SKU_ID3, HID2)
            ));

        reportExecutor.doRealJob(null);

        assertEquals("2018-08-18T19:09",
            yt.cypress().get(YPath.simple("//report/last-processed-table")).stringValue());
        Mockito.verify(yqlTemplate, Mockito.never()).execute(Mockito.anyString());
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(smtpSender).send(captor.capture());

        Message message = captor.getValue();
        Assertions.assertThat(message.getAttachements()).hasSize(1);
        Assertions.assertThat(dumpFileContent).contains("" + SKU_ID1, "Test");

        List<YTreeMapNode> values = Lists.newArrayList(yt.tables().read(reportCategoriesTable, YTableEntryTypes.YSON));
        Assertions.assertThat(values)
            .extracting(node -> node.getLong("hid"))
            .containsExactlyInAnyOrder(HID1, HID2);
    }
}
