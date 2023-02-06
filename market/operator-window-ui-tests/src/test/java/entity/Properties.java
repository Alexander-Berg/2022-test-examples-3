package ui_tests.src.test.java.entity;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import tools.Tools;
import unit.Config;

import java.util.ArrayList;
import java.util.List;

public final class Properties {

    private WebDriver webDriver;

    public Properties(WebDriver webDriver) {
        this.webDriver = webDriver;
    }

    public String getXPathElement(String attributeCode) {
        return "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']";
    }

    /**
     * Указать значение в поле для файла
     *
     * @param findBlock     блок в котором находится элемент
     * @param attributeCode код атрибута
     * @param filePath      путь до файла
     */
    public void setPropertiesFileField(String findBlock, String attributeCode, String filePath) {
        try {
            By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input[@type='file']");
            Tools.waitElement(webDriver).waitElementToAppearInDOM(fieldBy);
            WebElement webElement = Tools.findElement(webDriver).findElement(fieldBy);
            webElement.sendKeys(filePath);
        } catch (Throwable throwable) {
            throw new Error("Не удалось указать значение в поле для загрузки файла\n" + throwable);
        }
    }

    public Properties setInputField(String attributeCode, String value) {
        return setInputField("", attributeCode, value);
    }

    public Properties setInputField(String findBlock, String attributeCode, String value) {
        if (value != null) {
            By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input");
            Tools.sendElement(webDriver).sendElement(fieldBy, value);
        }
        return this;
    }

    /**
     * Задать значение поля селекта
     *
     * @param findBlock     блок в котором находится элемент
     * @param attributeCode код атрибута
     * @param value         значение атрибута
     */
    public void setPropertiesOfSelectTypeField(String findBlock, String attributeCode, String value) {
        if (value != null) {
            By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//button");
            String xpath = Entity.properties(webDriver).getXPathElement(value) + "/..";
            Tools.clickerElement(webDriver).clickElement(fieldBy);
            Tools.clickerElement(webDriver).clickElement(By.xpath(xpath));

        }
    }

    /**
     * Задать значение поля селекта
     *
     * @param attributeCode код атрибута
     * @param value         значение атрибута
     */
    public void setPropertiesOfSelectTypeField(String attributeCode, String value) {
        setPropertiesOfSelectTypeField("", attributeCode, value);
    }

    /**
     * Нажать на "сбросить выбор" в селекте с заданным data-ow-test-attribute-container
     */
    public void clickResetSelectionButton(String attributeCode) {
        WebElement element = Tools.findElement(webDriver).findElement(By.xpath(String.format(
                "//*[@*[starts-with(name(.),'data-ow-test')]='%s']//button", attributeCode)));
        Tools.clickerElement(webDriver).clickElement(element);
        Tools.clickerElement(webDriver).clickElement(element.findElement(By.xpath("//button[text()='сбросить выбор']")));
    }

    /**
     * Задать значение для поля Саджестом
     *
     * @param attributeCode код атрибута
     * @param value         значение которое необходимо ввести
     * @return
     */
    public Properties setPropertiesOfSuggestTypeField(String attributeCode, String value) {
        return setPropertiesOfSuggestTypeField("", attributeCode, value);
    }

