package ru.yandex.market.pers.notify.export;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import ru.yandex.market.pers.notify.comparison.ComparisonService;
import ru.yandex.market.pers.notify.comparison.model.ComparisonItem;
import ru.yandex.market.pers.notify.model.Identity;
import ru.yandex.market.pers.notify.model.YandexUid;
import ru.yandex.market.pers.notify.settings.SubscriptionAndIdentityDAO;
import ru.yandex.market.pers.notify.test.MarketMailerMockedDbTest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;

/**
 * @author Ivan Anisimov
 *         valter@yandex-team.ru
 *         12.12.16
 */
public class ComparisonExportServiceTest extends MarketMailerMockedDbTest {
    @Autowired
    private ComparisonExportService comparisonExportService;
    @Autowired
    private ComparisonService comparisonService;
    @Autowired
    private SubscriptionAndIdentityDAO subscriptionAndIdentityDAO;
    @Autowired
    private MdsExportService mdsExportService;

    @Value("${mailer.mds.local.path}")
    private String mdsLocalPath;

    private GzFileCapture mdsFile = new GzFileCapture();


    @BeforeEach
    public void setUpMdsMock() throws Exception {
        Files.createDirectories(Paths.get(mdsLocalPath));
    }

    @Test
    public void testExport() throws IOException {
        Identity identity = new YandexUid("lsdkfj983");
        long id = subscriptionAndIdentityDAO.createIfNecessaryUserIdentity(identity);
        List<ComparisonItem> items = Arrays.asList(
            new ComparisonItem(id, 12312, "dsfad12"),
            new ComparisonItem(id, 12312, "32lkas"),
            new ComparisonItem(id, 456345, "121231"),
            new ComparisonItem(id, 121223, "dsfdas"),
            new ComparisonItem(id, 54325, "43243"),
            new ComparisonItem(id, 9384765, "dsfaa")
        );
        comparisonService.saveItems(identity, items);
        comparisonExportService.exportProductIds();

        HashSet<String> expected = new HashSet<>(items.stream()
            .map(ComparisonItem::getProductId)
            .filter(ComparisonExportService.PRODUCT_IDS_FILTER)
            .collect(Collectors.toSet()));
        verify(mdsExportService).export(mdsFile.getLocalPathCapture().capture(), anyString());

        assertEquals(expected,
            new HashSet<>(Arrays.asList(mdsFile.getFileContents().split("\n"))));
    }

    public static class GzFileCapture {
        private final ArgumentCaptor<String> localPathCapture = ArgumentCaptor.forClass(String.class);

        public String getFileContents() {
            String fileName = localPathCapture.getValue();
            try (BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(fileName)), StandardCharsets.UTF_8))) {
                return in.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public ArgumentCaptor<String> getLocalPathCapture() {
            return localPathCapture;
        }
    }
}
