package ru.yandex.market.core.logbroker.event.datacamp;

import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import org.assertj.core.api.Assertions;

import ru.yandex.market.common.test.util.ProtoTestUtil;

/**
 * Абстрактный тест для всех событий, отправляемых в datacamp
 * Date: 02.04.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class AbstractDataCampEventTest {

    /**
     * Проверка на корректность маппинга {@link DataCampEvent} в {@link DataCampOffer.Offer}
     *
     * @param dataCampEvent событие, которое проверяем
     * @param expectedFile  название файла с ожидаемыми данными
     */
    protected void assertDataCampEvent(String expectedFile, DataCampEvent dataCampEvent) {
        DataCampOffer.Offer expectedOffer = ProtoTestUtil.getProtoMessageByJson(
                DataCampOffer.Offer.class,
                "proto/" + expectedFile,
                getClass()
        );

        Assertions.assertThat(dataCampEvent.convertToDataCampOffer())
                .isEqualTo(expectedOffer);
    }
}
