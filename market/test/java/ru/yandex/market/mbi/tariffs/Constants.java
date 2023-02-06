package ru.yandex.market.mbi.tariffs;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.market.mbi.tariffs.model.Partner;
import ru.yandex.market.mbi.tariffs.model.PartnerType;

/**
 * Класс с различными константами.
 */
@ParametersAreNonnullByDefault
public final class Constants {
    /**
     * см файлик partners.csv
     */
    public static class Partners {
        public static final long VALID_PARTNER_ID_12345 = 12345L; //shop
        public static final long VALID_PARTNER_ID_45678 = 45678L; //supplier
        public static final long VALID_PARTNER_ID_999 = 999L;     //business
        public static final long INVALID_PARTNER_ID_100 = 100L;

        public static final Partner VALID_PARTNER_SHOP = new Partner().id(VALID_PARTNER_ID_12345).type(PartnerType.SHOP);
        public static final Partner VALID_PARTNER_SUPPLIER = new Partner().id(VALID_PARTNER_ID_45678).type(PartnerType.SUPPLIER);
        public static final Partner VALID_PARTNER_BUSINESS = new Partner().id(VALID_PARTNER_ID_999).type(PartnerType.BUSINESS);

        public static final Partner INVALID_PARTNER = new Partner().id(INVALID_PARTNER_ID_100).type(PartnerType.BUSINESS);

    }
}
