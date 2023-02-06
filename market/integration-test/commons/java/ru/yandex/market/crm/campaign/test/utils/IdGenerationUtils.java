package ru.yandex.market.crm.campaign.test.utils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author vtarasoff
 * @since 21.09.2020
 */
public class IdGenerationUtils {
    private IdGenerationUtils() {
        // Utility class
    }

    public static String dateTimeId() {
        return LocalDateTime.now().toString().replace(".", "_");
    }

    public static String randomId() {
        return UUID.randomUUID().toString();
    }
}
