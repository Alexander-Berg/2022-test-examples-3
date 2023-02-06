package ru.yandex.travel;

import com.carlosbecker.guice.GuiceModules;
import com.google.inject.Inject;
import freemarker.template.TemplateException;
import io.qameta.allure.Feature;
import lombok.extern.log4j.Log4j;
import org.aeonbits.owner.ConfigFactory;
import org.junit.*;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.model.Statement;
import ru.yandex.travel.beans.SearchParameters;
import ru.yandex.travel.beans.Summary;
import ru.yandex.travel.beans.TourInformation;
import ru.yandex.travel.junit.GuiceParametersRunnerFactory;
import ru.yandex.travel.module.TravelWebModule;
import ru.yandex.travel.steps.SletatSteps;
import ru.yandex.travel.steps.TravelSteps;

import java.io.IOException;
import java.util.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assume.assumeThat;

@Log4j
@Feature("Конкуренты")
@RunWith(Parameterized.class)
@GuiceModules(TravelWebModule.class)
@Parameterized.UseParametersRunnerFactory(GuiceParametersRunnerFactory.class)
public class SletatDestinationPriceComparisonTest {

    private static List<Summary> result = new ArrayList();


    @Inject
    private TravelSteps travelSteps;

    @Inject
    private SletatSteps sletatSteps;

    @Rule
    @Inject
    public RuleChain defaultRules;

    @Rule
    public TestRule testRule = (statement, description) -> new Statement() {
        @Override
        public void evaluate() throws Throwable {
            try {
                statement.evaluate();
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }
    };

    @Before
    public void SetCookies() {
        travelSteps.SetSortCookie();
    }

    @Parameterized.Parameter(0)
    public Map<String, String> map;

    @Parameterized.Parameter(1)
    public String travelSearchUrl;

    @Parameterized.Parameter(2)
    public SearchParameters sletatSearchParameters;

    @Parameterized.Parameter(3)
    public String country;

    @Parameterized.Parameter(4)
    public String destination;

    @Parameterized.Parameters(name = "Country -> {3}, Destination -> {4}")
    public static Collection<Object[]> getData() throws Exception {
        SletatPriceComparisonConfig config = ConfigFactory.create(SletatPriceComparisonConfig.class, System.getProperties());

        List<Object[]> collection = new ArrayList<>();
        TestUtils.readSearches("pricechecking/tours.destinations.random.1000.searches.tsv")
                .stream().filter(SletatDestinationPriceComparisonTest::isValid).forEach(map -> {
                    SearchParameters sletatParameters = TestUtils.convertSearchParameters(map, false);
                    String url = map.get("searchUrl");
                    collection.add(new Object[]{map, url, sletatParameters, map.get("to_country"), map.get("to")});
                });
        Collections.shuffle(collection);
        return collection.stream().limit(config.countForDestinations()).collect(toList());
    }


    @Test
    public void priceTest() throws Exception {
        log.info(format(" START destination price test «%s, %s» (%s)", country, destination, travelSearchUrl));
//        log.info(travelSearchUrl);

        TourInformation travelTour = travelSteps.findCheapestDestinationTourInformation(travelSearchUrl)
            .orElse(new TourInformation().setOperator("не найден").setPrice(0));
        assumeThat(travelTour.getPrice(), greaterThan(0));
        log.info(format(" got travel price: %d", travelTour.getPrice()));

        sletatSteps.openSearchPage();
        sletatSteps.search(sletatSearchParameters, false);
        TourInformation sletatTour = sletatSteps.findCheapestTourInformation()
                .orElse(new TourInformation().setOperator("не найден").setPrice(0));
        assumeThat(sletatTour.getPrice(), greaterThan(0));
        log.info(format(" got sletat price: %d", sletatTour.getPrice()));

        result.add(new Summary()
                .setInformation(sletatSearchParameters)
                .setYaTour(travelTour)
                .setSlTour(sletatTour)
                .setSuccess(TestUtils.successRate(travelTour.getPrice(), sletatTour.getPrice())));
        log.info(format("destination price test done (%s) ", travelSearchUrl));
    }


    @AfterClass
    public static void generateReport() throws IOException, TemplateException {
        TestUtils.generateReport(result, "Результаты сравнения цен (направления)");
    }

    private static boolean isValid(Map map) {
        return !map.get("to_country").equals(map.get("to"));
    }

}
