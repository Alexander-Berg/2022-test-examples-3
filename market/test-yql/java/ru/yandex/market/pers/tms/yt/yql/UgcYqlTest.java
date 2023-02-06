package ru.yandex.market.pers.tms.yt.yql;

import org.junit.jupiter.api.Test;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 22.10.2021
 */
public class UgcYqlTest extends AbstractPersYqlTest {
    @Test
    public void testSiteRatingSimple() {
        // skip this tests in recipes - fails with connection issues
        // can not connect to [[::1]:18281, 127.0.0.1:18281]
        if (isUseRecipe()) {
            return;
        }
        runTest(
            loadScript("/yql/tables/site_rating.sql"),
            "/ugc/site_rating_expected.json",
            "/ugc/site_rating.mock"
        );
    }
}
