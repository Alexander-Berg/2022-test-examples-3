package ru.yandex.market.core.notification.service.resolver;

import java.util.HashSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.language.model.Language;
import ru.yandex.market.notification.common.model.destination.MbiDestination;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NotificationLanguageResolverTest extends FunctionalTest {

    @Autowired
    private NotificationLanguageResolver tested;

    static Stream<Arguments> positiveArgs() {
        return Stream.of(
                Arguments.of(4l, Language.RUSSIAN), // белый магазин
                Arguments.of(null, Language.RUSSIAN)
        );
    }

    @DbUnitDataSet(before = "databaseTestData.csv")
    @ParameterizedTest
    @MethodSource("positiveArgs")
    public void testResolveLanguageByDestination_successful(Long shopId, Language expectedLanguage) {
        assertEquals(expectedLanguage, tested.resolve(new HashSet<>(asList(
                createDestinationForShop(shopId),
                createDestinationForShop(null)))));
    }

    @Test
    public void testResolveLanguageByDestination_noDestinations() {
        assertThat(tested.resolve(null), equalTo(Language.RUSSIAN));
        assertThat(tested.resolve(new HashSet<>()), equalTo(Language.RUSSIAN));
    }

    private MbiDestination createDestinationForShop(Long shopId) {
        return MbiDestination.create(shopId, null, null);
    }
}
