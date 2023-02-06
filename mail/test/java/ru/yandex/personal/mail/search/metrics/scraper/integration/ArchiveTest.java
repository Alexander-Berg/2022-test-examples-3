package ru.yandex.personal.mail.search.metrics.scraper.integration;

import java.io.File;

import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.FileSystemUtils;

import ru.yandex.personal.mail.search.metrics.scraper.controllers.ArchiveController;
import ru.yandex.personal.mail.search.metrics.scraper.services.archive.response.html.HtmlResponseRepository;
import ru.yandex.personal.mail.search.metrics.scraper.services.archive.response.json.JsonResponseRepository;
import ru.yandex.personal.mail.search.metrics.scraper.services.archive.screenshot.ScreenshotRepository;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Using junit vintage (4) for spring 4 compatibility
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestingConfiguration.class})
@ActiveProfiles("testing")
@TestPropertySource(locations = {"classpath:properties/context-testing.properties"},
        properties = {"scraper.persistentStoragePath=${java.io.tmpdir}/ephemeral"})
public class ArchiveTest {
    @Autowired
    private ArchiveController archiveController;

    @Autowired
    private HtmlResponseRepository htmlResponseRepository;

    @Autowired
    private JsonResponseRepository jsonResponseRepository;

    @Autowired
    private ScreenshotRepository screenshotRepository;

    @Value("${scraper.persistentStoragePath}")
    private String path;

    @After
    public void tearDown() {
        FileSystemUtils.deleteRecursively(new File(path));
    }

    @Test
    @DirtiesContext
    public void archiveHtmlTest() {
        String content = "hello string";

        String[] rid = htmlResponseRepository.save(content, "content").getId().split("/");
        Response r = archiveController.getArchive("html", rid[1], rid[0]);
        String result = (String) r.getEntity();
        assertEquals(content, result);
    }

    @Test
    @DirtiesContext
    public void archiveJsonTest() {
        String content = "hello string";

        String[] rid = jsonResponseRepository.save(content, "content").getId().split("/");
        Response r = archiveController.getArchive("json", rid[1], rid[0]);
        String result = (String) r.getEntity();
        assertEquals(content, result);
    }

    @Test
    @DirtiesContext
    public void archiveScreenshotTest() {
        byte[] content = "hello string".getBytes();

        String[] rid = screenshotRepository.save(content, "content").getId().split("/");
        Response r = archiveController.getArchive("screenshot", rid[1], rid[0]);
        byte[] result = (byte[]) r.getEntity();
        assertArrayEquals(content, result);
    }
}
