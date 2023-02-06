package ru.yandex.market.logistics.logging.rest.transactional;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.logistics.logging.rest.transactional.json.RestInTransactionMonitoringConfiguration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

public class RestInTransactionAdviceTest extends AbstractContextualTest {
    @Autowired
    private ServiceWithRestCall service;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${logging.path}")
    private String logDirPath;
    private static File logFile;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.setActualTransactionActive(false);

        File logDir = new File(logDirPath);
        logFile = new File(logDir, RestInTransactionMonitoringConfiguration.LOG_FILE_NAME + ".log");
    }

    @Test
    public void doLoggingOnSuccess(TestInfo testInfo) throws IOException {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        mockOkReponse();

        int logOffset = logLineCount();
        service.methodWithRest();

        assertRowsCountForTest(testInfo, logOffset, 1);
    }


    @Test
    public void doLoggingOnFail(TestInfo testInfo) throws IOException {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        doThrow(new RuntimeException())
            .when(restTemplate)
            .getForEntity(eq(ServiceWithRestCall.URL), eq(Object.class));

        int logOffset = logLineCount();
        try {
            service.methodWithRest();
        } catch (Exception e) {
            // do nothing
        }

        assertRowsCountForTest(testInfo, logOffset, 1);
    }

    @Test
    public void noRestNoLogging(TestInfo testInfo) throws IOException {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        doThrow(new RuntimeException())
            .when(restTemplate)
            .getForEntity(eq(ServiceWithRestCall.URL), eq(Object.class));

        int logOffset = logLineCount();
        service.methodWithoutRest();

        assertRowsCountForTest(testInfo, logOffset, 0);
    }

    @Test
    public void doNoTransactionNoLogging(TestInfo testInfo) throws IOException {
        mockOkReponse();

        int logOffset = logLineCount();
        service.methodWithRest();

        assertRowsCountForTest(testInfo, logOffset, 0);
    }

    private void assertRowsCountForTest(TestInfo testInfo, int logOffset, int i) throws IOException {
        AtomicInteger counter = new AtomicInteger(logOffset);
        List<String> lines = Files.lines(Paths.get(logFile.getAbsolutePath()))
            .filter(line -> counter.getAndDecrement() <= 0)
            .collect(Collectors.toList())
            .stream()
            .filter(line -> line.contains(testInfo.getTestMethod().map(Method::getName).orElse("")))
            .collect(Collectors.toList());
        softly.assertThat(lines.size()).isEqualTo(i);
    }

    private int logLineCount() throws IOException {
        return (int) Files.lines(Paths.get(logFile.getAbsolutePath())).count();
    }

    private void mockOkReponse() {
        Mockito.when(restTemplate.getForEntity(eq(ServiceWithRestCall.URL), eq(Object.class)))
            .thenReturn(new ResponseEntity<>("", new HttpHeaders(), HttpStatus.OK));
    }
}
