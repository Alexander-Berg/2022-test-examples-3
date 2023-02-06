package ru.yandex.travel.steps;

import io.qameta.allure.Step;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.WebElement;
import ru.yandex.travel.beans.SearchParameters;
import ru.yandex.travel.beans.SletatOperators;
import ru.yandex.travel.beans.TourInformation;
import ru.yandex.travel.page.SletatSearchPage;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.qameta.htmlelements.matcher.HasTextMatcher.hasText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.hasItem;

public class SletatSteps extends WebDriverSteps {

    public SletatSearchPage onSearchPage() {
        return onPage(SletatSearchPage.class);
    }

    @Step
    public void openSearchPage() {
        onSearchPage().open("https://sletat.ru");
        onSearchPage().banner().closeButton().click();
    }

    @Step
    public void search(SearchParameters parameters, boolean specifyHotel) {
        onSearchPage().searchForm().fromCity()
                .select(parameters.getFromCity());
        onSearchPage().searchForm().toCountry()
                .select(parameters.getToCountry());
        onSearchPage().searchForm().resort()
                .select(parameters.getResort());
        if (specifyHotel) {
            onSearchPage().searchForm().hotel()
                    .select(parameters.getHotel());
        }
        onSearchPage().searchForm().minNights()
                .select(parameters.getMinNights() + "");
        onSearchPage().searchForm().maxNights()
                .select(parameters.getMaxNights() + "");
        onSearchPage().searchForm().adults()
                .select(parameters.getAdults() + "");
        if (parameters.getChilds().size() > 0) {
            onSearchPage().searchForm().childs().select(parameters.getChilds().size() + "");
            for (int i = 0; i < parameters.getChilds().size(); i++) {
                onSearchPage().searchForm().childAge().get(i).sendKeys(parameters.getChilds().get(i));
            }
        }
        onSearchPage().searchForm().date()
                .select(parameters.getFromDate(), parameters.getToDate());
        onSearchPage().searchForm().flightInfoCheckboxes()
                .forEach(WebElement::click);
        onSearchPage().searchForm().searchButton()
                .click();
        onSearchPage().searchStatus().percents()
                .waitUntil("Ждем ста процентов", hasText(containsString("100%")), 60);
        try {
            onSearchPage().searchStatus().button().click();
        }
        catch (Exception ignored) {
        }
    }

    @Step
    public Optional<TourInformation> findCheapestTourInformation() throws IOException {
        List<TourInformation> tours = onSearchPage().operatorsList().stream()
                .map(e -> new TourInformation().setPrice(toPrice(e.price().getText())).setOperator(e.name().getText()))
                .collect(Collectors.toList());
        Optional<TourInformation> tour = tours.stream()
                .filter(e -> hasItem(e.getOperator()).matches(SletatOperators.getOperators().values()))
                .sorted(Comparator.comparing(TourInformation::getPrice)).findFirst();
        if (tour.isPresent()) {
            addCurrentUrl(tour.get()).saveScreenshot(tour.get());
        }
        return tour;
    }

    private static int toPrice(String price) {
        return Integer.parseInt(price.replace("Р", "").replace(" ", "").replace(" ", ""));
    }

}
