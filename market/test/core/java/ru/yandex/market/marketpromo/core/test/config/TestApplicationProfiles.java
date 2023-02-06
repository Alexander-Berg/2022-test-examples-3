package ru.yandex.market.marketpromo.core.test.config;

import ru.yandex.market.marketpromo.core.config.ApplicationProfile;

public interface TestApplicationProfiles extends ApplicationProfile {
    String UNIT_TEST = "unit-test";
    String YT_ACTIVE = "yt-active";
    String LOGBROKER_ACTIVE = "logbroker-active";
    String OFFER_STORAGE_ACTIVE = "offer-storage-active";
    String S3_ACTIVE = "s3-active";
}
