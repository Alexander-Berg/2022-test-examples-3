package ru.yandex.market.partner.mvc.controller.program.dropshipbyseller;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.matchers.HttpClientErrorMatcher;
import ru.yandex.market.mbi.util.MbiMatchers;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

public class DropshipBySellerProgramControllerFunctionalTest extends FunctionalTest {

    @Test
    @DbUnitDataSet(before = "db/dropshipbysellerProgram.before.csv")
    void invalidPartnerProgramType() {
        String url = String.format("%s/campaigns/programs/dropship_by_seller?_user_id=12345&id=%d", baseUrl, 1001L);

        HttpClientErrorException exception = Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> FunctionalTestHelper.get(url)
        );

        String expectedErrors = "[" +
                "  {" +
                "    \"code\": \"BAD_PARAM\"," +
                "    \"details\": {" +
                "      \"fields\": [\"program\", \"partnerId\"]," +
                "      \"subcode\": \"INVALID\"" +
                "    }," +
                "    \"message\": \"Invalid program type DROPSHIP_BY_SELLER for partner 1\"" +
                "  }" +
                "]";

        MatcherAssert.assertThat(
                exception,
                Matchers.allOf(
                        HttpClientErrorMatcher.hasErrorCode(HttpStatus.BAD_REQUEST),
                        HttpClientErrorMatcher.bodyMatches(
                                MbiMatchers.jsonPropertyEquals("errors", expectedErrors)
                        )
                )
        );
    }
}
