package ru.yandex.market.checkout.checkouter.promo.util;

import java.util.Iterator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.hamcrest.Matcher;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;

import static java.util.Comparator.comparingLong;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * @author Nikolai Iusiumbeli
 * date: 15/08/2017
 */
public final class Utils {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private Utils() {
    }

    public static void addCartErrorsChecks(ResultActions resultActions, String errorType, String errorCode,
                                           String errorSeverity, Matcher<String> userMessage) throws Exception {
        if (errorType == null) {
            resultActions
                    .andExpect(jsonPath("$.validationErrors[*].type").doesNotExist())
                    .andExpect(jsonPath("$.validationErrors[*].code").doesNotExist())
                    .andExpect(jsonPath("$.validationErrors[*].severity").doesNotExist())
                    .andExpect(jsonPath("$.carts[*].validationErrors[*].type").doesNotExist())
                    .andExpect(jsonPath("$.carts[*].validationErrors[*].code").doesNotExist())
                    .andExpect(jsonPath("$.carts[*].validationErrors[*].severity").doesNotExist());
        } else {
            resultActions
                    .andExpect(jsonPath("$.validationErrors[*].type").value(hasItem(errorType)))
                    .andExpect(jsonPath("$.validationErrors[*].code").value(hasItem(errorCode)))
                    .andExpect(jsonPath("$.validationErrors[*].severity").value(hasItem(errorSeverity)))
                    .andExpect(jsonPath("$.carts[*].validationErrors[*].type").value(hasItem(errorType)))
                    .andExpect(jsonPath("$.carts[*].validationErrors[*].code").value(hasItem(errorCode)))
                    .andExpect(jsonPath("$.carts[*].validationErrors[*].severity").value(hasItem(errorSeverity)));
        }

        if (userMessage == null) {
            resultActions
                    .andExpect(jsonPath("$.validationErrors[*].userMessage").doesNotExist());
        } else {
            resultActions
                    .andExpect(jsonPath("$.validationErrors[*].userMessage").value(hasItem(userMessage)));
        }
    }

    public static void checkCoins(List<UserCoinResponse> actual, List<UserCoinResponse> expected) {
        assertThat(actual, hasSize(expected.size()));

        Iterator<UserCoinResponse> actualIterator =
                actual.stream().sorted(comparingLong(UserCoinResponse::getId)).iterator();
        Iterator<UserCoinResponse> expectedIterator = expected.stream()
                .sorted(comparingLong(UserCoinResponse::getId)).iterator();

        while (expectedIterator.hasNext()) {
            String expectedJson = GSON.toJson(expectedIterator.next());
            String actualJson = GSON.toJson(actualIterator.next());
            assertThat(actualJson, equalTo(expectedJson));
        }
    }
}
