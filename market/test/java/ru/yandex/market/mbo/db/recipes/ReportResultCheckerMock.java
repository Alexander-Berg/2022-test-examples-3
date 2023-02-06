package ru.yandex.market.mbo.db.recipes;

import ru.yandex.market.mbo.gwt.models.recipe.BiggestRussianCities;
import ru.yandex.market.mbo.gwt.models.recipe.Recipe;

public class ReportResultCheckerMock extends ReportResultChecker {

    public static final long TOTAL_OFFERS = 76L;
    public static final long TOTAL_MODELS = 2L;

    private ReportResponse response;

    public ReportResultCheckerMock() {
        super(null, null);
    }

    @Override
    protected ReportResponse getReportResponse(BiggestRussianCities city, Recipe recipe, CheckType checkType) {
        if (recipe.getId() == 1) {
            response = new ReportResponse(0L, 0L, 0L);
        } else {
            response = new ReportResponse(TOTAL_OFFERS + TOTAL_MODELS, TOTAL_OFFERS, TOTAL_MODELS);
        }
        return response;
    }
}
