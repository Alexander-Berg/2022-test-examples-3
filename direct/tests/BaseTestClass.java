package ru.yandex.autotests.direct.tests;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.autotests.direct.Properties;
import ru.yandex.autotests.direct.data.ApiMode;
import ru.yandex.autotests.direct.data.Cmd;
import ru.yandex.autotests.direct.objects.advqstrings.Word;
import ru.yandex.autotests.direct.objects.report.Report;
import ru.yandex.autotests.direct.objects.report.ReportRow;
import ru.yandex.autotests.direct.objects.report.ReportTable;
import ru.yandex.autotests.direct.utils.AdvqError;
import ru.yandex.autotests.direct.utils.RetryRule;

import javax.xml.bind.JAXB;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assume.assumeFalse;
import static ru.yandex.autotests.direct.data.AdvqConstants.*;
import static ru.yandex.autotests.direct.data.ReportConstants.*;
import static ru.yandex.autotests.direct.utils.ApiHelper.getApiResponse;

/**
 * Базовый класс для всех тестов
 * @author xy6er
 */
@RunWith(Parameterized.class)
public abstract class BaseTestClass {
    protected static final Logger LOGGER = LogManager.getLogger(BaseTestClass.class);

    private static final Report REPORT = new Report();

    private static boolean[] isWordTestedFlags;
    private static boolean isTestClassRetry = false;
    private static boolean isFindTainted = false;

    protected final Word word;
    protected ReportRow reportRow;

    @ClassRule
    public static final RetryRule RETRY_RULE = RetryRule.retry()
            .ifException(AdvqError.class)
            .times(1);

