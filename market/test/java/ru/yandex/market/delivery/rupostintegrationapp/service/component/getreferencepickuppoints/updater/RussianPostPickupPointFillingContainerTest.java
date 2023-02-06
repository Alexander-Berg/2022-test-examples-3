package ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;

class RussianPostPickupPointFillingContainerTest extends BaseTest {
    private RussianPostPickupPointFillingContainer fillingContainer;

    @BeforeEach
    void init() {
        fillingContainer = new RussianPostPickupPointFillingContainer();
    }

    @Test
        // TODO: 28/03/17 fill test by another tags
    void testReadingTextContents() {
        fillingContainer.readTagContents("name", "name 999999");
        fillingContainer.readTagContents("company-id", "123");
        fillingContainer.readTagContents("country", "Россия");
        fillingContainer.readTagContents("address", "address");
        fillingContainer.readTagContents("url", "url");
        fillingContainer.readTagContents("working-time", "вт: 08:00-16:00");
        fillingContainer.readTagContents("rubric-id", "234");
        fillingContainer.readTagContents("actualization-date", "1529373673");
        fillingContainer.readPhoneTagContents("ext", "ext");
        fillingContainer.readPhoneTagContents("type", "type");
        fillingContainer.readPhoneTagContents("number", "number");
        fillingContainer.readPhoneTagContents("info", "info");

        fillingContainer.readTagContents("street", "street");

        RussianPostPickupPoint pickupPoint = fillingContainer.getPickupPoint();

        softly.assertThat(pickupPoint.getId()).isEqualTo(Integer.valueOf(123));
        softly.assertThat(pickupPoint.getName()).isEqualTo("name 999999");
        softly.assertThat(pickupPoint.getIndex()).isEqualTo("999999");
        softly.assertThat(pickupPoint.getCountry()).isEqualTo("Россия");
        softly.assertThat(pickupPoint.getAddress()).isEqualTo("address");
        softly.assertThat(pickupPoint.getUrl()).isEqualTo("url");
        softly.assertThat(pickupPoint.getWorkingTime()).isEqualTo("вт: 08:00-16:00");
        softly.assertThat(pickupPoint.getRubricId()).isEqualTo("234");
        softly.assertThat(pickupPoint.getActualizationDate()).isEqualTo("1529373673");
        softly.assertThat(pickupPoint.getStreet()).isNull();

        softly.assertThat(pickupPoint.getPhoneExtension()).isEqualTo("ext");
        softly.assertThat(pickupPoint.getPhoneType()).isEqualTo("type");
        softly.assertThat(pickupPoint.getPhoneNumber()).isEqualTo("number");
        softly.assertThat(pickupPoint.getPhoneInfo()).isEqualTo("info");
    }
}
