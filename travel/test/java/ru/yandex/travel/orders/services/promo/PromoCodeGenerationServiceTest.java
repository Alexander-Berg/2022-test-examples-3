package ru.yandex.travel.orders.services.promo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.orders.commons.proto.EPromoCodeNominalType;
import ru.yandex.travel.orders.entities.promo.BasePromoCodeGenerationConfig;
import ru.yandex.travel.orders.entities.promo.PromoAction;
import ru.yandex.travel.orders.entities.promo.PromoCode;
import ru.yandex.travel.orders.entities.promo.PromoCodeActivationsStrategy;
import ru.yandex.travel.orders.entities.promo.SimplePromoCodeGenerationConfig;
import ru.yandex.travel.orders.entities.promo.ValidTillGenerationType;
import ru.yandex.travel.orders.repository.promo.PromoActionRepository;
import ru.yandex.travel.orders.repository.promo.PromoCodeRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.travel.orders.services.promo.PromoCodeGenerationService.atEndOfDayInMoscow;

public class PromoCodeGenerationServiceTest {
    private PromoCodeGenerationService service;
    private PromoActionRepository promoActionRepository;
    private PromoCodeRepository promoCodeRepository;
    private PromoAction action;

    @Before
    public void setUp() {
        promoActionRepository = mock(PromoActionRepository.class);

        promoCodeRepository = mock(PromoCodeRepository.class);
        when(promoCodeRepository.save(any())).then(req -> req.getArguments()[0]);

        service = new PromoCodeGenerationService(
                (type) -> new SimplePromoCodeGenerationStrategy(promoActionRepository),
                promoActionRepository,
                promoCodeRepository,
                Clock.systemDefaultZone());

        action = new PromoAction();
        action.setId(UUID.randomUUID());
        action.setName("test" + action.getId());
        action.setValidFrom(Instant.now());

        action.setValidTill(atEndOfDayInMoscow(
                LocalDate.ofInstant(Instant.now().plus(2, ChronoUnit.DAYS), ZoneId.systemDefault())
        ));
        var config = new SimplePromoCodeGenerationConfig();
        config.setValidTillGenerationType(ValidTillGenerationType.FIXED_DATE);
        config.setMaxActivations(0);
        config.setFixedDate(LocalDate.of(1000, 1, 1));
        config.setMaxUsagePerUser(1);
        config.setNominal(10.0);
        config.setNominalType(EPromoCodeNominalType.NT_VALUE);
        action.setPromoCodeGenerationConfig(config);

        when(promoActionRepository.getOne(action.getId())).thenReturn(action);
        when(promoActionRepository.findByName(action.getName())).thenReturn(Optional.of(action));
    }

    /**
     * Tests {@link PromoCodeGenerationService#generatePromoCodeForAction(PromoAction, BigDecimal, EPromoCodeNominalType, LocalDate)}
     * with all its setters.
     */
    @Test
    public void generatePromoCodeForActionSetsAllPropertiesCorrectly() {
        BigDecimal nominal = BigDecimal.TEN;
        EPromoCodeNominalType nominalType = EPromoCodeNominalType.NT_VALUE;

        PromoCode code = service.generatePromoCodeForAction(action,
                nominal, nominalType,
                LocalDate.now(ZoneId.systemDefault()));

        assertNotNull(code.getId());
        assertSame(action, code.getPromoAction());
        assertNotNull(code.getCode());

        assertEquals(nominal, code.getNominal());
        assertEquals(nominalType, code.getNominalType());

        BasePromoCodeGenerationConfig config = action.getPromoCodeGenerationConfig();

        assertEquals(config.getMaxUsagePerUser(), code.getAllowedUsageCount());
        assertEquals(PromoCodeActivationsStrategy.UNLIMITED_ACTIVATIONS, code.getActivationsStrategy());
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), code.getAllowedActivationsTotal());
        assertEquals(Integer.valueOf(0), code.getAllowedActivationsCount());

        assertNotNull(code.getValidFrom());
        assertFalse(action.getValidFrom().isAfter(code.getValidFrom()));
        assertEquals(action.getValidTill(), code.getValidTill());

        assertFalse(code.isBlacklisted());

        verify(promoCodeRepository).save(code);
    }

    @Test
    public void generatePromoCodeForActionWorksFineWhenValidFromIsNull() {
        action.setValidFrom(null);

        PromoCode code = service.generatePromoCodeForAction(action,
                LocalDate.now(ZoneId.systemDefault()));

        assertNotNull(code.getId());

        verify(promoCodeRepository).save(code);
    }

    @Test
    public void generatePromoCodeSetsLimitedActivationsWhenDefinedSoInPromoAction() {
        Integer maxActivations = 1;
        action.getPromoCodeGenerationConfig().setMaxActivations(maxActivations);

        PromoCode code = service.generatePromoCodeForAction(action, LocalDate.now());

        assertEquals(maxActivations, code.getAllowedActivationsTotal());
        assertEquals(PromoCodeActivationsStrategy.LIMITED_ACTIVATIONS, code.getActivationsStrategy());
    }

    @Test
    public void generatePromoCodeCanSetCalculateValidTillWhenFixedDuration() {
        long days = 100L;
        action.getPromoCodeGenerationConfig().setValidTillGenerationType(ValidTillGenerationType.FIXED_DURATION);
        action.getPromoCodeGenerationConfig().setFixedDaysDuration(days);

        Instant validTill = service.generatePromoCodeForAction(action, LocalDate.now()).getValidTill();

        assertThat(validTill).isAfter(action.getValidFrom().plus(days, ChronoUnit.DAYS));
        assertThat(validTill).isBefore(action.getValidFrom().plus(days + 1, ChronoUnit.DAYS));
    }

    @Test
    public void generatePromoCodeReturnsNullIfWrongNameWasProvided() {
        assertNull(service.generatePromoCodeForAction("wrong_name", LocalDate.now()));
        verifyNoMoreInteractions(promoCodeRepository);
    }
}
