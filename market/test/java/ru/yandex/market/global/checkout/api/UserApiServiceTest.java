package ru.yandex.market.global.checkout.api;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.checkout.domain.order.OrderModel;
import ru.yandex.market.global.checkout.domain.promo.apply.fixed_discount.FixedDiscountArgs;
import ru.yandex.market.global.checkout.domain.promo.model.PromoTarget;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.db.jooq.enums.EDeliveryOrderState;
import ru.yandex.market.global.db.jooq.enums.EOrderState;
import ru.yandex.market.global.db.jooq.enums.EPaymentOrderState;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.mj.generated.server.model.ApiPromoType;
import ru.yandex.mj.generated.server.model.FixedDiscountArgsDto;
import ru.yandex.mj.generated.server.model.PromoAccessType;
import ru.yandex.mj.generated.server.model.PromoApplicationType;
import ru.yandex.mj.generated.server.model.PromoArgsDto;
import ru.yandex.mj.generated.server.model.PromoCommunicationArgsDto;
import ru.yandex.mj.generated.server.model.PromoCommunicationType;
import ru.yandex.mj.generated.server.model.PromoWithUsagesLeftDto;

import static ru.yandex.market.global.common.test.TestUtil.createCheckedUserTicket;
import static ru.yandex.market.global.common.test.TestUtil.mockRequestAttributes;
import static ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType.CHECKOUT;
import static ru.yandex.market.global.db.jooq.enums.EPromoCommunicationType.INFORMER;
import static ru.yandex.market.starter.tvm.filters.UserTicketFilter.CHECKED_USER_TICKET_ATTRIBUTE;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UserApiServiceTest extends BaseApiTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(TestPromoFactory.class).build();

    private final UserApiService userApiService;
    private final TestPromoFactory testPromoFactory;
    private final TestOrderFactory testOrderFactory;

    @Test
    public void testGetPromoWithoutTag() {
        Promo promoWithoutTag = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                )
                .build()
        );

        Promo promoWithTag = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)

                        .setTags("tag")
                )
                .build()
        );

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(1L)));
        List<PromoWithUsagesLeftDto> promoDtos = userApiService.apiV1UserAvailablePromosGet(List.of(), "ut", "tt")
                .getBody();

        Assertions.assertThat(promoDtos)
                .map(PromoWithUsagesLeftDto::getName)
                .containsExactly(promoWithoutTag.getName());
    }

    @Test
    public void testNotGettingPromoForFirstOrder() {
        Promo firstOrderPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIRST_ORDER_DISCOUNT.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                )
                .build()
        );

        Promo firstOrderNoAdultPromo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIRST_ORDER_DISCOUNT_NO_ADULT.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                )
                .build()
        );

        Promo fixedDiscount = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                )
                .build()
        );

        OrderModel order = testOrderFactory.createOrder(TestOrderFactory.CreateOrderBuilder.builder()
                .setupOrder(it -> it
                        .setOrderState(EOrderState.FINISHED)
                        .setDeliveryState(EDeliveryOrderState.ORDER_DELIVERED)
                        .setPaymentState(EPaymentOrderState.CLEARED)).build());

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE,
                createCheckedUserTicket(order.getOrder().getUid())));
        List<PromoWithUsagesLeftDto> promoDtos = userApiService.apiV1UserAvailablePromosGet(List.of(), "ut", "tt")
                .getBody();

        Assertions.assertThat(promoDtos).hasSize(1);
        Assertions.assertThat(promoDtos)
                .map(PromoWithUsagesLeftDto::getName)
                .containsExactly(fixedDiscount.getName());
    }

    @Test
    public void testGetFindPromoWithTag() {
        Promo promoWithoutTag = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                )
                .build()
        );

        Promo promoWithTag = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)

                        .setTags("tag1", "tag2")
                )
                .build()
        );

        mockRequestAttributes(Map.of(CHECKED_USER_TICKET_ATTRIBUTE, createCheckedUserTicket(1L)));
        List<PromoWithUsagesLeftDto> promoDtos = userApiService.apiV1UserAvailablePromosGet(
                List.of("tag2", "tag3"), "ut", "tt"
        ).getBody();

        Assertions.assertThat(promoDtos)
                .map(PromoWithUsagesLeftDto::getName)
                .containsExactly(promoWithoutTag.getName(), promoWithTag.getName());
    }

    @Test
    public void testGetFindPromoForAnonymous() {
        Promo promoWithoutTag = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setLimitedUsagesCount(1000)
                )
                .build()
        );

        Promo promoWithTag = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)

                        .setTags("tag1", "tag2")
                )
                .build()
        );

        List<PromoWithUsagesLeftDto> promoDtos = userApiService.apiV1UserAvailablePromosGet(
                List.of("tag2", "tag3"), "ut", "tt"
        ).getBody();

        Assertions.assertThat(promoDtos)
                .map(PromoWithUsagesLeftDto::getName)
                .containsExactly(promoWithoutTag.getName(), promoWithTag.getName());
    }

    @Test
    public void testGetFindPromoForCommunication() {
        Promo promoWithCommunication = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                )
                .build()
        );

        Promo promoWithoutCommunication = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FREE_DELIVERY_USER.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                )
                .setupCommunicationTypes(t -> null)
                .setupCommunicationArgs(a -> null)
                .build()
        );

        List<PromoWithUsagesLeftDto> promoDtos = userApiService.apiV1UserAvailablePromosGet(
                null, "ut", "tt"
        ).getBody();

        Assertions.assertThat(promoDtos)
                .map(PromoWithUsagesLeftDto::getName)
                .containsExactly(promoWithCommunication.getName());
    }

    @Test
    public void testGetReturnAllExpectedFields() {
        Promo promo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(PromoType.FIXED_DISCOUNT.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_LIMITED)
                        .setCommunicationTypes(CHECKOUT, INFORMER)
                        .setCommunicationArgs(RANDOM.nextObject(PromoCommunicationArgsDto.class))
                )
                .setupArgs((a) -> new FixedDiscountArgs()
                        .setBudget(100)
                        .setDiscount(50)
                        .setMinTotalItemsCost(0)
                )
                .build()
        );

        List<PromoWithUsagesLeftDto> promoWithUsagesLeft = userApiService.apiV1UserAvailablePromosGet(
                null, "ut", "tt"
        ).getBody();

        Assertions.assertThat(promoWithUsagesLeft)
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withIgnoreAllExpectedNullFields(true)
                        .withComparatorForType(
                                Comparator.comparing(OffsetDateTime::toEpochSecond), OffsetDateTime.class
                        )
                        .build()
                )
                .containsExactlyInAnyOrder(new PromoWithUsagesLeftDto()
                        .name(promo.getName())
                        .description(promo.getDescription())
                        .applicationType(PromoApplicationType.UNCONDITIONAL)
                        .accessType(PromoAccessType.ALL_LIMITED)
                        .type(ApiPromoType.FIXED_DISCOUNT)
                        .args(new PromoArgsDto()
                                .fixedDiscountArgs(new FixedDiscountArgsDto()
                                        .budget(100L)
                                        .discount(50L)
                                        .minTotalItemsCost(0L)
                                )
                        )
                        .communicationArgs(promo.getCommunicationArgs())
                        .communicationTypes(List.of(
                                PromoCommunicationType.CHECKOUT,
                                PromoCommunicationType.INFORMER
                        ))
                        .validTill(promo.getValidTill())
                );
    }

    @SneakyThrows
    @ParameterizedTest(name = "{0}")
    @EnumSource(PromoType.class)
    public void testAllArgTypesConverter(PromoType promoType) {
        if (promoType.getTarget() == PromoTarget.SHOP) {
            return;
        }

        Promo promo = testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setType(promoType.name())

                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                )
                .build()
        );

        List<PromoWithUsagesLeftDto> promoWithUsagesLeft = userApiService.apiV1UserAvailablePromosGet(
                null, "ut", "tt"
        ).getBody();

        String argsFieldName = StringUtils.uncapitalize(promoType.getArgsClass().getSimpleName());

        Assertions.assertThat(promoWithUsagesLeft).hasSize(1);
        //noinspection ConstantConditions
        PromoArgsDto args = promoWithUsagesLeft.get(0).getArgs();

        Assertions.assertThat(args)
                .hasAllNullFieldsOrPropertiesExcept(argsFieldName);

        Field declaredField = PromoArgsDto.class.getDeclaredField(argsFieldName);
        declaredField.setAccessible(true);

        Assertions.assertThat(declaredField.get(args))
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withIgnoredFields("area") //BigDecimal can't be compared with Double
                        .build())
                .isEqualTo(promo.getArgs());
    }
}
