package ru.yandex.market.fulfillment.stockstorage;

import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.annotation.EnableCaching;

import ru.yandex.market.fulfillment.stockstorage.domain.EnumWithName;
import ru.yandex.market.fulfillment.stockstorage.domain.dto.MarketColor;
import ru.yandex.market.fulfillment.stockstorage.repository.SystemPropertyRepository;
import ru.yandex.market.fulfillment.stockstorage.service.lms.LmsPartnerType;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyIntegerKey;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyService;
import ru.yandex.market.fulfillment.stockstorage.service.system.SystemPropertyStringKey;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.fulfillment.stockstorage.domain.dto.MarketColor.BLUE;
import static ru.yandex.market.fulfillment.stockstorage.domain.dto.MarketColor.GREEN;
import static ru.yandex.market.fulfillment.stockstorage.domain.dto.MarketColor.RED;

@EnableCaching
public class SystemPropertyServiceTest extends AbstractContextualTest {

    @Autowired
    private SystemPropertyService systemPropertyService;

    @MockBean
    private SystemPropertyRepository systemPropertyRepository;

    @Test
    @DatabaseSetup("classpath:database/states/system_property/1.xml")
    public void shouldCachingProperties() {
        systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.MAX_JOB_ATTEMPT_COUNT);
        systemPropertyService.getIntegerProperty(SystemPropertyIntegerKey.MAX_JOB_ATTEMPT_COUNT);

        verify(systemPropertyRepository, times(1)).getIntegerProperty(any());
    }

    @Test
    public void onCorrectCommaSeparatedProperty() {
        when(systemPropertyRepository.getStringProperty(
                SystemPropertyStringKey.ALLOWED_MARKET_COLORS_FOR_FEED_ID.name()))
                .thenReturn("BLUE  , RED,GREEN  ");

        var allowedColors = systemPropertyService.getCommaSeparatedValuesProperty(
                SystemPropertyStringKey.ALLOWED_MARKET_COLORS_FOR_FEED_ID,
                valueAsStr -> EnumWithName.findByName(valueAsStr, MarketColor.class),
                Set.of()
        );

        softly.assertThat(allowedColors).containsExactlyInAnyOrder(RED, BLUE, GREEN);
    }

    @Test
    public void onEmptyCommaSeparatedProperty() {
        when(systemPropertyRepository.getStringProperty(
                SystemPropertyStringKey.ADDITIONAL_LMS_TYPES_TO_SYNC_STOCKS.name()))
                .thenReturn("");

        var allowedColors = systemPropertyService.getCommaSeparatedValuesProperty(
                SystemPropertyStringKey.ADDITIONAL_LMS_TYPES_TO_SYNC_STOCKS,
                valueAsStr -> EnumWithName.findByName(valueAsStr, LmsPartnerType.class).getLmsPartnerType(),
                Set.of()
        );

        softly.assertThat(allowedColors).isNotNull();
        softly.assertThat(allowedColors).isEmpty();
    }
}
