package ui_tests.src.test.java.pages.ticketPage.messageTab.orders.beruOrderTab;

import entity.Entity;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import tools.Tools;

public class DeliveryProperties {
    private final WebDriver webDriver;

    public DeliveryProperties(WebDriver webDriver) {
        this.webDriver = webDriver;
        PageFactory.initElements(this.webDriver, this);
    }

    // Кнопка контактов курьера
    @FindBy(xpath = "//button//*[text()='Контакты']")
    private WebElement contactInfoCourierButton;
    // Имя курьера
    @FindBy(xpath = "//*[@id='ow-popper-portal']//*[text()='Имя']/../*[2]")
    private WebElement nameContactCourier;
    // Номер телефона контакта курьера
    @FindBy(xpath = "//*[@id='ow-popper-portal']//*[text()='Телефон']/../*[2]")
    private WebElement phoneContactCourier;

    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='trackingLink']/*")
    private WebElement trackingLink;

    @FindBy(xpath = "//*[@*[starts-with(name(.),'data-ow-test')]='multiOrderLinks']/*")
    private WebElement multiOrderLink;


    /**
     * Получить трек-код заказа
     *
     * @return трек-код
     */
    public String getTrackCode() {
        try {
            return Entity.properties(webDriver).getValueField("deliveryServiceTrackCode");
        } catch (Throwable t) {
            throw new Error("Не удалось получить трек-код заказа\n" + t);
        }
    }

    /**
     * Получить дату доставки при оформлении
     *
     * @return
     */
    public String getOriginalDeliveryTimeFull() {
        try {
            return Entity.properties(webDriver).getValueField("originalDeliveryTimeFull");
        } catch (Throwable t) {
            throw new Error("Не удалось получить дату доставки при оформлении \n" + t);
        }

    }

    /**
     * Получить плановую дату доставки
     *
     * @return
     */
    public String getPlanningDateDelivery() {
        try {
            return Entity.properties(webDriver).getValueField("deliveryTimeFull");
        } catch (Throwable t) {
            throw new Error("Не удалось получить плановую дату доставки \n" + t);
        }
    }

    /**
     * Получить способ доставки
     *
     * @return
     */
    public String getTypeDelivery() {
        try {
            return Entity.properties(webDriver).getValueField("deliveryServiceFull");
        } catch (Throwable t) {
            throw new Error("Не удалось получить способ доставки:\n" + t);
        }
    }

    /**
     * Получить адрес
     *
     * @return
     */
    public String getAddress() {
        try {
            return Entity.properties(webDriver).getValueField("buyerAddressFull");
        } catch (Throwable t) {
            throw new Error("Не удалось получить адрес заказа:\n" + t);
        }
    }

    /**
     * Получить адрес
     *
     * @return
     */
    public String getRegion() {
        try {
            return Entity.properties(webDriver).getValueField("regionName");
        } catch (Throwable t) {
            throw new Error("Не удалось получить регион доставки заказа:\n" + t);
        }
    }

    /**
     * Получить получателя
     *
     * @return
     */
    public String getConsignee() {
        try {
            return Entity.properties(webDriver).getValueField("buyerAddressRecipientFull");
        } catch (Throwable t) {
            throw new Error("Не удалось получить получателя заказа:\n" + t);
        }
    }

    /**
     * Получить комментарий
     *
     * @return
     */
    public String getComment() {
        try {
            return Entity.properties(webDriver).getValueField("notes");
        } catch (Throwable t) {
            throw new Error("Не удалось получить комментарий по заказу:\n" + t);
        }
    }

    /**
     * нажать на ссылку Треккинг в превью заказа
     *
     * @return
     */
    public DeliveryProperties clickOnTrackingLink() {
        try {

            Tools.clickerElement(webDriver).clickElement(trackingLink);
            return this;
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на ссылку Трекинг в превью заказа\n" + e);
        }
    }

    /**
     * Нажать на ссылку трекинг и передать фокус на новую вкладку
     *
     * @return
     */
    public void clickOnTrackingLinkAndSetFocusOnNewTab() {
        Tools.tabsBrowser(webDriver).takeFocusNewTab(trackingLink);
    }

    /**
     * Видна ли кнопка с контактами курьера
     *
     * @return
     */
    public boolean isCourierDataButton() {
        try {
            Tools.waitElement(webDriver).waitClickableElementTheTime(contactInfoCourierButton, 3);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    public DeliveryProperties clickOnButtonContactCourier() {
        try {
            Tools.clickerElement(webDriver).clickElement(contactInfoCourierButton);
        } catch (Throwable t) {
            throw new Error("Не удалось нажать на кнопку с контактами курьера\n" + t);
        }
        return this;
    }

    /**
     * Получить имя курьера
     *
     * @return
     */
    public String getNameCourier() {

        try {
            return nameContactCourier.getText();
        } catch (Throwable t) {
            throw new Error("Не удалось получить имя курьера\n" + t);
        }
    }

    /**
     * Получить телефон курьера
     *
     * @return
     */
    public String getPhoneCourier() {

        try {
            return phoneContactCourier.getText().replaceAll("\\D", "");
        } catch (Throwable t) {
            throw new Error("Не удалось получить имя курьера\n" + t);
        }
    }

    /**
     * Получить номер логистического заказа
     *
     * @return
     */
    public String getDeliveryOrderNumber() {
        try {
            return Entity.properties(webDriver).getValueField("yaDeliveryOrder");
        } catch (Throwable t) {
            throw new Error("Не удалось получить номер логистического заказа \n" + t);
        }
    }

    /**
     * Получить статус логистического заказа
     *
     * @return
     */
    public String getStatusDeliveryOrder() {
        try {
            return Entity.properties(webDriver).getValueField("yaDeliveryStatus");
        } catch (Throwable t) {
            throw new Error("Не удалось получить статус логистического заказа \n" + t);
        }
    }

    /**
     * Получить количество коробок в заказе
     *
     * @return
     */
    public String getCountOfBoxes() {
        try {
            return Entity.properties(webDriver).getValueField("parcelBoxCount");
        } catch (Throwable t) {
            throw new Error("Не удалось получить Количество коробок в заказе \n" + t);
        }
    }

    /**
     * Получить вес заказа
     *
     * @return
     */
    public String getWightOfOrder() {
        try {
            return Entity.properties(webDriver).getValueField("parcelWeight");
        } catch (Throwable t) {
            throw new Error("Не удалось получить вес заказа \n" + t);
        }
    }

    /**
     * Получить дату отгрузки заказа
     *
     * @return
     */
    public String getDateOfShipment() {
        try {
            return Entity.properties(webDriver).getValueField("parcelShipmentDate");
        } catch (Throwable t) {
            throw new Error("Не удалось получить дату отгрузки заказа \n" + t);
        }
    }

    /**
     * Получить Расчетное время доставки
     *
     * @return
     */
    public String getEstimatedDeliveryTime() {
        try {
            return Entity.properties(webDriver).getValueField("parcelExpectedDeliveryDateTime");
        } catch (Throwable t) {
            throw new Error("Не удалось получить Расчетное время доставки \n" + t);
        }
    }

    /**
     * Получить ссылку на страницу логистического заказа
     *
     * @return
     */
    public String getLinkToDeliveryOrderPage() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("yaDeliveryOrder");
        } catch (Throwable t) {
            throw new Error("Не удалось получить ссылку на страницу логистического заказа \n" + t);
        }
    }

    /**
     * Выделено ли поле Плановая дата доставки
     *
     * @return
     */
    public boolean isDeliveryTimeFullFieldHighlighted() {
        WebElement webElement = Tools.findElement(webDriver).findVisibleElement(By.xpath("//*[@*[starts-with(name(.),'data-ow-test')]=\"deliveryTimeFull\"]/*[@*[starts-with(name(.),'data-ow-test')]]"));
        return webElement.getAttribute("data-ow-test-delivery-time-changed").equals("true");
    }

    /**
     * Получить значение поля Также в составе мультизаказа
     *
     * @return
     */
    public String getMultiOrderFieldText() {
        try {
            return Entity.properties(webDriver).getValueField("multiOrderLinks");
        } catch (Throwable t) {
            throw new Error("Не удалось получить текст из поля Также в составе мультизаказа:\n" + t);
        }
    }

    /**
     * Получить ссылку на заказ в составе мультизаказа
     *
     * @return
     */
    public String getMultiOrderLinks() {
        try {
            return Entity.properties(webDriver).getValueLinkToPageEntity("multiOrderLinks");
        } catch (Throwable t) {
            throw new Error("Не удалось получить ссылку на заказ в составе мультизаказа \n" + t);
        }
    }

    /**
     * нажать на ссылку мультизаказа в блоке Доставка
     *
     * @return
     */
    public DeliveryProperties clickOnMultiOrderLink() {
        try {
            Tools.clickerElement(webDriver).clickElement(multiOrderLink);
            return this;
        } catch (Throwable e) {
            throw new Error("Не получилось нажать на ссылку с мультизаказом\n" + e);
        }
    }

    /**
     * Нажать на ссылку мультизаказа и передать фокус на новую вкладку
     *
     * @return
     */
    public void clickOnMultiOrderLinkAndSetFocusOnNewTab() {
        Tools.tabsBrowser(webDriver).takeFocusNewTab(multiOrderLink);
    }
}
