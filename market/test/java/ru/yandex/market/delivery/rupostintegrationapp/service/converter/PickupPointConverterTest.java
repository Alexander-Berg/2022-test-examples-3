package ru.yandex.market.delivery.rupostintegrationapp.service.converter;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import utils.FixtureRepository;

import ru.yandex.market.delivery.entities.common.Location;
import ru.yandex.market.delivery.entities.common.Phone;
import ru.yandex.market.delivery.entities.common.PickupPoint;
import ru.yandex.market.delivery.entities.common.WorkTime;
import ru.yandex.market.delivery.entities.common.constant.PickupPointType;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater.RuPostPickupPointSaxHandler;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.getreferencepickuppoints.updater.consumer.RuPostPickupPointConsumer;

class PickupPointConverterTest extends BaseTest {

    private static final List<WorkTime> WORK_TIMES = Collections.singletonList(new WorkTime());

    static Stream<Arguments> getParameters() throws Exception {
        return Stream.of(
            Arguments.of(getSimpleRussianPostPickupPoint(), getSimplePickupPoint()),
            Arguments.of(getFilledRussianPostPickupPoint(), getFilledPickupPoint())
        );
    }

    private static PickupPoint getSimplePickupPoint() {
        PickupPoint pickupPoint = new PickupPoint();
        pickupPoint.setCode("633451");
        pickupPoint.setName("Отделение почтовой связи ТОГУЧИН 633451");
        pickupPoint.setAddress(new Location());

        Phone phone = new Phone();
        phone.setPhoneNumber("+7 (38340) 2-04-29");
        pickupPoint.setPhones(new Phone[]{phone});

        pickupPoint.setActive(true);
        pickupPoint.setCashAllowed(true);
        pickupPoint.setPrepayAllowed(true);
        pickupPoint.setCardAllowed(false);
        pickupPoint.setReturnAllowed(true);
        pickupPoint.setType(PickupPointType.RUSSIAN_POST_PICKUP_POINT);
        pickupPoint.setStoragePeriod(30);
        pickupPoint.setSchedule(WORK_TIMES.toArray(new WorkTime[WORK_TIMES.size()]));

        return pickupPoint;
    }

    private static RussianPostPickupPoint getSimpleRussianPostPickupPoint() throws Exception {
        Consumer consumer = new Consumer();
        SAXParser parser = SAXParserFactory.newInstance().newSAXParser();

        parser.parse(
            new ByteArrayInputStream(FixtureRepository.getSinglePickupPointListXml()),
            new RuPostPickupPointSaxHandler(consumer)
        );

        return consumer.postPickupPoint;
    }

    private static PickupPoint getFilledPickupPoint() {
        PickupPoint point = getSimplePickupPoint();
        point.setMaxWeight(1.1);
        point.setMaxWidth(100);
        point.setMaxHeight(200);
        point.setMaxLength(300);
        point.setMaxSidesSum(100 + 200 + 300);
        return point;
    }

    private static RussianPostPickupPoint getFilledRussianPostPickupPoint() throws Exception {
        RussianPostPickupPoint point = getSimpleRussianPostPickupPoint();
        point.setMaxWeight("1100");
        point.setMaxWidth("100");
        point.setMaxHeight("200");
        point.setMaxLength("300");
        point.setTariffActualizationDate("1000000");
        return point;
    }

    @ParameterizedTest
    @MethodSource("getParameters")
    void convert(RussianPostPickupPoint russianPostPickupPoint, PickupPoint pickupPoint) {
        WorkTimeParser workTimeParserMock = Mockito.mock(WorkTimeParser.class);
        Mockito.when(workTimeParserMock.parse(Mockito.anyString()))
            .thenReturn(WORK_TIMES);

        RuPostPickupPointToLocationConverter locationConverter = Mockito.mock(
            RuPostPickupPointToLocationConverter.class
        );
        Mockito.when(locationConverter.convert(Mockito.any(RussianPostPickupPoint.class))).thenReturn(new Location());

        softly.assertThat(
            new PickupPointConverter(workTimeParserMock, locationConverter).convert(russianPostPickupPoint)
        )
            .as("PickupPoint converter cannot convert ruPostPP to YD PP")
            .isEqualTo(pickupPoint);
    }

    private static class Consumer implements RuPostPickupPointConsumer {
        RussianPostPickupPoint postPickupPoint;

        @Override
        public void consume(RussianPostPickupPoint pickupPoint) {
            this.postPickupPoint = pickupPoint;
        }
    }


}