    @Parameterized.Parameters(name = TEST_NAME_FORMAT)
    public static List<Object[]> wordsData() throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                Thread.currentThread().getContextClassLoader().getResourceAsStream("words.txt"), "UTF-8"));
        List<Object[]> convertedList = new ArrayList<>(Properties.getInstance().getTestWordsCount());
        String line;
        for (int id = 0; id < Properties.getInstance().getTestWordsCount() && (line = reader.readLine()) != null; id++){
            convertedList.add( new Object[]{ new Word(line, id) } );
        }
        isWordTestedFlags = new boolean[convertedList.size()];
        return convertedList;
    }

    /**
     * Проверяет, если isWordTestedFlags[word.getId()]=true,
     *  значит слово протестировано при первом запуске тескласса, и для него не нужно запускать тест
     * @param word Фраза для запроса
     */
    public BaseTestClass(Word word) {
        assumeFalse("Cлово протестировано при первом запуске тескласса", isWordTestedFlags[word.getId()]);
        this.word = word;
    }

    /**
     * Проверка отвечает ли на запрос продакшен и бета.
     */
    protected static void checkApiIsWork() {
        try {
            getApiResponse(ApiMode.PROD, Cmd.SEARCH, TEST_WORD);
            getApiResponse(ApiMode.BETA, Cmd.SEARCH, TEST_WORD);
        } catch (AdvqError error) {
            REPORT.getErrorMessages().add(error.getMessage());
            REPORT.setStatus(FAIL);
            throw error;
        }
    }

    /**
     * Проверяет, если prodTainted=false и betaTainted=false,
     *      то флаг для слова помечается 'true' - это значит, что при перезапуске тестКласса слово будет пропущено.
     * Иначе по окончанию всех тестов, тестКласс будет перезапущен, если он до этого не был перезапущен.
     * @param prodTainted Значения tainted на продакшене
     * @param betaTainted Значения tainted на бете
     */
    protected void checkTainted(boolean prodTainted, boolean betaTainted) {
        if (!prodTainted && !betaTainted) {
            isWordTestedFlags[word.getId()] = true;
        } else {
            String message = String.format("После 3 попыток значение TAINTED=TRUE в базе %s для слова '%s'",
                    prodTainted ? "продакшена" : "беты", word);
            if (isTestClassRetry) {
                REPORT.getErrorMessages().add(message);
                REPORT.setStatus(FAIL);
            } else {
                isFindTainted = true;
            }
            throw new AdvqError(message);
        }
    }

    /**
     * Расчитывает разницу показов, продакшен берется за 100%.
     * Проверяет, что разница меньше MAX_TOTAL_COUNT_DIFF
     * @param prodCount Количество показов на продакшене
     * @param betaCount Количество показов на бете
     */
    protected void setTotalCountDiff(ReportTable reportTable, int prodCount, int betaCount) {
        double diff = betaCount - prodCount;
        diff = diff / (double) prodCount * 100;
        reportRow.setDiff(String.format(Locale.US, "%.2f%%", diff));

        if (Math.abs(diff) > MAX_TOTAL_COUNT_DIFF) {
            LOGGER.error("Разница должна быть меньше 50%");
            reportTable.setStatus(FAIL);
        }
    }

    /**
     * Расчитывает разницу кол-во недель/месяцев, продакшен берется за 100%.
     * Проверяет, что на продакшене не должно быть больше недель/месяцев, чем на бете.
     * Проверяет, что на бете должно быть не более 2ух недель/месяцев, чем на продакшене.
     * @param prodCount Количество недель/месяцев на продакшене
     * @param betaCount Количество недель/месяцев на бете
     * @param name      Неделя или Месяц
     */
    protected void setCountDiff(ReportTable reportTable, int prodCount, int betaCount, String name) {
        int diff = betaCount - prodCount;
        reportRow.setDiff(String.valueOf(diff));

        if (diff < MIN_COUNT_DIFF) {
            LOGGER.error(String.format("На продакшене не должно быть больше %s, чем на бете", name));
            reportTable.setStatus(FAIL);

        } else if (diff > MAX_COUNT_DIFF) {
            LOGGER.error(String.format("На бете не должно быть больше %s чем на +%d", name, MAX_COUNT_DIFF));
            reportTable.setStatus(FAIL);
        }
        if ( !reportTable.getReportRows().isEmpty() &&
                Integer.parseInt(reportTable.getReportRows().get(0).getDiff()) != diff) {
            LOGGER.error(String.format("Разница по кол-ву %s, не совпадает по всем словам", name));
            reportTable.setStatus(FAIL);
        }
    }

    /**
     * Добавляет reportTable в REPORT.
     * Обновляет статус репорта.
     * @param title Заголовок таблицы
     * @param header Название первого столбца таблицы
     */
    protected static void addReportTable(ReportTable reportTable, String title, String header,
                                         String prodColumnName, String betaColumnName, String diffColumnName) {
        if (reportTable == null) {
            LOGGER.info("reportTable == null in addReportTable");
            return;
        }
        if ( !reportTable.getReportRows().isEmpty() ) {
            reportTable.setTitle(title);
            reportTable.setHeader(header);
            reportTable.setProdColumnName(prodColumnName);
            reportTable.setBetaColumnName(betaColumnName);
            reportTable.setDiffColumnName(diffColumnName);
            REPORT.getReportTables().add(reportTable);

            if (reportTable.getStatus().equals(FAIL)) {
                REPORT.setStatus(FAIL);
            }
        }
    }

    protected static void addReportTable(ReportTable reportTable, String title, String header) {
        addReportTable(reportTable, title, header, "Продакшен", "Бета", "Разница");
    }

    /**
     * Если тестКласс был запущен впервые и в тестах была получено tainted=true,
     *  то выбрасывается AdvqError, для того чтобы RetryRule перезапустил тестКласс.
     */
    private static void checkTestClassForTainted() {
        if (!isTestClassRetry && isFindTainted) {
            isTestClassRetry = true;
            throw new AdvqError("Retry testClass!");
        }
    }

    /**
     * Заполняет оставщиеся поля REPORT. Если репорт не содержит таблиц,
     *  то добавляет пустую таблицу с заголовком 'Нет расхождений'.
     * Создает xml файл из репорта, с именем className.xml
     * @param className Имя тестКласса
     * @param title Заголовок репорта
     */
    protected static void createReportFiles(String className, String title) {
        checkTestClassForTainted();
        REPORT.setTitle(title);
        if (REPORT.getReportTables().isEmpty() && REPORT.getErrorMessages().isEmpty()) {
            ReportTable reportTable = new ReportTable();
            reportTable.setTitle("Нет расхождений");
            REPORT.getReportTables().add(reportTable);
        }
        REPORT.setAdvqProd(ApiMode.PROD.getUrl());
        REPORT.setAdvqBeta(ApiMode.BETA.getUrl());

        //TODO-b delete
        for (ReportTable table : REPORT.getReportTables()) {
            assertThat("ReportTable == null :(", table, notNullValue());
            assertThat("ReportTable TITLE == null :(", table.getTitle(), notNullValue());
            for (ReportRow row : table.getReportRows()) {
                assertThat("Row == null for " + table.getTitle(), row, notNullValue());
                assertThat("Row NAME == null for " + table.getTitle(), row.getName(), notNullValue());
            }
        }

        Collections.sort(REPORT.getReportTables(), new Comparator<ReportTable>() {
            @Override
            public int compare(ReportTable o1, ReportTable o2) {
                int result = o1.getStatus().compareTo(o2.getStatus());
                if (result == 0) {
                    result = o1.getTitle().compareTo(o2.getTitle());
                }
                return result;
            }
        });

        File reportDir = new File(REPORT_DIR_PATH);
        reportDir.mkdirs();
        File reportFile = new File(reportDir, className + ".xml");
        JAXB.marshal(REPORT, reportFile);
        LOGGER.info("REPORT FINISHED!!!");

        //TODO-b delete
        for (ReportTable table : REPORT.getReportTables()) {
            for (int i = 0; i < table.getReportRows().size(); i++) {
                for (int j = i + 1; j < table.getReportRows().size(); j++) {
                    if (table.getReportRows().get(i).getName()
                            .equals(table.getReportRows().get(j).getName())) {
                        throw new RuntimeException("Дублирующиеся строки для " + table.getTitle() + " | "
                                + table.getReportRows().get(i).getName());
                    }
                }
            }
        }
    }

}
