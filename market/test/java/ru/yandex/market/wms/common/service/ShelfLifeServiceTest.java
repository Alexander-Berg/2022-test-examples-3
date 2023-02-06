package ru.yandex.market.wms.common.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import ru.yandex.market.wms.common.model.dto.SkuLifetimesDTO;
import ru.yandex.market.wms.common.model.enums.ExpirationType;
import ru.yandex.market.wms.common.model.enums.RotationType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeCodeType;
import ru.yandex.market.wms.common.model.enums.ShelfLifeTemplate;
import ru.yandex.market.wms.common.pojo.ShelfLifeDates;
import ru.yandex.market.wms.common.service.validation.rule.shelflife.dates.ShelfLifeIsNotTooBigValidationRule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static ru.yandex.market.wms.common.model.enums.ShelfLifeIndicatorType.SHELF_LIFE_APPLICABLE;
import static ru.yandex.market.wms.common.model.enums.ShelfLifeIndicatorType.SHELF_LIFE_NOT_APPLICABLE;


@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ShelfLifeServiceTest {

    private static final Integer DEFAULT_DAYS = 20;
    private static final Integer OUTBOUND_DAYS = 1;
    private static final Integer INBOUND_DAYS = 98;
    private static final String OUTBOUND_SUSR5_DAYS = "1";
    private static final String INBOUND_SUSR4_DAYS = "98";
    private static final Integer LIFETIME_DAYS = 100;

    @Mock
    private DbConfigService configService;

    private ShelfLifeService service;

    @BeforeEach
    void initialize() {
        when(configService.getConfigAsInteger(eq("YM_DEF_INBOUND_SHELFLIFE_DAYS"), anyInt())).thenReturn(DEFAULT_DAYS);
        when(configService.getConfigAsInteger(eq("YM_DEF_OUTBOUND_SHELFLIFE_DAYS"), anyInt())).thenReturn(DEFAULT_DAYS);
        Clock clock = Clock.fixed(Instant.parse("2020-04-01T12:34:56.789Z"), ZoneOffset.UTC);
        service = new ShelfLifeService(configService, clock,
                Collections.singletonList(new ShelfLifeIsNotTooBigValidationRule()));
    }

    @Test
    void shouldAcceptValidEnabledShelflives() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();
        skuLifetimes.setShelflifeindicator("Y");

        service.validateShelfLives(skuLifetimes);
    }

    @Test
    void shouldAcceptAnyWhenIndicatorDisabled() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();
        skuLifetimes.setShelflifeindicator("N");

        service.validateShelfLives(skuLifetimes);
    }

    @Test
    void shouldNotChangeShelflivesWhenPercentageEmpty() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5(OUTBOUND_SUSR5_DAYS);
        skuLifetimes.setSusr4(INBOUND_SUSR4_DAYS);

        service.calculateAndUpdateShelflives(skuLifetimes);

        assertEquals(OUTBOUND_DAYS, skuLifetimes.getShelflife());
        assertEquals(INBOUND_DAYS, skuLifetimes.getShelflifeonreceiving());
    }

    @Test
    void shouldNotChangeShelflivesWhenResolvedPercentageMoreOrEqualsDays() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();

        service.calculateAndUpdateShelflives(skuLifetimes);

        assertEquals(OUTBOUND_DAYS, skuLifetimes.getShelflife());
        assertEquals(INBOUND_DAYS, skuLifetimes.getShelflifeonreceiving());
    }

    @Test
    void shouldChangeShelflivesWhenResolvedPercentageLessThanDays() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setSusr5(OUTBOUND_SUSR5_DAYS + 1);
        skuLifetimes.setSusr4(INBOUND_SUSR4_DAYS);
        skuLifetimes.setToexpiredays(LIFETIME_DAYS);
        skuLifetimes.setShelflifePercentage(1);
        skuLifetimes.setShelflifeonreceivingPercentage(50);

        service.calculateAndUpdateShelflives(skuLifetimes);

        assertEquals(1, skuLifetimes.getShelflife());
        assertEquals(50, skuLifetimes.getShelflifeonreceiving());
    }

    @Test
    void shouldChangeShelflivesWhenOnlyPercentagePresent() {
        SkuLifetimesDTO skuLifetimes = new SkuLifetimesDTO();
        skuLifetimes.setToexpiredays(LIFETIME_DAYS);
        skuLifetimes.setShelflifePercentage(10);
        skuLifetimes.setShelflifeonreceivingPercentage(70);

        service.calculateAndUpdateShelflives(skuLifetimes);

        assertEquals(10, skuLifetimes.getShelflife());
        assertEquals(20, skuLifetimes.getShelflifeonreceiving());
    }

    @Test
    void shouldSetupCorrectShelflivesWhenLottable04() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();
        skuLifetimes.setRotateby(RotationType.BY_MANUFACTURE_DATE.getValue());

        service.calculateAndUpdateShelflives(skuLifetimes);

        assertEquals(99, skuLifetimes.getShelflife());
        assertEquals(2, skuLifetimes.getShelflifeonreceiving());
    }

    @Test
    void shouldUseDefaultShelflivesWhenGlobalConfigEnabled() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();
        when(configService.getConfigAsBoolean("YM_USE_DEFAULT_SHELFLIFE_DAYS")).thenReturn(true);

        service.calculateAndUpdateShelflives(skuLifetimes);

        assertEquals(DEFAULT_DAYS, skuLifetimes.getShelflife());
        assertEquals(DEFAULT_DAYS, skuLifetimes.getShelflifeonreceiving());
    }

    @Test
    void shouldSetupShelflifeindicatorToNWithDependentFields() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();
        skuLifetimes.setShelflifeindicator("Y");
        skuLifetimes.setRotateby(RotationType.BY_MANUFACTURE_DATE.getValue());
        skuLifetimes.setShelflifecodetype(ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE.getValue());
        skuLifetimes.setLottablevalidationkey(ShelfLifeTemplate.MANUFACTURED.getValue());

        service.changeShelflifeIndicatorWithDependentFields(skuLifetimes, SHELF_LIFE_NOT_APPLICABLE);

        assertEquals("N", skuLifetimes.getShelflifeindicator());
        assertEquals(RotationType.BY_LOT.getValue(), skuLifetimes.getRotateby());
        assertEquals(ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE.getValue(), skuLifetimes.getShelflifecodetype());
        assertEquals(ShelfLifeTemplate.WITHOUT_LIMIT.getValue(), skuLifetimes.getLottablevalidationkey());
    }

    @Test
    void shouldSetupShelflifeindicatorToYWithDependentFields() {
        SkuLifetimesDTO skuLifetimes = getSkuWithAllShelflives();
        skuLifetimes.setShelflifeindicator("N");
        skuLifetimes.setRotateby(RotationType.BY_LOT.getValue());
        skuLifetimes.setShelflifecodetype(ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE.getValue());
        skuLifetimes.setLottablevalidationkey(ShelfLifeTemplate.MANUFACTURED.getValue());

        service.changeShelflifeIndicatorWithDependentFields(skuLifetimes, SHELF_LIFE_APPLICABLE);

        assertEquals("Y", skuLifetimes.getShelflifeindicator());
        assertEquals(RotationType.BY_EXPIRATION_DATE.getValue(), skuLifetimes.getRotateby());
        assertEquals(ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE.getValue(), skuLifetimes.getShelflifecodetype());
        assertEquals(ShelfLifeTemplate.EXPIRATION_DATE.getValue(), skuLifetimes.getLottablevalidationkey());
    }

    @Test
    void isExpiredForSkuWithDisabledLifetimes() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("N", null, null, null,
                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, null, ExpirationType.ON_RECEIVING).isExpired();
        boolean expiredOnOutbound =
                service.calculateExpirationInfo(skuLifetimes, null, ExpirationType.ON_OUTBOUND).isExpired();
        assertFalse(expiredOnReceiving);
        assertFalse(expiredOnOutbound);
    }

    @Test
    void isExpiredWithoutExpirationDateAndCreationDate() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, 5, 3, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates(null, null);
        assertImpossibleToCalculateBasedOnGivenArguments(skuLifetimes, dates);
    }

    @Test
    void isExpiredWithoutExpirationDateAndToExpireDays() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", null, 5, 3, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-04-01T12:34:56.789Z", null);
        assertImpossibleToCalculateBasedOnGivenArguments(skuLifetimes, dates);
    }

    private void assertImpossibleToCalculateBasedOnGivenArguments(SkuLifetimesDTO skuLifetimes,
                                                                  ShelfLifeDates dates) {
        IllegalArgumentException exceptionOnReceiving =
                assertThrows(IllegalArgumentException.class, () -> service.calculateExpirationInfo(skuLifetimes, dates,
                        ExpirationType.ON_RECEIVING));
        IllegalArgumentException exceptionOnOutbound =
                assertThrows(IllegalArgumentException.class, () -> service.calculateExpirationInfo(skuLifetimes, dates,
                        ExpirationType.ON_OUTBOUND));
        assertEquals("It is impossible to calculate expiration date based on given arguments",
                exceptionOnReceiving.getMessage());
        assertEquals("It is impossible to calculate expiration date based on given arguments",
                exceptionOnOutbound.getMessage());
    }

    @Test
    void isExpiredWithoutShelfLifeOnReceiving() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, null, null,
                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-04-01T12:34:56.789Z", null);
        IllegalArgumentException exceptionOnReceiving =
                assertThrows(IllegalArgumentException.class, () -> service.calculateExpirationInfo(skuLifetimes, dates,
                        ExpirationType.ON_RECEIVING));
        IllegalArgumentException exceptionOnOutbound =
                assertThrows(IllegalArgumentException.class, () -> service.calculateExpirationInfo(skuLifetimes, dates,
                        ExpirationType.ON_OUTBOUND));
        assertEquals("It is impossible to calculate expiration date on receiving/outbound based on given arguments",
                exceptionOnReceiving.getMessage());
        assertEquals("It is impossible to calculate expiration date on receiving/outbound based on given arguments",
                exceptionOnOutbound.getMessage());
    }

    @Test
    void isExpiredOnReceivingBasedOnExpirationDateWhenExpired() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, 5, null,
                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-01T12:34:56.789Z", "2020-04-04T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_RECEIVING).isExpired();
        assertTrue(expiredOnReceiving);
    }

    @Test
    void isExpiredOnOutboundBasedOnExpirationDateWhenExpired() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, null, 5,
                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-01T12:34:56.789Z", "2020-04-04T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_OUTBOUND).isExpired();
        assertTrue(expiredOnReceiving);
    }

    @Test
    void isExpiredOnReceivingBasedOnExpirationDateWhenNotExpired() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, 5, null, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates(null, "2020-04-07T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_RECEIVING).isExpired();
        assertFalse(expiredOnReceiving);
    }

    @Test
    void isExpiredOnOutboundBasedOnExpirationDateWhenNotExpired() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, null, 5, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates(null, "2020-04-07T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_OUTBOUND).isExpired();
        assertFalse(expiredOnReceiving);
    }

    @Test
    void isExpiredOnReceivingBasedOnCreationDateWhenExpired() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, 5, null, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-25T12:34:56.789Z", null);
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_RECEIVING).isExpired();
        assertTrue(expiredOnReceiving);
    }

    @Test
    void isExpiredOnOutboundBasedOnCreationDateWhenExpired() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, null, 5, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-25T12:34:56.789Z", null);
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_OUTBOUND).isExpired();
        assertTrue(expiredOnReceiving);
    }

    @Test
    void isExpiredOnReceivingBasedOnCreationDateWhenNotExpired() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, 5, null, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-27T12:34:56.789Z", null);
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_RECEIVING).isExpired();
        assertFalse(expiredOnReceiving);
    }

    @Test
    void isExpiredOnOutboundBasedOnCreationDateWhenNotExpired() {
        SkuLifetimesDTO skuLifetimes =
                createSkuLifetimes("Y", 10, null, 5, ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-27T12:34:56.789Z", null);
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_OUTBOUND).isExpired();
        assertFalse(expiredOnReceiving);
    }

    @Test
    void isExpiredOnReceivingWithManufacturedDateType() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, 7, null,
                ShelfLifeCodeType.BY_MANUFACTURED_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-27T12:34:56.789Z", "2020-04-06T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_RECEIVING).isExpired();
        assertFalse(expiredOnReceiving);
    }

    @Test
    void isExpiredOnOutboundWithManufacturedDateType() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, null, 7,
                ShelfLifeCodeType.BY_MANUFACTURED_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-27T12:34:56.789Z", "2020-04-06T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_OUTBOUND).isExpired();
        assertFalse(expiredOnReceiving);
    }

    @Test
    void isExpiredOnReceivingWithExpirationDateType() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, 7, null,
                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-27T12:34:56.789Z", "2020-04-06T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_RECEIVING).isExpired();
        assertTrue(expiredOnReceiving);
    }

    @Test
    void isExpiredOnOutboundWithExpirationDateType() {
        SkuLifetimesDTO skuLifetimes = createSkuLifetimes("Y", 10, null, 7,
                ShelfLifeCodeType.BY_BEST_EXPIRATION_DATE);
        ShelfLifeDates dates = createShelfLifeDates("2020-03-27T12:34:56.789Z", "2020-04-06T12:34:56.789Z");
        boolean expiredOnReceiving =
                service.calculateExpirationInfo(skuLifetimes, dates, ExpirationType.ON_OUTBOUND).isExpired();
        assertTrue(expiredOnReceiving);
    }

    @Test
    void calculateShelfLifeDatesWhenDisabled() {
        ShelfLifeDates shelfLifeDates =
                createShelfLifeDates("2020-04-18T12:00:00.000Z", null);
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator("N");
        skuLifetimesDTO.setToexpiredays(10);
        Exception exception =
                assertThrows(Exception.class,
                        () -> service.calculateShelfLifeDates(shelfLifeDates, skuLifetimesDTO));
        assertEquals("Could not calculate shelf life dates because it is not applicable for sku",
                exception.getMessage());
    }

    @Test
    void calculateShelfLifeWhenBothDatesSpecified() {
        ShelfLifeDates shelfLifeDates =
                createShelfLifeDates("2020-04-18T12:00:00.000Z", "2020-04-20T12:00:00.000Z");
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator("Y");
        skuLifetimesDTO.setToexpiredays(10);
        skuLifetimesDTO.setLottablevalidationkey(ShelfLifeTemplate.EXPIRATION_DATE.getValue());

        ShelfLifeDates calculatedShelfLifeDates = service.calculateShelfLifeDates(shelfLifeDates, skuLifetimesDTO);
        assertEquals(Instant.parse("2020-04-10T12:00:00.000Z"), calculatedShelfLifeDates.getCreationDateTime());
    }

    @Test
    void calculateShelfLifeWhenToExpireDaysNotSpecified() {
        ShelfLifeDates shelfLifeDates =
                createShelfLifeDates("2020-04-18T12:00:00.000Z", null);
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator("Y");
        skuLifetimesDTO.setToexpiredays(null);
        Exception exception =
                assertThrows(Exception.class,
                        () -> service.calculateShelfLifeDates(shelfLifeDates, skuLifetimesDTO));
        assertEquals("It is impossible to calculate shelf life dates because toExpireDays is not specified",
                exception.getMessage());
    }

    @Test
    void calculateShelfLifeWhenDatesParametersAreInvalid() {
        ShelfLifeDates shelfLifeDates =
                createShelfLifeDates("2020-04-18T12:00:00.000Z", null);
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator("Y");
        skuLifetimesDTO.setToexpiredays(4000);
        Exception exception =
                assertThrows(Exception.class,
                        () -> service.calculateShelfLifeDates(shelfLifeDates, skuLifetimesDTO));
        assertEquals("Срок годности не должен превышать 3650 дней", exception.getMessage());
    }

    @Test
    void calculateShelfLifeByCreationDateTime() {
        ShelfLifeDates shelfLifeDates =
                createShelfLifeDates("2020-04-18T12:00:00.000Z", null);
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator("Y");
        skuLifetimesDTO.setToexpiredays(10);
        ShelfLifeDates calculatedShelfLifeDates = service.calculateShelfLifeDates(shelfLifeDates, skuLifetimesDTO);
        ShelfLifeDates expectedShelfLifeDates =
                createShelfLifeDates("2020-04-18T12:00:00.000Z", "2020-04-28T12:00:00.000Z");
        assertEquals(expectedShelfLifeDates, calculatedShelfLifeDates);
    }

    @Test
    void calculateShelfLifeByExpirationDateTime() {
        ShelfLifeDates shelfLifeDates =
                createShelfLifeDates(null, "2020-04-18T12:00:00.000Z");
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator("Y");
        skuLifetimesDTO.setToexpiredays(10);
        skuLifetimesDTO.setLottablevalidationkey(ShelfLifeTemplate.EXPIRATION_DATE.getValue());
        ShelfLifeDates calculatedShelfLifeDates = service.calculateShelfLifeDates(shelfLifeDates, skuLifetimesDTO);
        ShelfLifeDates expectedShelfLifeDates =
                createShelfLifeDates("2020-04-08T12:00:00.000Z", "2020-04-18T12:00:00.000Z");
        assertEquals(expectedShelfLifeDates, calculatedShelfLifeDates);
    }

    private SkuLifetimesDTO getSkuWithAllShelflives() {
        SkuLifetimesDTO sku = new SkuLifetimesDTO();
        sku.setSusr5(OUTBOUND_SUSR5_DAYS);
        sku.setSusr4(INBOUND_SUSR4_DAYS);
        sku.setToexpiredays(LIFETIME_DAYS);
        sku.setShelflifePercentage(1);
        sku.setShelflifeonreceivingPercentage(99);
        return sku;
    }

    private SkuLifetimesDTO createSkuLifetimes(String shelfLifeIndicator,
                                               Integer toExpireDays,
                                               Integer shelfLifeOnReceiving,
                                               Integer shelfLifeOnOutbound,
                                               ShelfLifeCodeType shelfLifeCodeType) {
        SkuLifetimesDTO skuLifetimesDTO = new SkuLifetimesDTO();
        skuLifetimesDTO.setShelflifeindicator(shelfLifeIndicator);
        skuLifetimesDTO.setToexpiredays(toExpireDays);
        skuLifetimesDTO.setShelflifeonreceiving(shelfLifeOnReceiving);
        skuLifetimesDTO.setShelflife(shelfLifeOnOutbound);
        skuLifetimesDTO.setShelflifecodetype(shelfLifeCodeType.getValue());
        return skuLifetimesDTO;
    }

    private ShelfLifeDates createShelfLifeDates(String creationDateTime, String expirationDateTime) {
        return new ShelfLifeDates(fromString(creationDateTime), fromString(expirationDateTime));
    }

    private Instant fromString(String dateString) {
        return Optional.ofNullable(dateString)
                .map(Instant::parse)
                .orElse(null);
    }

}
