package ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.FixtureRepository;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater.consumer.RuPostPickupPointConsumer;

class RuPostPickupPointSaxHandlerTest extends BaseTest {
    private RussianPostPickupPoint pickupPoint;

    @BeforeEach
    void prepare() throws Exception {
        Consumer consumer = new Consumer();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

        parser.parse(
            new ByteArrayInputStream(FixtureRepository.getSinglePickupPointListXml()),
            new RuPostPickupPointSaxHandler(consumer)
        );

        pickupPoint = consumer.postPickupPoint;
    }

    @Test
    void testTagParsing() {
        softly.assertThat(pickupPoint.getId().intValue())
            .as("Getter \"getCompanyId\" return incorrect value")
            .isEqualTo(40566);

        softly.assertThat(pickupPoint.getName())
            .as("Getter \"getName\" return incorrect value")
            .isEqualTo("Отделение почтовой связи ТОГУЧИН 633451");

        softly.assertThat(pickupPoint.getIndex())
            .as("Getter \"getIndex\" return incorrect value")
            .isEqualTo("633451");

        softly.assertThat(pickupPoint.getCountry())
            .as("Getter \"getCountry\" return incorrect value")
            .isEqualTo("Россия");

        softly.assertThat(pickupPoint.getArea())
            .as("Getter \"getArea\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getSubAdminArea())
            .as("Getter \"getSubAdminArea\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getLocality())
            .as("Getter \"getLocality\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getStreet())
            .as("Getter \"getStreet\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getHouse())
            .as("Getter \"getHouse\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getPhoneExtension())
            .as("Getter \"getPhoneExtension\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getPhoneType())
            .as("Getter \"getPhoneType\" return incorrect value")
            .isEqualTo("phone");

        softly.assertThat(pickupPoint.getPhoneNumber())
            .as("Getter \"getPhoneNumber\" return incorrect value")
            .isEqualTo("+7 (38340) 2-04-29");

        softly.assertThat(pickupPoint.getPhoneInfo())
            .as("Getter \"getPhoneInfo\" return incorrect value")
            .isEqualTo("Начальник ОПС");

        softly.assertThat(pickupPoint.getEmail())
            .as("Getter \"getEmail\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getUrl())
            .as("Getter \"getUrl\" return incorrect value")
            .isEqualTo("https://www.pochta.ru/");

        softly.assertThat(pickupPoint.getAdditionalUrl())
            .as("Getter \"getAdditionalUrl\" return incorrect value")
            .isNull();

        softly.assertThat(pickupPoint.getWorkingTime())
            .as("Getter \"getWorkingTime\" return incorrect value")
            .isEqualTo("вт: 08:00-17:00, ср: 08:00-17:00, чт: 08:00-17:00, пт: 08:00-17:00, сб: 08:00-17:00");

        softly.assertThat(pickupPoint.getRubricId())
            .as("Getter \"getRubricId\" return incorrect value")
            .isEqualTo("184108341");

        softly.assertThat(pickupPoint.getActualizationDate())
            .as("Getter \"getActualizationDate\" return incorrect value")
            .isEqualTo("1526349674");
    }

    @Test
    void testBooleanFlagParsing() {
        //it seems, that boolean flags are deprecated
        softly.assertThat(pickupPoint.getBooleanFlags())
            .as("Boolean flags")
            .isEmpty();
    }

    @Test
    void testMultipleEnumFlags() {
        softly.assertThat(pickupPoint.getEnumMultipleFlags())
            .as("Multiple flags")
            .containsKeys("consumer_services", "postal_services", "financial_services");
    }

    @Test
    void testSomeMultipleEnumFlag() {
        //@todo test all flags
        softly.assertThat(pickupPoint.getEnumMultipleFlags().get("consumer_services"))
            .as("consumer_services")
            .contains("printout_post", "subscription_periodicals", "air_train_tickets_post", "lottery_post");
    }

    private static class Consumer implements RuPostPickupPointConsumer {
        RussianPostPickupPoint postPickupPoint;

        @Override
        public void consume(RussianPostPickupPoint pickupPoint) {
            this.postPickupPoint = pickupPoint;
        }
    }
}
