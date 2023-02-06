package ui_tests.src.test.java.pages.importsSummaryPage;

import entity.Entity;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.List;

public class ImportsSummaryPage {
    private WebDriver webDriver;

    public ImportsSummaryPage(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    /**
     * Получить название результата импорта
     *
     * @return
     */
    public String getTitle() {
        try {
            return Entity.properties(webDriver).getValueField("title");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Название\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "Строк получено"
     *
     * @return
     */
    public String getReceived() {
        try {
            return Entity.properties(webDriver).getValueField("received");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Строк получено\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "Строк пропущено"
     *
     * @return
     */
    public String getSkipped() {
        try {
            return Entity.properties(webDriver).getValueField("skipped");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Строк пропущено\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "Успешно"
     *
     * @return
     */
    public String getSuccessful() {
        try {
            return Entity.properties(webDriver).getValueField("successful");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Успешно\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "Создано"
     *
     * @return
     */
    public String getCreated() {
        try {
            return Entity.properties(webDriver).getValueField("created");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Объектов создано\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "Изменено"
     *
     * @return
     */
    public String getUpdated() {
        try {
            return Entity.properties(webDriver).getValueField("updated");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Объектов изменено\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "С ошибкой"
     *
     * @return
     */
    public String getFailed() {
        try {
            return Entity.properties(webDriver).getValueField("failed");
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"С ошибкой\"\n" + throwable);
        }
    }

    /**
     * Получить значение поля "Id неудачных строк"
     *
     * @return
     */
    public List<String> getFailedIds() {
        try {
            return Arrays.asList(Entity.properties(webDriver).getValueField("failedIds").replace(" ", "").split(","));
        } catch (Throwable throwable) {
            throw new Error("Не удалось получить текст из поля \"Id неудачных строк\"\n" + throwable);
        }
    }
}
