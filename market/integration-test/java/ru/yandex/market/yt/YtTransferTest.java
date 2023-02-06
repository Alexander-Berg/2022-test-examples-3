package ru.yandex.market.yt;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.inside.yt.kosher.tables.YTableEntryTypes;
import ru.yandex.inside.yt.kosher.ytree.YTreeStringNode;
import ru.yandex.market.ir.classifier.model.SCOfferTypeFieldNames;
import ru.yandex.market.util.session.SessionInfo;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Aleksandr Kormushin &lt;kormushin@yandex-team.ru&gt;
 */
@Ignore("For manual use. Uses YT. For debug purposes only")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:YtTransferTest.xml"})
public class YtTransferTest {

    public static final String CHECK = "check";
    public static final String[] SESSION_TYPES = {"approved", "specialApproved", "assured", "checked", "unchecked"};

    @Rule
    public final TemporaryFolder testFolder = new TemporaryFolder();

    private final SessionInfo sessionInfo = new SessionInfo("session_id", "type");

    @Inject
    private YtTransfer ytTransfer;

    @Inject
    private Yt ytApi;

    @Value("${classifier.yt.offersdata.home.path}")
    private String ytOffersDataPath;

    @Before
    public void setUp() {
        ytTransfer.setLocalPathTemplate(testFolder.getRoot().getAbsolutePath() + "/$ID");
    }

    @Test
    public void uploadDownloadFile() throws IOException {
        File createdFile = testFolder.newFile("test.json");
        Files.write(createdFile.toPath(), CHECK.getBytes());

        ytTransfer.uploadFile(sessionInfo, createdFile.getAbsolutePath());
        String downloadedFile = ytTransfer.downloadFile(sessionInfo);

        assertThat(Files.exists(Paths.get(downloadedFile)), is(true));
        assertThat(new String(Files.readAllBytes(Paths.get(downloadedFile))), is(CHECK));
    }

    @Test
    public void downloadTables() throws IOException {
        List<YPath> tables =
            Stream.of(SESSION_TYPES)
                .map(s ->
                    YPath.simple(ytOffersDataPath).child(s).child(
                        ytApi.cypress().list(YPath.simple(ytOffersDataPath).child(s)).stream()
                            .map(YTreeStringNode::getValue)
                            .sorted(Comparator.reverseOrder())
                            .findFirst()
                            .get()))
                .collect(Collectors.toList());

        String localPath = ytTransfer.downloadTables(
            tables,
            new String[]{SCOfferTypeFieldNames.OFFER_ID, SCOfferTypeFieldNames.TITLE, SCOfferTypeFieldNames.CATEGORY_ID},
            sessionInfo,
            YTableEntryTypes.YSON,
            o -> o.getString(SCOfferTypeFieldNames.OFFER_ID) + "\t" +
                o.getString(SCOfferTypeFieldNames.TITLE) + "\t" +
                o.getLong(SCOfferTypeFieldNames.CATEGORY_ID) + "\n");

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(localPath))) {
            System.out.println(reader.readLine());
        }
    }
}
