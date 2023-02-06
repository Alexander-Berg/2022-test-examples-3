package ru.yandex.market.adv.shop.integration.metrika.yt.repository;

import java.time.Instant;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.metrika.yt.entity.ContactInfo;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

@DisplayName("Тесты на репозиторий YtContactInfoRepository")
class YtContactInfoRepositoryTest extends AbstractShopIntegrationTest {

    @Autowired
    private YtContactInfoRepository ytContactInfoRepository;

    @DisplayName("Успешно получили информацию о контактах админов бизнеса из Yt-таблицы")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/getAdminContacts_existIds_listContacts_mbi_contact_all_info"
            ),
            before = "YtContactInfoRepositoryTest/json/getAdminContacts_existIds_listContacts.before.json"
    )
    @Test
    void getAdminContacts_existIds_listContacts() {
        run("getAdminContacts_existIds_listContacts_",
                () -> Assertions.assertThat(
                                ytContactInfoRepository.getAdminContacts(List.of(1L, 2L))
                        )
                        .containsExactlyInAnyOrder(
                                getContactInfo(1, 1, "BUSINESS_OWNER"),
                                getContactInfo(2, 1, "BUSINESS_ADMIN"),
                                getContactInfo(4, 2, "BUSINESS_ADMIN")
                        )
        );
    }

    @DisplayName("Успешно обработали случай, когда входящий список партнеров пустой")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/getAdminContacts_existIds_listContacts_mbi_contact_all_info"
            )
    )
    @Test
    void getAdminContacts_emptyIds_emptyList() {
        run("getAdminContacts_emptyIds_emptyList_",
                () -> Assertions.assertThat(
                                ytContactInfoRepository.getAdminContacts(List.of())
                        )
                        .isEmpty()
        );
    }

    @DisplayName("Успешно обработали случай, когда таблица в Yt пустая")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ContactInfo.class,
                    path = "//tmp/getAdminContacts_emptyTable_emptyList_mbi_contact_all_info"
            )
    )
    @Test
    void getAdminContacts_emptyTable_emptyList() {
        run("getAdminContacts_emptyTable_emptyList_",
                () -> Assertions.assertThat(
                                ytContactInfoRepository.getAdminContacts(List.of(1L, 2L))
                        )
                        .isEmpty()
        );
    }

    private ContactInfo getContactInfo(long contactId, long partnerId, String userType) {
        return new ContactInfo(contactId, partnerId, userType, String.format("User %s", contactId),
                String.format("user%s@yandex.ru", contactId), "+79999999999", "UNKNOWN",
                String.format("user%s", contactId), "2022-05-12", Instant.ofEpochMilli(1649421305000L), 1);
    }
}
