package ui_tests.src.test.java.entity.modalWindow;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

import java.util.ArrayList;
import java.util.List;

public class Content extends Entity {
    private String block = "//*[@data-ow-test-modal-body]";
    private WebDriver webDriver;

    public Content(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    /**
     * Нажать на кнопкуу действия
     *
     * @param buttonName название кнопки
     */
    public void clickActionButton(String buttonName) {
        buttons(webDriver).clickButton(block, buttonName);
    }

    /**
     * Нажать на кнопку с текстом
     *
     * @param buttonName текст кнопки
     * @return
     */
    public void clickButton(String buttonName) {
        buttons(webDriver).clickCustomButton(block, buttonName);
    }

    /**
     * Получить все шаблоны сообщений
     *
     * @return
     */
    public List<String> getTitleTemplates() {
        try {
            List<String> messageTemplates = new ArrayList<>();
            List<WebElement> messageTemplatesButton = Tools.findElement(webDriver).findElements(By.xpath(block + "//button[text()]"));
            for (WebElement webElement : messageTemplatesButton) {
                messageTemplates.add(webElement.getText());
            }
            return messageTemplates;
        } catch (Throwable t) {
            throw new Error("Не удалось получить список шаблонов сообшений\n" + t);
        }
    }

    /**
     * Получить текст шаблона сообщения
     *
     * @param templateName
     * @return
     */
    public String getTemplateText(String templateName) {
        try {
            StringBuilder textTemplate = new StringBuilder();
            List<WebElement> webElements = Tools.findElement(webDriver).findElements(By.xpath(block + "//*[text()='" + templateName + "']"));
            if (webElements.size() < 2) {
                throw new Error("Не отобразился текст шаблона " + templateName);
            }
            List<WebElement> textTemplatesWebElement = webElements.get(1).findElements(By.xpath("./../*[2]//p[text()]"));
            for (WebElement webElement : textTemplatesWebElement) {
                textTemplate.append(webElement.getText().trim()).append(" ");
            }
            return textTemplate.toString().trim();
        } catch (Throwable t) {
            throw new Error("Не удалось получить текст шаблона сообщения\n" + t);
        }
    }

    public void setPropertiesInputField(String keyProperties, String value) {
        properties(webDriver).setInputField(block, keyProperties, value);
    }

    public void setPropertiesOfSelectField(String attributeCode, String value) {
        properties(webDriver).setPropertiesOfSelectTypeField(block, attributeCode, value);
    }

    public void setPropertiesOfTextArea(String attributeCode, String value) {
        properties(webDriver).setPropertiesOfTextArea(block, attributeCode, value);
    }

    public void setPropertiesOfMultiSuggestTypeField(String attributeCode, List<String> values) {
        properties(webDriver).setPropertiesOfMultiSuggestTypeField(block, attributeCode, values);
    }

    public void setPropertiesOfSuggestTypeField(String attributeCode, String value) {
        properties(webDriver).setPropertiesOfSuggestTypeField(block, attributeCode, value);
    }

    public void setPropertiesOfTreeSelectTypeField(String attributeCode, List<String> values) {
        properties(webDriver).setPropertiesOfTreeSelectTypeField(attributeCode, values);
    }

}
