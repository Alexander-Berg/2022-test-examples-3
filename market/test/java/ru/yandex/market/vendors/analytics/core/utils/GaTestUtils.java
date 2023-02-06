package ru.yandex.market.vendors.analytics.core.utils;

import com.google.api.services.analytics.model.Profile;
import com.google.api.services.analytics.model.Profiles;
import one.util.streamex.StreamEx;

/**
 * @author antipov93.
 */
public class GaTestUtils {

    private GaTestUtils() {
    }


    public static Profiles profiles(Profile... profiles) {
        return new Profiles().setItems(StreamEx.of(profiles).toList());
    }

    public static Profile profile(
            String id,
            String timezone
    ) {
        return new Profile()
                .setECommerceTracking(true)
                .setId(id)
                .setTimezone(timezone);
    }

    public static Profile profile(
            String id,
            String accountId,
            String webPropertyId,
            String name,
            String websiteUrl,
            boolean ecom
    ) {
        return new Profile()
                .setECommerceTracking(true)
                .setId(id)
                .setAccountId(accountId)
                .setWebPropertyId(webPropertyId)
                .setName(name)
                .setWebsiteUrl(websiteUrl)
                .setECommerceTracking(ecom);
    }
}
