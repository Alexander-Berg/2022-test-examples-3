package ru.yandex.market.api.partner.logbroker.util;

import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferStatus;
import Market.DataCamp.SyncAPI.SyncChangeOffer;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.core.logbroker.event.datacamp.DataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.logbroker.LogbrokerService;

/**
 * Класс утилита для валидации отправки в logbroker события {@link SyncChangeOfferLogbrokerEvent}
 * Date: 27.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class ChangeOfferLogbrokerTestUtil {

    private ChangeOfferLogbrokerTestUtil() {
    }

    /**
     * Получаем список событий отправленных в логброкер
     *
     * @param logbrokerService   сервис
     * @param numberOfInvocation количество вызовов logbroker
     * @return список событий
     */
    public static List<SyncChangeOfferLogbrokerEvent> getLogbrokerEvents(LogbrokerEventPublisher logbrokerService,
                                                                         int numberOfInvocation) {
        var captor = ArgumentCaptor.forClass(SyncChangeOfferLogbrokerEvent.class);

        Mockito.verify(logbrokerService, Mockito.times(numberOfInvocation))
                .publishEvent(captor.capture());

        return captor.getAllValues();
    }

    /**
     * Проверяем класс у {@link DataCampEvent}, отправленного в логброкер
     *
     * @param allValues     список событий
     * @param i             индекс
     * @param expectedClass ожидаемый класс
     */
    public static void assertEvent(List<SyncChangeOfferLogbrokerEvent> allValues, int i,
                                   Class<?> expectedClass) {
        Class<? extends DataCampEvent> actualClass = allValues.get(i)
                .getPayload()
                .iterator()
                .next()
                .getClass();

        Assertions.assertThat(actualClass)
                .isEqualTo(expectedClass);
    }

    /**
     * Проверяем тип запроса в логброкер
     *
     * @param event        событие
     * @param expectedType ожидаемый класс
     */
    public static void assertEventType(SyncChangeOfferLogbrokerEvent event,
                                       SyncChangeOffer.ChangeOfferRequest.RequestType expectedType) {
        try {
            Assertions.assertThat(SyncChangeOffer.ChangeOfferRequest.parseFrom(event.getBytes()).getRequestType())
                    .isEqualTo(expectedType);
        } catch (InvalidProtocolBufferException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Проверяем скрытый оффер на корректность заполнения информации
     *
     * @param offer          предложение
     * @param expectedFlag   ожидаемый флаг
     * @param expectedSource ожидаемый источник скрытия
     * @param expectedColor  ожидаемый цвет
     */
    public static void assertHiddenOffer(@Nullable DataCampOffer.Offer offer,
                                         boolean expectedFlag,
                                         DataCampOfferMeta.DataSource expectedSource,
                                         DataCampOfferMeta.MarketColor expectedColor) {
        Assertions.assertThat(offer)
                .isNotNull();

        DataCampOfferStatus.OfferStatus offerStatus = offer.getStatus();
        Assertions.assertThat(offerStatus.getDisabledCount())
                .isEqualTo(1);

        DataCampOfferMeta.OfferMeta offerMeta = offer.getMeta();
        Assertions.assertThat(offerMeta.getRgb())
                .isEqualTo(expectedColor);

        DataCampOfferMeta.Flag disabled = offerStatus.getDisabled(0);
        DataCampOfferMeta.UpdateMeta updateMeta = disabled.getMeta();

        Assertions.assertThat(updateMeta.getSource())
                .isEqualTo(expectedSource);
        Assertions.assertThat(disabled.getFlag())
                .isEqualTo(expectedFlag);

        Timestamp timestamp = updateMeta.getTimestamp();
        boolean after = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos())
                .plus(1, ChronoUnit.HOURS)
                .isAfter(Instant.now());

        Assertions.assertThat(after)
                .isTrue();
    }

    /**
     * Получение справочника идентификатор оффера - оффер
     *
     * @param event событие в logbroker
     * @return справочник
     */
    @Nonnull
    public static Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampOffer.Offer> getOffers(
            SyncChangeOfferLogbrokerEvent event
    ) {
        return event.getPayload()
                .stream()
                .map(DataCampEvent::convertToDataCampOffer)
                .collect(Collectors.toMap(DataCampOffer.Offer::getIdentifiers, Function.identity()));
    }
}
