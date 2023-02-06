package ui_tests.src.test.java.pages.importPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

public class ImportPage {
    private final WebDriver webDriver;

    public ImportPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить значение из поля Текст комментария
     *
     * @return
     */
    public String getCommentText() {
        try {
            return Entity.properties(webDriver).getValueField("commentText");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить данные из поля \"Текст комментария\"\n" + throwable);
        }
    }

    /**
     * Получить значение из поля Статус
     *
     * @return
     */
    public String getStatus() {
        try {
            return Entity.properties(webDriver).getValueField("status");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить данные из поля \"Статус\"\n" + throwable);
        }
    }

    /**
     * Получить значение из поля Обработано строк
     *
     * @return
     */
    public String getProgress() {
        try {
            return Entity.properties(webDriver).getValueField("progress");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить данные из поля \"Обработано строк\"\n" + throwable);
        }
    }

    /**
     * Получить значение текста из поля Результат импорта
     *
     * @return
     */
    public String getSummaryText() {
        try {
            return Entity.properties(webDriver).getValueField("summary");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Результат импорта\"\n" + throwable);
        }
    }

    /**
     * Получить значение ссылки из поля Результат импорта
     *
     * @return
     */
    public String getLinkOnSummaryPage() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("summary");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить ссылку из поля \"Результат импорта\"\n" + throwable);
        }
    }

    /**
     * Получить значение текста из поля Конфигурация импорта
     *
     * @return
     */
    public String getTitleConfigurationImport() {
        try {
            return Entity.properties(webDriver).getValueField("configuration");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Конфигурация импорта\"\n" + throwable);
        }
    }

    /**
     * Получить значение ссылки из поля Конфигурация импорта
     *
     * @return
     */
    public String getLinkOnConfigurationImportPage() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("configuration");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить ссылку из поля \"Конфигурация импорта\"\n" + throwable);
        }
    }

    /**
     * Получить значение текста из поля Файл с данными
     *
     * @return
     */
    public String getTitleSourcesFile() {
        try {
            return Entity.properties(webDriver).getValueField("sources");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Файл с данными\"\n" + throwable);
        }
    }

    /**
     * Получить значение ссылки из поля Файл с данными
     *
     * @return
     */
    public String getLinkOnSourcesFile() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("sources");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить ссылку из поля \"Файл с данными\"\n" + throwable);
        }
    }

    /**
     * Получить значение текста из поля Файл с логами
     *
     * @return
     */
    public String getTitleLogsFile() {
        try {
            return Entity.properties(webDriver).getValueField("logs");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Файл с логами импорта\"\n" + throwable);
        }
    }

    /**
     * Получить значение ссылки из поля Файл с логами
     *
     * @return
     */
    public String getLinkOnLogsFile() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("logs");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить ссылку из поля \"Файл с логами\"\n" + throwable);
        }
    }
}
