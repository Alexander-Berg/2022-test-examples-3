package ru.yandex.personal.mail.search.metrics.scraper.integration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.util.FileSystemUtils;

import ru.yandex.personal.mail.search.metrics.scraper.mocks.GmailMockFactory;
import ru.yandex.personal.mail.search.metrics.scraper.services.account.FileStorageAccountRepository;
import ru.yandex.personal.mail.search.metrics.scraper.services.archive.response.json.JsonResponseRepository;
import ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.gapi.GApiMessageToMessageSearchSnippetConverter;
import ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.gapi.GApiSearchSystem;
import ru.yandex.personal.mail.search.metrics.scraper.services.scraping.systems.gapi.GApiSearchSystemFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Configuration
@ImportResource({"classpath:spring/application-test-ctx.xml"})
public class TestingConfiguration {
    public static final int SERP_SIZE = 10;

    @Bean
    @Primary
    public GApiSearchSystemFactory mockedFactory() {
        GApiSearchSystemFactory mockedFactory = Mockito.mock(GApiSearchSystemFactory.class);
        GmailMockFactory serviceMockFactory = new GmailMockFactory();
        JsonResponseRepository rr = mock(JsonResponseRepository.class);
        when(mockedFactory.createServiceFromFiles(any(), any())).thenAnswer((Answer<GApiSearchSystem>) invocation ->
                new GApiSearchSystem(serviceMockFactory.mockGmail(SERP_SIZE),
                        new GApiMessageToMessageSearchSnippetConverter(),
                        rr, new ObjectMapper()));
        return mockedFactory;
    }

    @Bean(destroyMethod = "destroy")
    @Primary
    public DestroyableFileStorageAccountRepository tempFileSystemAccountRepository() throws IOException {
        Path temp = Files.createTempDirectory("test");
        while (Files.newDirectoryStream(temp).iterator().hasNext()) {
            temp = Files.createTempDirectory("test");
        }
        String tempDir = temp.toString();
        return new DestroyableFileStorageAccountRepository(tempDir);
    }

    private static class DestroyableFileStorageAccountRepository extends FileStorageAccountRepository {
        private final Path baseDir;

        DestroyableFileStorageAccountRepository(String baseDir) {
            super(baseDir, new ObjectMapper());
            this.baseDir = Paths.get(baseDir);
        }

        void destroy() {
            FileSystemUtils.deleteRecursively(baseDir.toFile());
        }
    }
}
