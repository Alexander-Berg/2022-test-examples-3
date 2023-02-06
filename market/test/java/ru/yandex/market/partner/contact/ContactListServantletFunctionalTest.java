package ru.yandex.market.partner.contact;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Tags;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.partner.contact.model.ContactListResponse;
import ru.yandex.market.partner.contact.model.DataDTO;
import ru.yandex.market.partner.contact.model.FullContactDTO;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;
import ru.yandex.market.tags.Components;
import ru.yandex.market.tags.Tests;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Tags({
        @Tag(Components.MBI_PARTNER),
        @Tag(Tests.COMPONENT)
})
public class ContactListServantletFunctionalTest extends FunctionalTest {

    @DisplayName("/contactList должен учитывать поставщиков")
    @Test
    @DbUnitDataSet(before = "testGetSupplierContacts.before.csv")
    public void testGetSupplierContacts() {
        long userId = 10;
        ContactListResponse response =
                FunctionalTestHelper.getXml(baseUrl + "/contactList?_user_id={userId}", ContactListResponse.class,
                        userId).getBody();

        DataDTO data = response.getPagedData().getData();
        assertThat("Есть элемент с контактами", data.getContacts(), notNullValue());
        assertThat("Возвращается один контакт", data.getContacts(), hasSize(1));
        assertThat("Есть контакты", data.getContacts(), not(empty()));
        FullContactDTO fullContact = data.getContacts().get(0);
        assertThat("userId должен совпадать с запрошенным",
                fullContact.getContact(), hasProperty("userId", equalTo(userId)));
    }

}
