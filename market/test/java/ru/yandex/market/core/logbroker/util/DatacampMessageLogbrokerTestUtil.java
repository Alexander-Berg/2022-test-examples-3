package ru.yandex.market.core.logbroker.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampUnitedOffer;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.core.logbroker.event.datacamp.DatacampMessageLogbrokerEvent;
import ru.yandex.market.logbroker.LogbrokerService;

/**
 * Класс утилита для валидации отправки в logbroker события {@link DatacampMessageLogbrokerEvent}
 * Date: 03.02.2021
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
public class DatacampMessageLogbrokerTestUtil {

    private DatacampMessageLogbrokerTestUtil() {
    }

    /**
     * Получаем список событий отправленных в логброкер
     *
     * @param logbrokerService   сервис
     * @param numberOfInvocation количество вызовов logbroker
     * @return список событий
     */
    public static List<DatacampMessageLogbrokerEvent> getLogbrokerEvents(LogbrokerService logbrokerService,
                                                                         int numberOfInvocation) {
        var captor = ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);

        Mockito.verify(logbrokerService, Mockito.times(numberOfInvocation))
                .publishEvent(captor.capture());

        return captor.getAllValues();
    }

    /**
     * Получение справочника идентификатор оффера - оффер
     *
     * @param allValues список событий в logbroker
     * @param index     индекс
     * @param partnerId идентификатор партнера
     * @return справочник
     */
    @Nonnull
    public static Map<DataCampOfferIdentifiers.OfferIdentifiers, DataCampUnitedOffer.UnitedOffer> assertAndGetOffers(
            List<DatacampMessageLogbrokerEvent> allValues, int index, int partnerId, String... ignoreFields
    ) {
        return allValues.get(index)
                .getPayload()
                .getUnitedOffersList()
                .stream()
                .map(DataCampUnitedOffer.UnitedOffersBatch::getOfferList)
                .flatMap(Collection::stream)
                .peek(unitedOffer -> {
                    DataCampOffer.Offer basic = unitedOffer.getBasic();
                    DataCampOffer.Offer service = unitedOffer.getServiceMap().get(partnerId);

                    if (!DataCampOffer.Offer.getDefaultInstance().equals(basic)) {
                        Assertions.assertThat(basic)
                                .usingRecursiveComparison()
                                .ignoringAllOverriddenEquals()
                                .ignoringFieldsMatchingRegexes(ignoreFields)
                                .isEqualTo(service);
                    }
                })
                .collect(Collectors.toMap(
                        e -> (DataCampOffer.Offer.getDefaultInstance().equals(e.getBasic())
                                ? e.getServiceOrThrow(partnerId)
                                : e.getBasic()
                        ).getIdentifiers(),
                        Function.identity()
                ));
    }
}
