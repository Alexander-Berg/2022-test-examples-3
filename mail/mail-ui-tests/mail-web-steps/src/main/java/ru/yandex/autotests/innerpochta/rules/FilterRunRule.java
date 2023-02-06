package ru.yandex.autotests.innerpochta.rules;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.yandex.autotests.innerpochta.ignores.FilterIgnoreStatement;
import ru.yandex.autotests.innerpochta.ignores.IgnoreStatement;
import ru.yandex.qatools.allure.annotations.TestCaseId;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static ru.yandex.autotests.innerpochta.util.props.TestProperties.testProperties;
import static ru.yandex.autotests.innerpochta.util.props.UrlProps.urlProps;
import static ru.yandex.autotests.innerpochta.util.props.YandexServicesProperties.yandexServicesProps;

/**
 * Данная рула фильтрует (игнорирует) тесты, которые не удовлетворяют фильтру, если он указан.
 *
 * @author pavponn
 */
public class FilterRunRule extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterRunRule.class);

    private String filter;
    private Set<Integer> filteredTestIds;

    private static FilterRunRule ruleInstance;

    public static FilterRunRule filterRunRule() {
        if (ruleInstance == null) {
            ruleInstance = new FilterRunRule();
            ruleInstance.setFilterSet();
        }
        return ruleInstance;
    }

    private FilterRunRule() {
        filter = testProperties().getFilterExpression();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (base instanceof IgnoreStatement) {
            return base;
        }

        if (filter == null || filter.equals("")) {
            LOGGER.info("No filter provided");
            return base;
        }
        LOGGER.info("Filter: {}", filter);

        if (hasTestCaseIdAnnotation(description) && !satisfiesFilter(getTestCaseId(description))) {
            LOGGER.info("Test doesn't suite filter, ignoring...");
            return new FilterIgnoreStatement(filter);
        }
        return base;
    }

    private boolean satisfiesFilter(String testCaseId) {
        try {
            return filteredTestIds.contains(Integer.parseInt(testCaseId));
        } catch (NumberFormatException e) {
            LOGGER.info("NOT A VALID TEST CASE ID: {}", testCaseId);
        }
        return false;
    }

    private void setFilterSet() {
        if (filter != null && !filter.equals("")) {
            filteredTestIds = new HashSet<>(getTestIds());
        } else {
            filteredTestIds = new HashSet<>();
        }
    }

    private List<Integer> getTestIds() {
        String requestPath = format(
            "https://testpalm-api.yandex-team.ru/testcases/%s?include=id&expression=%s",
            getProjectId(),
            filter
        );
        LOGGER.info("REQUEST: {}", requestPath);
        RequestSpecification request = RestAssured.given()
            .urlEncodingEnabled(false)
            .header(new Header("Authorization", "OAuth " + yandexServicesProps().getTestpalmToken()));
        Response response;
        try {
            response = request.get(requestPath);
            LOGGER.info("RESPONSE: {}", response.getStatusLine());
        } catch (Exception e) {
            LOGGER.info("ERROR! {} ", e.getMessage());
            return new ArrayList<>();
        }
        Assert.assertEquals("Запрос в TestPalm завершился с ошибкой", 200, response.getStatusCode());
        return retrieveIdsFromJson(response.getBody().asString());
    }

    private List<Integer> retrieveIdsFromJson(String jsonString) {
        JSONArray casesJsonArray = new JSONArray(jsonString);
        List<Integer> ids = new ArrayList<>(casesJsonArray.length());
        for(int i = 0; i < casesJsonArray.length(); i++) {
            ids.add(casesJsonArray.getJSONObject(i).getInt("id"));
        }
        return ids;
    }

    private String getProjectId() {
        if (urlProps().getProject().equals("liza")) {
            return "mail-liza";
        } else if (urlProps().getProject().equals("touch")) {
            return "mail-touch";
        } else {
            return "cal";
        }
    }

    private static String getTestCaseId(Description description) {
        return hasTestCaseIdAnnotation(description) ? description.getAnnotation(TestCaseId.class).value() : "";
    }

    private static boolean hasTestCaseIdAnnotation(Description description) {
        return description.getAnnotation(TestCaseId.class) != null;
    }
}
