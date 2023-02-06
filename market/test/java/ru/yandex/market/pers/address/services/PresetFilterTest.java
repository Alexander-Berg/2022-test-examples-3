package ru.yandex.market.pers.address.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.yandex.market.pers.address.dao.ObjectKey;
import ru.yandex.market.pers.address.model.Address;
import ru.yandex.market.pers.address.model.Contact;
import ru.yandex.market.pers.address.model.Preset;
import ru.yandex.market.pers.address.model.identity.Uid;
import ru.yandex.market.pers.address.services.blackbox.BlackboxClient;
import ru.yandex.market.pers.address.services.blackbox.UserFromBlackbox;
import ru.yandex.market.pers.address.services.monitor.Monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

class PresetFilterTest {

    private static final Uid UID = new Uid(1L);

    private BlackboxClient blackboxClient;
    private PresetFilter filter;

    @BeforeEach
    void setUp() {
        Monitor monitor = Mockito.mock(Monitor.class);
        blackboxClient = Mockito.mock(BlackboxClient.class);
        filter = new PresetFilter(blackboxClient, monitor);
    }

    @Test
    void testEmpty() {
        filter.filter(UID, new ArrayList<>());
        filter.filter(UID, null);
    }

    @Test
    public void test() {
        Mockito.when(blackboxClient.userInfo(Mockito.anyLong())).thenReturn(new UserFromBlackbox(
                "asd",
                "qwe",
                "123123123",
                Arrays.asList("q", "w", "bb-email")
        ));

        ArrayList<Preset> datasyncPresets = new ArrayList<>();
        datasyncPresets.add(new Preset(
                new ObjectKey("1"),
                null,
                Address.builder().setAddressLine("1").build(),
                Contact.builder().setEmail("1").setPhoneNum("112233").build())
        );
        datasyncPresets.add(new Preset(
                new ObjectKey("2"),
                null,
                Address.builder().setAddressLine("2").build(),
                Contact.builder().setEmail("2").setPhoneNum("112233").build())
        );
        datasyncPresets.add(new Preset(
                new ObjectKey("3"),
                null,
                Address.builder().setAddressLine("3").build(),
                Contact.builder().setEmail("3").setPhoneNum("112233").build())
        );
        datasyncPresets.add(new Preset(
                new ObjectKey("4"),
                null,
                Address.builder().setAddressLine("same-email-as-prev-but-not-from-bb").build(),
                Contact.builder().setEmail("3").setPhoneNum("1122334").build())
        );
        datasyncPresets.add(new Preset(
                new ObjectKey("5"),
                null,
                Address.builder().setAddressLine("fake").build(),
                Contact.builder().setEmail("fake").setPhoneNum("fake").build())
        );
        datasyncPresets.add(new Preset(
                new ObjectKey("6"),
                null,
                Address.builder().setAddressLine("email-from-bb").build(),
                Contact.builder().setEmail("bb-email").setPhoneNum("4534545664").build())
        );


        PresetFilter.FilteredPresets filtered = this.filter.filter(UID, datasyncPresets);

        Assertions.assertEquals(4, filtered.getGoodPresets().size());
        Assertions.assertFalse(containsWithId(filtered.getGoodPresets(), "4"));
        Assertions.assertFalse(containsWithId(filtered.getGoodPresets(), "5"));
        Assertions.assertTrue(containsWithId(filtered.getGoodPresets(), "1"));
        Assertions.assertTrue(containsWithId(filtered.getGoodPresets(), "2"));
        Assertions.assertTrue(containsWithId(filtered.getGoodPresets(), "3"));
        Assertions.assertTrue(containsWithId(filtered.getGoodPresets(), "6"));
    }

    private boolean containsWithId(List<Preset> list, String id) {
        return list
                .stream()
                .map(preset -> preset.getId().getObjectKey())
                .anyMatch(preset -> Objects.equals(preset, id));
    }

}
