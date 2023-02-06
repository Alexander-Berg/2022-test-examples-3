package ru.yandex.market.api.partner.api.resources;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.AntPathMatcher;

import ru.yandex.market.api.partner.metrics.PageMatcherService;

/**
 * Проверяет наличие ресурса в базе, для новых ручек надо добавить ресурс, группу ресурсов и определить лимиты
 * ресурс добавлять в api_resources.csv
 * группу добавлять в api_resources_group.csv
 */
class ApiResourcesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ApiResourcesTest.class);
    private static final String API_RESOURCES_CSV = "shops_web/api_resources/api_resources.csv";

    private static HashSet<String> buildRouteSet() {
        HashSet<String> result = new HashSet<>();
        try (BufferedReader br = new BufferedReader(
                new FileReader(PageMatcherService.class.getResource("routes.csv.txt").getFile()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String metricName = parts[0];

                String metricUrlPattern = parts[1];
                if (result.contains(metricUrlPattern)) {
                    LOG.error("Url pattern is not unique: " + metricUrlPattern);
                    Assertions.fail("Url pattern is not unique: " + metricUrlPattern);
                }
                if (!metricName.endsWith("__v1") && !metricName.endsWith("__v2")) {
                    result.add(metricUrlPattern);
                }
            }
        } catch (IOException e) {
            LOG.error("Unable to load route map");
            Assertions.fail("Unable to load route map");
        }
        return result;
    }

    private static HashSet<String> buildResourceSet() {
        HashSet<String> result = new HashSet<>();


        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(API_RESOURCES_CSV).getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                String method = parts[1];
                String pattern = parts[2];
                result.add(method + ":" + pattern);
            }
        } catch (IOException e) {
            LOG.error("Unable to load resources");
            Assertions.fail("Unable to load resources");
        }
        return result;
    }

    @Test
    void checkRoutesResourceExists() {
        Set<String> routeSet = buildRouteSet();
        Set<String> resourceApi = buildResourceSet();

        boolean missedResource = false;

        for (String route : routeSet) {
            boolean isMatched = false;
            AntPathMatcher antPathMatcher = new AntPathMatcher();
            for (String resource : resourceApi) {
                if (antPathMatcher.match(resource, route)) {
                    isMatched = true;
                    break;
                }

            }
            if (!isMatched) {
                LOG.error("Добавьте в api_resources.csv ресурс для ручки: " + route);
                missedResource = true;

            }

        }
        if (missedResource) {
            Assertions.fail("Нужно добавить новые ручки в api_resources.csv");
        }
    }
}
