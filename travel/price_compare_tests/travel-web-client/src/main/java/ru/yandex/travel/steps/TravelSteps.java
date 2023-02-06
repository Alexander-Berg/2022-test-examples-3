package ru.yandex.travel.steps;

import io.qameta.allure.Step;
import io.qameta.htmlelements.WebPage;
import io.qameta.htmlelements.exception.WaitUntilException;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.StaleElementReferenceException;
import ru.yandex.travel.beans.TourInformation;
import ru.yandex.travel.element.DestinationTourRow;
import ru.yandex.travel.element.HotelTourRow;
import ru.yandex.travel.page.TravelDestinationPage;
import ru.yandex.travel.page.TravelHotelPage;

import java.io.IOException;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.not;
import static ru.yandex.qatools.htmlelements.matchers.WebElementMatchers.exists;

public class TravelSteps extends WebDriverSteps {

    public void SetSortCookie() {
        onHotelsPage().open("https://travel.yandex.ru");
        getDriver().manage().addCookie(new Cookie("sort", "price_asc"));
    }

    private static TourInformation convertForHotel(HotelTourRow row) {
        return new TourInformation()
                .setOperator(row.operator().getText())
                .setPrice(toPrice(row.price().getText()));
    }

    public TravelHotelPage onHotelsPage() {
        return onPage(TravelHotelPage.class);
    }

    @Step
    public Optional<TourInformation> findCheapestHotelTourInformation(String travelSearchUrl) throws IOException {
        for (int i = 0; i < 3; i++) {
            onHotelsPage().open(travelSearchUrl);
            try {
                onHotelsPage().toursSection().inProgress().waitUntil("waiting load", not(exists()), 60);
            } catch (WaitUntilException ignored) {
                continue;
            }
            break;
        }
        Optional<TourInformation> tour = onHotelsPage().toursSection().toursList()
                .stream().map(TravelSteps::convertForHotel)
                .sorted(Comparator.comparingInt(TourInformation::getPrice)).findFirst();
        if (tour.isPresent()) {
            addCurrentUrl(tour.get()).saveScreenshot(tour.get());
        }
        return tour;
    }

    public TravelDestinationPage onDestinationsPage() {
        return onPage(TravelDestinationPage.class);
    }

    private String extractOperator(String url) throws IOException {
        return findCheapestHotelTourInformation(url).get().getOperator();
    }

    private static TourInformation convertForDestination(DestinationTourRow row) {
        return new TourInformation()
                .setOperator(row.title().getAttribute("href"))
                .setPrice(toPrice(row.price().getText()));
    }

    @Step
    public Optional<TourInformation> findCheapestDestinationTourInformation(String travelSearchUrl) throws IOException {
        for (int i = 0; i < 3; i++) {
            onDestinationsPage().open(travelSearchUrl);
            try {
                onDestinationsPage().toursSection().inProgress().waitUntil("waiting load", not(exists()), 60);
            } catch (WaitUntilException ignored) {
                continue;
            }
            break;
        }
        Optional<TourInformation> tour = onDestinationsPage().toursSection().toursList()
                .stream().map(e -> convertForDestination(e))
                .sorted(Comparator.comparingInt(TourInformation::getPrice)).findFirst();
        if (tour.isPresent()) {
            addCurrentUrl(tour.get()).saveScreenshot(tour.get());
            tour.get().setOperator(extractOperator(tour.get().getOperator()));
        }
        return tour;
    }

    private static int toPrice(String price) {
        return Integer.parseInt(price.replace("Р", "").replace(" ", "").replace(" ", ""));
    }

}
