package ru.yandex.market.global.checkout.domain.promo;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ru.yandex.market.global.checkout.util.RandomDataGenerator;

import static ru.yandex.market.global.common.util.StringFormatter.sf;

@Disabled
public class PromoGenerator {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(RandomDataGenerator.class).build();
    private static final String LETTERS = "ABCDEFGHJKLMNOPQRSTUVWXYZ";
    private static final String TEMPLATE = "INSERT INTO checkout.promo (name, description, type, args, " +
            "access_type, limited_usages_count, application_type, valid_from, valid_till, state, tags, created_at, " +
            "modified_at, communication_types, communication_args) VALUES ('{}', 'Discount 25 ILS " +
            "for 1 use', 'FIXED_DISCOUNT_NO_ADULT', '{\"type\": \"FIXED_DISCOUNT_NO_ADULT\", " +
            "\"budget\": 2500, \"discount\": 2500, \"minTotalItemsCost\": 5000}', 'ALL_LIMITED', 1, 'PROMOCODE', " +
            "'2022-04-10 00:00:00', '2022-05-10 00:00:00', '{\"budgetUsed\": 0}', null, " +
            "'2022-04-08 00:00:00', '2022-04-08 00:00:00', null, null);";

    @Test
    public void generatePromoCodes() {
        String prefix = "PASSOVER";
        for (int i = 0; i < 1000; i++) {
            String code = prefix
                    + LETTERS.charAt(RANDOM.nextInt(LETTERS.length()))
                    + LETTERS.charAt(RANDOM.nextInt(LETTERS.length()))
                    + LETTERS.charAt(RANDOM.nextInt(LETTERS.length()))
                    + LETTERS.charAt(RANDOM.nextInt(LETTERS.length()));

            System.out.println(sf(TEMPLATE, code));
        }
    }
}