    /**
     * Задать значение для поля с выпадающим списком
     *
     * @param attributeCode код атрибута
     * @param value         значение которое необходимо ввести
     * @param findBlock     блок в котором находится элемент
     * @return
     */
    public Properties setPropertiesOfSuggestTypeField(String findBlock, String attributeCode, String value) {
        try {
            if (value != null) {
                By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input");
                Tools.sendElement(webDriver).sendElement(fieldBy, value);
                Tools.clickerElement(webDriver).clickElement(fieldBy);
                Tools.waitElement(webDriver).waitInvisibleLoadingElement();
                Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@id='ow-popper-portal']//*[ text()='" + value + "']"));
            }
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение поля с выпадающим списком\n" + t);
        }
    }

    /**
     * Задать значение для поля с выпадающим списком если название элемента отличается от вводимого
     *
     * @param attributeCode код атрибута
     * @param value         значение которое необходимо ввести
     * @param findBlock     блок в котором находится элемент
     * @param name          имя элемента которое надо выбрать
     * @return
     */
    public Properties setPropertiesOfSuggestTypeFieldWhenSearchById(String findBlock, String attributeCode, String value, String name) {
        try {
            if (value != null) {
                By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input");
                Tools.sendElement(webDriver).sendElement(fieldBy, value);
                Tools.clickerElement(webDriver).clickElement(fieldBy);
                Tools.waitElement(webDriver).waitInvisibleLoadingElement();
                Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@id='ow-popper-portal']//*[ text()='" + name + "']"));
            }
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение поля с выпадающим списком\n" + t);
        }
    }

    public Properties setPropertiesOfTextArea(String findBlock, String attributeCode, String value) {
        try {
            if (value != null) {
                By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//textarea");
                Tools.sendElement(webDriver).sendElement(fieldBy, value);
            }
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение поля TextArea\n" + t);
        }
    }

    /**
     * Получить текст введенный в поле  text area
     *
     * @return
     */
    public String getEnteredTextInTextArea(String findBlock) {
        By byXPath = By.xpath(findBlock + "//textarea[@aria-invalid=\"false\"]");
        return Tools.findElement(webDriver).findVisibleElement(byXPath).getText();
    }

    public Properties setPropertiesOfTextArea(String attributeCode, String value) {
        setPropertiesOfTextArea("", attributeCode, value);
        return this;
    }

    /**
     * Задать значение для поля с выпадающим списком
     *
     * @param attributeCode код атрибута
     * @param values        массив элементов которые необходимо выбрать
     * @param findBlock     блок в котором находится элемент
     * @return
     */
    public Properties setPropertiesOfMultiSuggestTypeField(String findBlock, String attributeCode, List<String> values) {
        Entity.toast(webDriver).hideNotificationError();
        try {
            if (values.size() > 0) {

                for (String value : values) {
                    setPropertiesOfSuggestTypeField(findBlock, attributeCode, value);
                }
            }
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение поля с выпадающим списком\n" + t);
        }
    }

    /**
     * Задать значение для поля с выпадающим списком
     *
     * @param attributeCode код атрибута
     * @param values        массив элементов которые необходимо выбрать
     * @return
     */
    public Properties setPropertiesOfMultiSuggestTypeField(String attributeCode, List<String> values) {
        return setPropertiesOfMultiSuggestTypeField("", attributeCode, values);
    }

    /**
     * Задать значение поля с чекбоксами в выпадающем списке
     *
     * @param attributeCode - код атрибута
     * @param values        - значения поля которые нужно выбрать
     * @param findBlock     блок где находится элемент
     * @return
     */
    public Properties setPropertiesOfTreeSelectTypeField(String findBlock, String attributeCode, List<String> values) {
        try {
            if (values != null) {
                for (String value : values) {
                    By fieldBy = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input");
                    Tools.sendElement(webDriver).sendElement(fieldBy, value);
                    Tools.clickerElement(webDriver).clickElement(fieldBy);
                    Tools.waitElement(webDriver).waitInvisibleLoadingElement();
                    Tools.waitElement(webDriver).waitVisibilityElement(By.xpath(findBlock + "//*[text()='" + value + "']/../../../preceding-sibling::*//input/.."));
                    if (!Tools.findElement(webDriver).findElement(By.xpath(findBlock + "//*[text()='" + value + "']/../../../preceding-sibling::*//input[@type='checkbox']")).isSelected()) {
                        Tools.clickerElement(webDriver).clickElement(By.xpath(findBlock + "//*[text()='" + value + "']/../../../preceding-sibling::*//input/.."));
                    }
                }
            }
            return this;
        } catch (Throwable t) {
            throw new Error("Не удалось указать значение поля TreeSelect \n" + t);
        }
    }

    public void clearPropertiesOfMultiSuggestTypeField(String findBlock, String attributeCode) {
        By values = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//label/button");
        try {
            List<WebElement> webElements = Tools.findElement(webDriver).findElements(values);
            for (WebElement webElement : webElements) {
                Tools.waitElement(webDriver).waitClickableElement(webElement).click();
            }
        } catch (Throwable error) {

        }
    }

    public void clearPropertiesOfSuggestTypeField(String findBlock, String attributeCode) {
        By values = By.xpath(findBlock + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input/..//button");
        try {
            List<WebElement> webElements = Tools.findElement(webDriver).findElements(values);
            for (WebElement webElement : webElements) {
                Tools.waitElement(webDriver).waitClickableElement(webElement).click();
            }
        } catch (Throwable error) {

        }
    }

    /**
     * Задать значение поля с чекбоксами в выпадающем списке
     *
     * @param attributeCode - код атрибута
     * @param values        - значения поля которые нужно выбрать
     * @return
     */
    public Properties setPropertiesOfTreeSelectTypeField(String attributeCode, List<String> values) {
        return setPropertiesOfTreeSelectTypeField("", attributeCode, values);
    }

    /**
     * Задать значение чек-бокса
     */
    public Properties setPropertiesOfCheckBoxTypeField(String attributeCode, boolean status) {
        try {
            By fieldBy = By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input");
            boolean currentStatus = Tools.findElement(webDriver).findElement(fieldBy).isSelected();
            if (status != currentStatus) {
                Tools.clickerElement(webDriver).clickElement(By.xpath("//*[@data-ow-test-attribute-container='" + attributeCode + "']//input/.."));
            }
            return this;
        } catch (Exception e) {
            throw new Error("Не удалось нажать на чекбокс: \n" + e);
        }
    }

    /**
     * Получить значение поля
     *
     * @param attributeCode код поля
     * @param findBlock     блок где находится элемент
     * @return значение поля
     */
    public String getValueField(String findBlock, String attributeCode) {
        By fieldXPath = By.xpath(findBlock + getXPathElement(attributeCode));
        try {
            List<WebElement> webElements;
            WebElement webElement = Tools.findElement(webDriver).findElementInDOM(fieldXPath, Config.DEF_TIME_WAIT_LOAD_PAGE);
            List<WebElement> webElementsLocator = Tools.findElement(webDriver).findElements(fieldXPath);
            if (webElementsLocator.size() > 1) {
                webElements = webElementsLocator.get(1).findElements(By.xpath("./*"));
            } else {
                webElements = webElementsLocator.get(0).findElements(By.xpath("./*"));
            }
            if (webElements.size() > 0) {
                return webElements.get(0).getText().trim();
            } else {
                return "";
            }
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение поля " + attributeCode + " \n" + e);
        }

    }

    /**
     * Получить значение поля из WebElement
     *
     * @param attributeCode    код поля
     * @param findInWebElement блок где находится элемент
     * @return значение поля
     */
    public String getValueField(WebElement findInWebElement, String attributeCode) {
        try {
            return findInWebElement.findElement(By.xpath(".//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']/*")).getText().trim();
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение поля " + attributeCode + " \n" + e);
        }

    }

    /**
     * Получить значение поля
     *
     * @param attributeCode код поля
     * @return значение поля
     */
    public String getValueField(String attributeCode) {
        return getValueField("", attributeCode);
    }

    public List<String> getValuesField(String attributeCode) {
        List<String> webElements = new ArrayList<>();
        try {
            List<WebElement> values = Tools.findElement(webDriver).findElements(By.xpath("//*[@*[starts-with(name(.),'data-ow-test') and @data-ow-test-hidden]='" + attributeCode + "']/*/*"));

            if (webElements.size() > 0) {
                webElements.remove(0);
            }
            for (WebElement webElement : values) {
                webElements.add(webElement.getText().trim());
            }
        } catch (Throwable t) {

        }
        return webElements;
    }

    /**
     * Проверить, что в элементе с указанным attributeCode нет значения
     */
    public boolean checkEmptyValueField(String attributeCode) {
        return checkEmptyValueField("", attributeCode);
    }

    /**
     * Проверить, что в элементе с указанным attributeCode нет значения
     */
    public boolean checkEmptyValueField(String block, String attributeCode) {
        return Tools.findElement(webDriver)
                .findElements(By.xpath(block + "//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']/*/*//*[@title]"))
                .size() == 0;
    }

    /**
     * Получить ссылку на страницу элемента
     *
     * @param attributeCode код поля
     * @return
     */
    public String getValueLinkToPageEntity(String attributeCode) {
        By fieldXPath = By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//*[@href]");

        try {
            return Tools.findElement(webDriver).findElementInDOM(fieldXPath).getAttribute("href");
        } catch (Throwable e) {
            throw new Error("Не удалось получить значение поля " + attributeCode + " \n" + e);
        }

    }

    /**
     * Получить состояние флага checkbox
     *
     * @param attributeCode
     * @return
     */
    public boolean getBooleanStateFlag(String attributeCode) {
        By fieldXPath = By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//input");
        WebElement webElement = Tools.findElement(webDriver).findElementInDOM(fieldXPath);
        return webElement.isSelected();
    }


    public void setRichTextEditor(String attributeCode, String text) {
        try {
            WebElement webElement = Tools.waitElement(webDriver).waitClickableElement(By.xpath("//*[@contenteditable=\"true\"]"));
            webElement.sendKeys(text);
        } catch (Throwable t) {
            throw new Error("Не удалось ввести текст в поле RichTextEditor \n" + t);
        }
    }

    public String getEnteredTextInRichTextEditor(String attributeCode) {
        try {
            StringBuilder textFromPage = new StringBuilder();
            Tools.waitElement(webDriver).waitVisibilityElement(By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]='" + attributeCode + "']//*[@data-contents=\"true\"]/div/div"));
            List<WebElement> textFromWebElement = Tools.findElement(webDriver).findElements(By.xpath("//*[@data-contents=\"true\"]/div/div"));
            for (WebElement element : textFromWebElement) {
                textFromPage.append(element.getText().trim()).append(" ");
            }
            return textFromPage.toString().trim();
        } catch (Throwable t) {
            throw new Error("Не удалось получить текст, введенный в поле комментрия \n" + t);
        }
    }
}
