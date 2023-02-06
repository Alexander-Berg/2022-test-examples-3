package ru.yandex.direct.api.v5.entity.vcards.delegate;

import java.math.BigDecimal;

import com.yandex.direct.api.v5.vcards.InstantMessenger;
import com.yandex.direct.api.v5.vcards.MapPoint;
import com.yandex.direct.api.v5.vcards.Phone;
import com.yandex.direct.api.v5.vcards.VCardAddItem;
import org.junit.Test;

import ru.yandex.direct.core.entity.vcard.model.PointOnMap;
import ru.yandex.direct.core.entity.vcard.model.Vcard;

import static org.assertj.core.api.Assertions.assertThat;

public class AddVCardsDelegateToVcardTest {
    private static final VCardAddItem REQUEST_ITEM = new VCardAddItem()
            .withCampaignId(1L)
            .withCountry(" country ")
            .withCity(" city ")
            .withCompanyName(" cn ")
            .withWorkTime(" 0;3;10;00;18;00;4;6;10;00;11;00 ")
            .withPhone(new Phone().withCountryCode("+7"))
            .withStreet(" s ")
            .withHouse(" h ")
            .withBuilding(" b ")
            .withApartment(" a ")
            .withInstantMessenger(new InstantMessenger().withMessengerClient("icq"))
            .withExtraMessage(" em ")
            .withContactEmail(" ce ")
            .withOgrn(" ogrn ")
            .withPointOnMap(new MapPoint().withX(BigDecimal.ONE))
            .withContactPerson(" cp ");
    private static final Vcard EXPECTED_VCARD = new Vcard()
            .withCampaignId(1L)
            .withCountry("country")
            .withCity("city")
            .withCompanyName("cn")
            .withWorkTime("0#3#10#00#18#00;4#6#10#00#11#00")
            .withPhone(new ru.yandex.direct.core.entity.vcard.model.Phone().withCountryCode("+7"))
            .withStreet("s")
            .withHouse("h")
            .withBuild("b")
            .withApart("a")
            .withInstantMessenger(new ru.yandex.direct.core.entity.vcard.model.InstantMessenger().withType("icq"))
            .withExtraMessage("em")
            .withEmail("ce")
            .withOgrn("ogrn")
            .withManualPoint(new PointOnMap().withX(BigDecimal.ONE))
            .withContactPerson("cp");

    @Test
    public void test() {
        Vcard actualVCard = AddVCardsDelegate.toVcard(REQUEST_ITEM);

        assertThat(actualVCard).isEqualTo(EXPECTED_VCARD);
    }
}
