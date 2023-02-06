package ru.yandex.travel.orders.services.promo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.javamoney.moneta.Money;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import ru.yandex.travel.commons.proto.ProtoCurrencyUnit;
import ru.yandex.travel.credentials.UserCredentials;
import ru.yandex.travel.credentials.UserCredentialsBuilder;
import ru.yandex.travel.orders.commons.proto.EPromoCodeNominalType;
import ru.yandex.travel.orders.commons.proto.EServiceType;
import ru.yandex.travel.orders.entities.promo.DiscountApplicationConfig;
import ru.yandex.travel.orders.entities.promo.PromoAction;
import ru.yandex.travel.orders.entities.promo.PromoCode;
import ru.yandex.travel.orders.repository.promo.PromoCodeActivationRepository;
import ru.yandex.travel.orders.repository.promo.PromoCodeRepository;
import ru.yandex.travel.orders.services.AuthorizationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PromoCodeApplicationServiceTest {
    private PromoCodeApplicationService promoCodeApplicationService;

    private PromoCodeRepository promoCodeRepository;

    private PromoCodeDiscountCalculator promoCodeDiscountCalculator;

    private UserCredentialsBuilder userCredentialsBuilder;

    private AuthorizationService authorizationService;

    private Environment env;

    @Before
    public void setUp() {

        LocalDateTime fixedPast = LocalDateTime.parse("2020-08-01T00:00:00");

        Clock pastClock = Clock.fixed(fixedPast.atZone(ZoneId.of("UTC")).toInstant(), ZoneId.of("UTC"));

        PromoAction expiredAction = new PromoAction();
        expiredAction.setDiscountApplicationConfig(new DiscountApplicationConfig());
        expiredAction.setValidTill(Instant.now(pastClock));
        PromoAction eternalAction = new PromoAction();
        eternalAction.setDiscountApplicationConfig(new DiscountApplicationConfig());
        PromoAction emptyAction = new PromoAction();
        emptyAction.setDiscountApplicationConfig(new DiscountApplicationConfig());
        emptyAction.setInitialBudget(BigDecimal.TEN);
        emptyAction.setRemainingBudget(BigDecimal.TEN);

        Map<String, PromoCode> promoCodes = new HashMap<>();
        promoCodes.put("SUCCESS", createPromoCode("SUCCESS", BigDecimal.valueOf(100L), EPromoCodeNominalType.NT_VALUE
                , eternalAction, false));
        promoCodes.put("EXPIRED", createPromoCode("EXPIRED", BigDecimal.valueOf(100L), EPromoCodeNominalType.NT_VALUE
                , expiredAction, false));
        promoCodes.put("BLACKLIST", createPromoCode("BLACKLIST", BigDecimal.valueOf(100L),
                EPromoCodeNominalType.NT_VALUE
                , eternalAction, true));
        promoCodes.put("EMPTY", createPromoCode("EMPTY", BigDecimal.valueOf(100L), EPromoCodeNominalType.NT_VALUE,
                emptyAction, false));

        userCredentialsBuilder = new UserCredentialsBuilder();
        promoCodeRepository = createPromoCodeRepository(promoCodes);
        authorizationService = mock(AuthorizationService.class);
        promoCodeDiscountCalculator = mock(PromoCodeDiscountCalculator.class);
        env = new MockEnvironment();
        promoCodeApplicationService = new PromoCodeApplicationService(
                promoCodeRepository, mock(PromoCodeActivationRepository.class), promoCodeDiscountCalculator,
                authorizationService, env, new PromoCodeChecker(Clock.systemDefaultZone())
        );
    }

    @Test
    public void testCalculateResultSuccess() {
        Money original = Money.of(BigDecimal.valueOf(1000L), ProtoCurrencyUnit.RUB);
        String promoCode = "SUCCESS";
        when(promoCodeDiscountCalculator.calculateDiscountForEstimation(any()))
                .thenReturn(CodeApplicationResult.success(promoCode, Money.of(100L, ProtoCurrencyUnit.RUB)));
        var staffUser = createUser(true, false);
        ServiceDescription serviceDescription = ServiceDescription.builder()
                .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                .originalCost(original)
                .build();
        var request = PromoCodeCalculationRequest.builder()
                .serviceDescriptions(List.of(serviceDescription))
                .promoCodes(List.of(promoCode))
                .build();
        PromoCodeApplicationResult result = promoCodeApplicationService.calculateResult(staffUser, request);
        assertThat(result.getApplicationResults()).isNotEmpty();
        assertThat(result.getOriginalAmount()).isGreaterThan(result.getDiscountedAmount());
        assertThat(result.getApplicationResults().get(0).getType()).isEqualTo(ApplicationResultType.SUCCESS);
    }

    @Test
    public void testCalculateResultExpired() {
        Money original = Money.of(BigDecimal.valueOf(1000L), ProtoCurrencyUnit.RUB);
        String promoCode = "EXPIRED";
        var staffUser = createUser(true, false);
        var request = PromoCodeCalculationRequest.builder()
                .serviceDescriptions(List.of(
                        ServiceDescription.builder()
                                .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                                .originalCost(original)
                                .build()
                ))
                .promoCodes(List.of(promoCode))
                .build();
        PromoCodeApplicationResult result = promoCodeApplicationService.calculateResult(staffUser, request);
        assertThat(result.getApplicationResults()).isNotEmpty();
        assertThat(result.getOriginalAmount()).isEqualTo(result.getDiscountedAmount());
        assertThat(result.getApplicationResults().get(0).getType()).isEqualTo(ApplicationResultType.EXPIRED);
    }

    @Test
    public void testCalculateResultEmptyBudget() {
        Money original = Money.of(BigDecimal.valueOf(1000L), ProtoCurrencyUnit.RUB);
        String promoCode = "EMPTY";
        when(promoCodeDiscountCalculator.calculateDiscountForEstimation(any()))
                .thenReturn(CodeApplicationResult.emptyBudget(promoCode));
        var staffUser = createUser(true, false);
        var request = PromoCodeCalculationRequest.builder()
                .serviceDescriptions(List.of(
                        ServiceDescription.builder()
                                .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                                .originalCost(original)
                                .build()
                ))
                .promoCodes(List.of(promoCode))
                .build();
        PromoCodeApplicationResult result = promoCodeApplicationService.calculateResult(staffUser, request);
        assertThat(result.getApplicationResults()).isNotEmpty();
        assertThat(result.getOriginalAmount()).isEqualTo(result.getDiscountedAmount());
        assertThat(result.getApplicationResults().get(0).getType()).isEqualTo(ApplicationResultType.EMPTY_BUDGET);
    }

    @Test
    public void testCalculateResultNotApplicable() {
        Money original = Money.of(BigDecimal.valueOf(1000L), ProtoCurrencyUnit.RUB);
        String promoCode = "SUCCESS";
        var staffUser = createUser(true, false);
        when(promoCodeDiscountCalculator.calculateDiscountForEstimation(any()))
                .thenReturn(CodeApplicationResult.notApplicable(promoCode));
        var request = PromoCodeCalculationRequest.builder()
                .serviceDescriptions(List.of(
                        ServiceDescription.builder()
                                .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                                .originalCost(original)
                                .build()
                ))
                .promoCodes(List.of(promoCode))
                .build();
        PromoCodeApplicationResult result = promoCodeApplicationService.calculateResult(staffUser, request);
        assertThat(result.getApplicationResults()).isNotEmpty();
        assertThat(result.getOriginalAmount()).isEqualTo(result.getDiscountedAmount());
        assertThat(result.getApplicationResults().get(0).getType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);
    }

    @Test
    public void testCalculateResultForBlacklistedNotApplicable() {
        Money original = Money.of(BigDecimal.valueOf(1000L), ProtoCurrencyUnit.RUB);
        String promoCode = "BLACKLIST";
        var staffUser = createUser(true, false);
        when(promoCodeDiscountCalculator.calculateDiscountForEstimation(any()))
                .thenReturn(CodeApplicationResult.success(promoCode, Money.of(100L, ProtoCurrencyUnit.RUB)));
        var request = PromoCodeCalculationRequest.builder()
                .serviceDescriptions(List.of(
                        ServiceDescription.builder()
                                .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                                .originalCost(original)
                                .build()
                ))
                .promoCodes(List.of(promoCode))
                .build();
        PromoCodeApplicationResult result = promoCodeApplicationService.calculateResult(staffUser, request);
        assertThat(result.getApplicationResults()).isNotEmpty();
        assertThat(result.getOriginalAmount()).isEqualTo(result.getDiscountedAmount());
        assertThat(result.getApplicationResults().get(0).getType()).isEqualTo(ApplicationResultType.NOT_APPLICABLE);
    }

    @Test
    public void testCalculateResultNotFound() {
        Money original = Money.of(BigDecimal.valueOf(1000L), ProtoCurrencyUnit.RUB);
        String promoCode = "DOES_NOT_EXIST";
        var staffUser = createUser(true, false);
        var request = PromoCodeCalculationRequest.builder()
                .serviceDescriptions(List.of(
                        ServiceDescription.builder()
                                .serviceType(EServiceType.PT_DOLPHIN_HOTEL)
                                .originalCost(original)
                                .build()
                ))
                .promoCodes(List.of(promoCode))
                .build();
        PromoCodeApplicationResult result = promoCodeApplicationService.calculateResult(staffUser, request);
        assertThat(result.getApplicationResults()).isNotEmpty();
        assertThat(result.getOriginalAmount()).isEqualTo(result.getDiscountedAmount());
        assertThat(result.getApplicationResults().get(0).getType()).isEqualTo(ApplicationResultType.NOT_FOUND);
    }

    public UserCredentials createUser(boolean isStaff, boolean isPlus) {
        return userCredentialsBuilder.build(
                "sessionKey", "yandexUid", "200", "login", "ut", "127.0.0.1", isStaff, isPlus
        );
    }

    private PromoCode createPromoCode(String code, BigDecimal nominal, EPromoCodeNominalType type,
                                      PromoAction promoAction, boolean blacklisted) {
        PromoCode result = new PromoCode();
        result.setCode(code);
        result.setNominal(nominal);
        result.setNominalType(type);
        result.setPromoAction(promoAction);
        result.setBlacklisted(blacklisted);
        return result;
    }

    private PromoCodeRepository createPromoCodeRepository(Map<String, PromoCode> promoCodes) {
        PromoCodeRepository result = mock(PromoCodeRepository.class);
        when(result.findByCodeEquals(any())).then(invocation -> promoCodes.get(invocation.getArgument(0)));
        return result;
    }
}
