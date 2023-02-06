package ru.yandex.market.core.partner;

import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.core.application.PartnerApplicationStatus;

public final class PartnerApplicationTestHelper {

    private PartnerApplicationTestHelper() {
    }

    public static void setApplicationStatus(
            JdbcTemplate jdbcTemplate,
            long requestId,
            PartnerApplicationStatus status
    ) {
        jdbcTemplate.update(
                "update shops_web.partner_app set status = ? where request_id = ?",
                status.getName(),
                requestId
        );
    }
}
