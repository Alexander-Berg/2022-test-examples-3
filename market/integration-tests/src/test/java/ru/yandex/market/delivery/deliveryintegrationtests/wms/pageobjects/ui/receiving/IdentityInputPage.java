package ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.receiving;

import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.FindBy;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.RandomUtil;
import ru.yandex.market.delivery.deliveryintegrationtests.tool.Retrier;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.AnomalyType;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.Item;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.RegistryItemWrapper;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.AbstractPage;
import ru.yandex.market.logistic.api.model.common.PartialIdType;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selectors.byXpath;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlMatches;
import static ru.yandex.market.logistic.api.model.common.PartialIdType.IMEI;

@Slf4j
public class IdentityInputPage extends AbstractPage {

    private static final Predicate<Item> HAS_INVALID_CIS_ANOMALY = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_REQUIRED_CIS);

    private static final Predicate<Item> HAS_INVALID_OPTIONAL_CIS_ANOMALY = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_OPTIONAL_CIS);

    private static final Predicate<Item> HAS_INVALID_IMEI_ANOMALY = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_IMEI);

    private static final Predicate<Item> HAS_INVALID_SN_ANOMALY = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_SERIAL_NUMBER);

    private static final Predicate<RegistryItemWrapper> HAS_INVALID_CIS = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_REQUIRED_CIS);

    private static final Predicate<RegistryItemWrapper> HAS_INVALID_OPTIONAL_CIS = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_OPTIONAL_CIS);

    private static final Predicate<RegistryItemWrapper> HAS_INVALID_IMEI = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_IMEI);

    private static final Predicate<RegistryItemWrapper> HAS_INVALID_SN = item ->
            item.getAnomalyTypes().contains(AnomalyType.INCORRECT_SERIAL_NUMBER);

    private static final int RECEIVE_ITEMS_TIMEOUT = 11;


    @FindBy(xpath = "//div[@data-e2e='text-field']//input")
    private SelenideElement input;
    @FindBy(xpath = "//span[text() = 'Нет Честного ЗНАКА']")
    private SelenideElement noCisButton;
    @FindBy(xpath = "//span[text() = 'Нет IMEI']")
    private SelenideElement noIMEIButton;
    @FindBy(xpath = "//span[text() = 'Нет SN']")
    private SelenideElement noSNButton;

    public IdentityInputPage(WebDriver driver) {
        super(driver);
        wait.until(urlMatches("identityInputPage$"));
    }

    @Step("Вводим идентификаторы")
    public void enterIdentities(Item item) {
        if (item.getCheckCis() > 0) {
            String cis = getIdentityValue(item, "CIS", () -> null);
            enterCis(cis);
            boolean returnNow = performInCaseOf(item, HAS_INVALID_CIS_ANOMALY, item1 -> clickNoCisButton());
            if (returnNow) {
                return;
            }
            performInCaseOf(item, HAS_INVALID_OPTIONAL_CIS_ANOMALY.or(s -> cis == null), item1 -> clickNoCisButton());
        }
        for (int i = 1; i <= item.getCheckImei(); i++) {
            String imei = getIdentityValue(item, "IMEI", RandomUtil::generateImei);
            enterImei(imei, i);
            performInCaseOf(item, HAS_INVALID_IMEI_ANOMALY, item1 -> clickNoIMEIButton());
        }
        for (int i = 1; i <= item.getCheckSn(); i++) {
            String sn = getIdentityValue(item, "SN", () -> RandomStringUtils.randomAlphanumeric(10));
            //иногда ручка receive-items таймаутит при вводе sn, во входящем потоке это не будут чинить, так что ретраим в тестах
            Retrier.retry(() -> {
                enterSn(sn);
                Assertions.assertFalse(notificationDialog.isPresentWithTitleCustomTimeout(
                        "INTERNAL_SERVER_ERROR", RECEIVE_ITEMS_TIMEOUT),
                        "При вводе SN произошла ошибка сервера");
            }, Retrier.RETRIES_TINY, Retrier.TIMEOUT_TINY, TimeUnit.SECONDS);
            performInCaseOf(item, HAS_INVALID_SN_ANOMALY, item1 -> clickNoSNButton());
        }
    }

    @Step("Вводим идентификаторы")
    public void enterIdentities(RegistryItemWrapper item) {
        if (item.isCheckCis()) {
            String cis = item.getIdentityValue(PartialIdType.CIS);
            enterCis(cis);
            boolean returnNow = performInCaseOf(item, HAS_INVALID_CIS, item1 -> clickNoCisButton());
            if (returnNow) {
                return;
            }
            performInCaseOf(item, HAS_INVALID_OPTIONAL_CIS.or(s -> cis == null), item1 -> clickNoCisButton());
        }
        List<String> imeis = item.getIdentityValues(IMEI);
        for (int i = 0; i < imeis.size(); i++) {
            String imei = Optional.ofNullable(imeis.get(i)).orElse(RandomUtil.generateImei());
            enterImei(imei, i);
            performInCaseOf(item, HAS_INVALID_IMEI, item1 -> clickNoIMEIButton());
        }
        List<String> serialNumbers = item.getIdentityValues(PartialIdType.SERIAL_NUMBER);
        for (int i = 0; i < serialNumbers.size(); i++) {
            String sn = Optional.ofNullable(serialNumbers.get(i))
                    .orElse(RandomStringUtils.randomAlphanumeric(10));
            enterSn(sn);
            performInCaseOf(item, HAS_INVALID_SN, item1 -> clickNoSNButton());
        }
    }

    @Step("Вводим IMEI")
    private void enterImei(String value, int number) {
        final String expression;
        if (number == 1) {
            expression = "//span[contains(.,'IMEI')]";
        } else {
            expression = String.format("//span[contains(.,'IMEI  %1$s')]", number);
        }
        $(byXpath(expression)).shouldBe(visible);
        input.sendKeys(value);
        input.pressEnter();
    }

    @Step("Вводим SN")
    private void enterSn(String value) {
        $(byXpath("//span[contains(.,'S/N')]")).shouldBe(visible);
        input.sendKeys(value);
        input.pressEnter();
    }

    @Step("Вводим КИЗ")
    private void enterCis(String value) {
        if (value == null) {
            return;
        }
        input.sendKeys(value);
        input.pressEnter();
    }

    @Step("Нажимаем на кнопку 'Нет Честного Знака'")
    private void clickNoCisButton() {
        noCisButton.click();
    }

    @Step("Нажимаем на кнопку 'Нет IMEI'")
    private void clickNoIMEIButton() {
        noIMEIButton.click();
    }

    @Step("Нажимаем на кнопку 'Нет SN'")
    private void clickNoSNButton() {
        noSNButton.click();
    }

    private <T> boolean performInCaseOf(T item, Predicate<T> condition, Consumer<T> action) {
        boolean actionPerformed = false;
        Optional<T> any = Optional.of(item).stream()
                .filter(condition)
                .findAny();
        if (any.isPresent()) {
            actionPerformed = true;
        }
        any.ifPresent(action);
        return actionPerformed;
    }

    private String getIdentityValue(Item item, String identityType, Supplier<String> valueProducer) {
        return Optional.ofNullable(item.getInstances())
                .map(idt -> item.getInstances().get(identityType)).orElseGet(valueProducer);
    }
}
