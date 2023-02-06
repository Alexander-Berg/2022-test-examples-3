package ru.yandex.market.delivery.deliveryintegrationtests.wms.step.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.LocationKey;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.dto.ParcelId;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.MenuPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances.BalancesOfUitPage;
import ru.yandex.market.delivery.deliveryintegrationtests.wms.pageobjects.ui.balances.BalancesPage;

@Slf4j
public class Balances {
    private WebDriver driver;
    private MenuPage menuPage;
    private BalancesPage balancesPage;
    private BalancesOfUitPage balancesOfUitPage;

    public Balances(WebDriver driver) {
        this.driver = driver;
        this.menuPage = new MenuPage(driver);
        this.balancesPage = new BalancesPage(driver);
        this.balancesOfUitPage = new BalancesOfUitPage(driver);
    }

    @Step("Находим партию айтемов по НЗН - {nzn}")
    public String findLotByNzn(String nzn) {
        menuPage.inputBalancesPath()
                .searchById(nzn);

        return balancesPage.getLotFromFilterResults();
    }

    @Step("Находим УИТ по НЗН - {nzn}")
    public List<String> findUitByNzn(String nzn) {
        menuPage.inputBalancesPath()
                .searchById(nzn);

        return new ArrayList<String>(Arrays.asList(balancesPage.getUITFromFilterResults().split(", ")));
    }

    @Step("Находим ячейку и НЗН по УИТу - {uit}")
    public LocationKey findLocationKeyByUit(String uit) {
        menuPage.inputBalancesPath()
                .searchByUit(uit);

        return balancesPage.getLocationKeyFromFilterResults();
    }

    @Step("Ищем отсутствующий УИТ {uit}")
    public void findDeletedUit(String uit) {
        menuPage.inputBalancesPath()
                .searchByUit(uit);

        Assertions.assertTrue(new BalancesPage(driver).checkUitNotFound(),
                "Нет ошибки что УИТ не найден");
    }

    @Step("Ищем УИТы по ячейке {loc}")
    public List<String> getUitsByLocStrict(String loc, int expectedNumberOfItems) {
        BalancesPage page = menuPage.inputBalancesPath().searchByLoc(loc);
        List<String> result = Arrays.asList(page.getUITFromFilterResults().split(", "));
        page.clickBack();
        Assertions.assertEquals(expectedNumberOfItems, result.size(),
                String.format("Неожиданное число УИТов в ячейке %s. Ожидали %d шт, а получили %d шт",
                        loc, expectedNumberOfItems, result.size()));
        return result;
    }



    @Step("Узнаем количество доступного товара по ячейке и supplierSku")
    public int getAvailableItemsNumber(String cellId, String supplierSku) {
        return menuPage.inputBalanceAdministrationPath()
                .searchBySku(supplierSku)
                .searchByCell(cellId)
                .getItemCountFromFilterResults();
    }

    @Step("Проверяем наличие товара")
    public Boolean isEnoughItemsAvailable(String cellId, String supplierSku, int expectedAmount) {

        int actualAmount = getAvailableItemsNumber(cellId, supplierSku);

        log.info("actualAmount: {}, expectedAmount: {}", actualAmount, expectedAmount);
        return actualAmount >= expectedAmount;
    }

    @Step("Находим серийники айтемов по НЗН и Артикулу поставщика")
    public List<String> findByNznAndSupSku(String sup, String nzn) {
        menuPage.inputBalancesOfUitPath()
                .inputNzn(nzn)
                .inputSupplierSku(sup);

        return balancesOfUitPage.getSerialsFromFilterResults();
    }

    @Step("Находим серийники заданного количества айтемов по НЗН и Артикулу поставщика")
    public List<String> findIdCountByNznAndSupSku(String supplierSku, String nzn, int count) {
        menuPage.inputBalancesOfUitPath()
                .inputNzn(nzn)
                .inputSupplierSku(supplierSku);

        return balancesOfUitPage.getSerialsFromFilterResults(count);
    }

    @Step("Получаем НЗН айтемов по серийнику")
    public List<ParcelId> findNznBySerial(List<String> serials) {
        log.info("Getting item nzns by serials");

        String searchString = String.join(", ", serials);

        menuPage.inputBalancesOfUitPath()
                .inputSerialNumber(searchString);

        return balancesOfUitPage.getNznsFromFilterResults()
                .stream()
                .map(ParcelId::new)
                .collect(Collectors.toList());
    }

    @Step("Получаем НЗН айтемов по локации")
    public List<ParcelId> findNznByLoc(String loc) {
        log.info("Getting item nzn by loc");

        menuPage.inputBalancesOfUitPath()
                .inputLoc(loc);

        return balancesOfUitPage.getNznsFromFilterResults()
                .stream()
                .map(ParcelId::new)
                .collect(Collectors.toList());
    }

    @Step("Получаем список уитов по ROV и номеру ячейки")
    public List<String> getUitListBySkuAndLoc(String sku, String loc) {
        log.info("Getting uit list by sku {} and loc {}", sku, loc);

        return menuPage
                .inputBalancesOfUitPath()
                .inputSku(sku)
                .inputLoc(loc)
                .getSerialsFromFilterResults();
    }

    @Step("Проверяем что НЗН - {nzn} пустая")
    public void checkNznIsEmpty(String nzn) {
        menuPage.inputBalancesPath()
                .searchById(nzn);

        Assertions.assertTrue(balancesPage.checkNoResultsFound());
    }
}
