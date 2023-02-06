package ru.yandex.market.checker;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import liquibase.util.csv.CSVReader;
import liquibase.util.csv.CSVWriter;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.common.util.TestHelper;
import ru.yandex.common.util.http.HttpClientFactory;
import ru.yandex.common.util.http.HttpClientFactoryImpl;
import ru.yandex.common.util.http.loader.PageBase;
import ru.yandex.inside.passport.tvm2.Tvm2;
import ru.yandex.market.checker.core.CoreCheckerTask;
import ru.yandex.market.checker.core.TaskState;
import ru.yandex.market.checker.zora.ZoraCheckerTaskRunner;
import ru.yandex.market.checker.zora.ZoraHttpService;
import ru.yandex.market.checker.zora.util.exception.WrongRequestToZoraException;

import static org.junit.Assert.assertNotNull;

/**
 * @author valeriashanti
 * @date 17/03/2020
 * Создаем тестинговый {@link Tvm2} для авторизации в зоре.
 * Информация по зоре в АБО - https://wiki.yandex-team.ru/market/development/abo/zora/.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:zora-test-bean.xml")
@Transactional
public class ZoraLoaderTest {

    /**
     * Если нужно проверить работу зоры от нашего источника, то нужно добавить значение ENVIRONMENT - production.
     */
    private static final String ENVIRONMENT = "testing";

    /**
     * Подставляем любой урл для скачивания.
     * https://httpstat.us удобрый способ возвращать конкретный ответ для тестирования.
     */
    private static final String URL = "https://httpstat.us/200";

    /**
     * Подставить на место XXX market.abo.tvm.secret для тестинга - из https://yav.yandex-team.ru
     * {@link market-abo-testing-properties}.
     */
    static {
        System.setProperty("environment", ENVIRONMENT);
        System.setProperty("market.abo.tvm.secret", "XXX");
    }

    @Autowired
    private ZoraHttpService zoraHttpService;

    private final HttpClientFactory factory = HttpClientFactoryImpl.getInstance();

    private ZoraCheckerTaskRunner zoraCheckerTaskRunner;

    @BeforeEach
    void init() {
        zoraCheckerTaskRunner = new ZoraCheckerTaskRunner(ENVIRONMENT, "", zoraHttpService);
    }

    /**
     * Скачивает html страницу напрямую через {@link ZoraHttpService}.
     */
    @Test
    @Disabled
    void loadUrlFromHttpPageLoader() {
        CoreCheckerTask coreCheckerTask = TestHelper.createTask(URL);
        HttpGet httpGet = zoraCheckerTaskRunner.prepareMethod(coreCheckerTask, factory, URL);
        try {
            HttpResponse response = zoraHttpService.executeRequest(httpGet);
            assertNotNull(response);
            System.out.println(response);
        } catch (IOException | WrongRequestToZoraException e) {
            e.printStackTrace();
        }
    }

    /**
     * Скачивает html страницу через общий метод чекера loadPage().
     */
    @Test
    @Disabled
    void loadUrlFromZoraCheckerTaskRunner() {
        CoreCheckerTask coreCheckerTask = TestHelper.createTask(URL);
        PageBase page = zoraCheckerTaskRunner.loadPage(coreCheckerTask);
        TaskState state = HttpStatus.SC_OK == coreCheckerTask.getHttpStatus() ? TaskState.SUCCESS : TaskState.FAIL;

        System.out.println(coreCheckerTask.getHttpStatus() + " " + coreCheckerTask.getErrorMessage() + " " + state);
        System.out.println(page != null ? new String(page.getBytes()) : "");
        assertNotNull(page);
    }

    /**
     * Принимает Сsv в формате [shop_id,url]. Записывает в формате [shop_id,url,httpStatus].
     */
    @Test
    @Disabled
    void loadUrlFromCsvAndWriteToCsv() throws IOException {
        try (CSVReader csvReader = new CSVReader(new FileReader(new File("")), '\t')) {
            FileWriter fileWriter = new FileWriter(new File(""));
            try (CSVWriter csvWriter = new CSVWriter(fileWriter, '\t')) {
                String[] line;
                while ((line = csvReader.readNext()) != null) {

                    String[] answers = new String[3];
                    var coreCheckerTask = TestHelper.createTask(line[1]);
                    zoraCheckerTaskRunner.loadPage(coreCheckerTask);

                    answers[0] = line[0];
                    answers[1] = line[1];
                    answers[2] = String.valueOf(coreCheckerTask.getHttpStatus());
                    csvWriter.writeNext(answers);
                }
            }
        }
    }
}

