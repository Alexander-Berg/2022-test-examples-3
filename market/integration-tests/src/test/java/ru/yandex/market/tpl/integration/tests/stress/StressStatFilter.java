package ru.yandex.market.tpl.integration.tests.stress;

import io.restassured.filter.Filter;
import io.restassured.filter.FilterContext;
import io.restassured.response.Response;
import io.restassured.specification.FilterableRequestSpecification;
import io.restassured.specification.FilterableResponseSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StressStatFilter implements Filter {
    private final StressStat stressStat;

    @Override
    public Response filter(FilterableRequestSpecification requestSpec, FilterableResponseSpecification responseSpec,
                           FilterContext ctx) {
        RequestStat requestStat =
                new RequestStat(requestSpec.getMethod() + " " + deleteCustomIds(requestSpec.getDerivedPath()));
        stressStat.add(requestStat);
        Response response = ctx.next(requestSpec, responseSpec);
        requestStat.stop();
        requestStat.setDurationMs(response.time());
        requestStat.setHttpStatus(response.getStatusCode());
        return response;
    }

    private String deleteCustomIds(String s) {
        return s.replaceAll("/\\d+/", "/{}/").replaceAll("/\\d+$", "/{}");
    }
}
