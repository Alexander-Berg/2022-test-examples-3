package ru.yandex.market.api.internal.social;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import static org.junit.Assert.assertEquals;

public class SocialApiParserTest extends UnitTestBase {

    private SocialApiParser parser = new SocialApiParser();

    @Test
    public void testParse() {
        Map<Long, UserSocialData> actual = parser.parse(ResourceHelpers.getResource("social-api-data.json"));

        final Map<Long, UserSocialData> expected = new HashMap<Long, UserSocialData>() {{
            put(
                    12345L,
                    new UserSocialData(
                            12345,
                            Collections.singletonList(
                                    new SocialProvider("VKONTAKTE", "http://vk-user1")
                            )
                    )
            );
            put(
                    23456L,
                    new UserSocialData(
                            23456,
                            Arrays.asList(
                                    new SocialProvider("VKONTAKTE", "http://vk-user2"),
                                    new SocialProvider("TWITTER", "http://tw-user2"),
                                    new SocialProvider("FACEBOOK", "http://fb-user2"),
                                    new SocialProvider("ODNOKLASSNIKI", "http://ok-user2"),
                                    new SocialProvider("GOOGLE", "http://gg-user2"),
                                    new SocialProvider("MAILRU", "http://mr-user2"),
                                    new SocialProvider("FOURSQUARE", "http://fs-user2")
                            )
                    )
            );
        }};

        assertEquals(expected, actual);
    }

    @Test
    public void testParseWithOunkownProviders() {
        Map<Long, UserSocialData> actual = parser.parse(ResourceHelpers.getResource("data-with-unknown-providers.json"));

        Map<Long, UserSocialData> expected = new HashMap<Long, UserSocialData>() {{
            put(12345L, new UserSocialData(
                    12345,
                    Arrays.asList(
                            new SocialProvider("PROVIDER", "http://dz-user1"),
                            new SocialProvider("LJ", "http://lj-user2"),
                            new SocialProvider("IG", "http://ig-user2")
                    )
            ));
        }};
        assertEquals(expected, actual);
    }
}
