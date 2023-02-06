package ru.yandex.market.partner.notification.dao.tag;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.notification.safe.model.tag.AddressTagFilterSettings;
import ru.yandex.market.notification.safe.model.tag.TagFilter;
import ru.yandex.market.notification.simple.service.filter.AddressFilterSettingsProviderContextImpl;
import ru.yandex.market.notification.xiva.push.model.address.PushNotificationAddress;
import ru.yandex.market.partner.notification.AbstractFunctionalTest;
import ru.yandex.market.partner.notification.service.tag.TagAddressFilterSettingsProvider;
import ru.yandex.market.partner.notification.service.tag.TagAddressFilterSettingsType;
import ru.yandex.market.partner.notification.service.tag.TagFilterSettingsImpl;
import ru.yandex.market.partner.notification.service.tag.model.AndTagFilter;
import ru.yandex.market.partner.notification.service.tag.model.MapTagFilter;
import ru.yandex.market.partner.notification.service.tag.model.OrTagFilter;
import ru.yandex.market.partner.notification.service.tag.model.ValueTagFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ru.yandex.market.notification.simple.model.type.NotificationTransport.MOBILE_PUSH;

@DbUnitDataSet(
        before = "NotificationTagFilterProvider/loadTagFilterSettings.before.csv"
)
public class TagFilterProviderImplTest extends AbstractFunctionalTest {
    @Autowired
    TagAddressFilterSettingsProvider filterProvider;

    @Test
    public void shouldLoadNotificationTags() throws IOException {
        List<AddressTagFilterSettings> filters = filterProvider.provide(
                new AddressFilterSettingsProviderContextImpl(
                        MOBILE_PUSH,
                        PushNotificationAddress.create(0),
                        List.of("testScope")
                )
        );
        Assertions.assertThat(filters).isEqualTo(
                List.of(new TagFilterSettingsImpl("testScope", TagAddressFilterSettingsType.SIMPLE_BOOLEAN, makeFilter()))
        );
    }

    @Test
    public void shouldLoadTagsWithEmpties() throws IOException {
        List<AddressTagFilterSettings> filters = filterProvider.provide(
                new AddressFilterSettingsProviderContextImpl(
                        MOBILE_PUSH,
                        PushNotificationAddress.create(0),
                        List.of("testScope3")
                )
        );
        Assertions.assertThat(filters).isEqualTo(
                List.of(new TagFilterSettingsImpl("testScope3", TagAddressFilterSettingsType.SIMPLE_BOOLEAN, makeEmpties()))
        );
    }

    private TagFilter makeFilter() throws IOException {
        return new OrTagFilter(Set.of(
                new AndTagFilter(Set.of(
                        new ValueTagFilter("dayOfWeek", Set.of("SA", "SU")),
                        new MapTagFilter(
                                "campaignId",
                                Map.of(
                                        "111", new AndTagFilter(Set.of(
                                                new ValueTagFilter("statusType", Set.of("NEW")))
                                        ),
                                        "222", new AndTagFilter(Set.of(
                                                new ValueTagFilter("statusType", Set.of("NEW", "CANCELLED")))
                                        )
                                )
                        )
                )),
                new AndTagFilter(Set.of(
                        new MapTagFilter(
                                "campaignId",
                                Map.of(
                                        "333", new OrTagFilter(Set.of(
                                                new AndTagFilter(Set.of(new ValueTagFilter("dayOfWeek", Set.of("MO")))),
                                                new AndTagFilter(Set.of(new ValueTagFilter("statusType", Set.of("DELIVERED"))))
                                        ))
                                )
                        )
                ))
        ));
    }

    private TagFilter makeEmpties() {
        return new AndTagFilter(
                Set.of(
                        new ValueTagFilter("tag", Set.of("value")),
                        new OrTagFilter(Set.of()),
                        new MapTagFilter("eventType", Map.of())

                )
        );
    }

}
