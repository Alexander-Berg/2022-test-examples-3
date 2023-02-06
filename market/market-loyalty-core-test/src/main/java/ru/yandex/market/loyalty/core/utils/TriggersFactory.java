package ru.yandex.market.loyalty.core.utils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ru.yandex.market.loyalty.core.dao.trigger.TriggerDao;
import ru.yandex.market.loyalty.core.model.accounting.BaseAccount;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.Trigger;
import ru.yandex.market.loyalty.core.model.trigger.TriggerAction;
import ru.yandex.market.loyalty.core.model.trigger.TriggerGroupType;
import ru.yandex.market.loyalty.core.model.trigger.TriggerMapper;
import ru.yandex.market.loyalty.core.model.trigger.TriggerRestriction;
import ru.yandex.market.loyalty.core.model.trigger.event.BaseTriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.EventWithRegion;
import ru.yandex.market.loyalty.core.model.trigger.event.EventWithUserData;
import ru.yandex.market.loyalty.core.model.trigger.event.ForceCreateCouponEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.ForceEmmitCouponEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.LoginEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusPredicate;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.SubscriptionEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventType;
import ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes;
import ru.yandex.market.loyalty.core.model.trigger.event.data.TriggerEventData;
import ru.yandex.market.loyalty.core.service.trigger.TriggerManagementService;
import ru.yandex.market.loyalty.core.service.trigger.coordinator.LoginEventProcessingCoordinator;
import ru.yandex.market.loyalty.core.service.trigger.coordinator.OrderStatusUpdatedEventProcessingCoordinator;
import ru.yandex.market.loyalty.core.trigger.actions.BrokenActionFactory;
import ru.yandex.market.loyalty.core.trigger.actions.CreateCoinAction;
import ru.yandex.market.loyalty.core.trigger.actions.EmmitCouponActionFactory;
import ru.yandex.market.loyalty.core.trigger.actions.SendCouponActionDto;
import ru.yandex.market.loyalty.core.trigger.actions.SendCouponByEmailActionFactory;
import ru.yandex.market.loyalty.core.trigger.actions.SendCouponByIdentityActionFactory;
import ru.yandex.market.loyalty.core.trigger.actions.TriggerActionType;
import ru.yandex.market.loyalty.core.trigger.actions.TriggerActionTypes;
import ru.yandex.market.loyalty.core.trigger.restrictions.AmountRangeDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.BlackboxInfoRestrictionDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.ClientDeviceRestrictionFactory;
import ru.yandex.market.loyalty.core.trigger.restrictions.ExperimentsDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.LoyalUserRestrictionDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.OrderStatusRestrictionFactory;
import ru.yandex.market.loyalty.core.trigger.restrictions.SetRelation;
import ru.yandex.market.loyalty.core.trigger.restrictions.SetWithRelationDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.actiononce.ActionOnceRestrictionType;
import ru.yandex.market.loyalty.core.trigger.restrictions.negate.NegateDto;
import ru.yandex.market.loyalty.core.trigger.restrictions.noauth.AuthType;
import ru.yandex.market.loyalty.lightweight.ExceptionUtils;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.loyalty.core.model.trigger.RestrictionDescription.byDefault;
import static ru.yandex.market.loyalty.core.model.trigger.RestrictionDescription.notRequired;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.FORCE_CREATE_COUPON;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.FORCE_EMMIT_COUPON;
import static ru.yandex.market.loyalty.core.model.trigger.event.TriggerEventTypes.ORDER_STATUS_UPDATED;
import static ru.yandex.market.loyalty.core.trigger.restrictions.ExperimentRestrictionFactory.EXPERIMENT_RESTRICTION_FACTORY;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ACTION_ONCE_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.AUTH_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.BLACKBOX_INFO_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.CLIENT_DEVICE_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.EXPERIMENT_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.LOYAL_USER_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.NEGATE_EXPERIMENT_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.NEGATE_REGION_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_AMOUNT_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_CATEGORY_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_MSKU_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_STATUS_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_SUPPLIERS_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.ORDER_VENDOR_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.PLATFORM_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.PREPAID_ORDER_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.PROMO_EMISSION_BUDGET_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.PROMO_EMISSION_DATE_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.REFUSAL_PREDICTED_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.REGION_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.SILENCE_GAP_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.TriggerRestrictionType.USER_SEGMENTS_RESTRICTION;
import static ru.yandex.market.loyalty.core.trigger.restrictions.region.RegionRestrictionFactory.REGION_RESTRICTION_FACTORY;
import static ru.yandex.market.loyalty.lightweight.ExceptionUtils.makeExceptionsUnchecked;

@Component
public class TriggersFactory {
    public static final String EMAIL_TEMPLATE_ID = "1234";
    private static final Trigger<?> MOCKED_TRIGGER = mock(Trigger.class);

    private static final TriggerActionType<BrokenActionFactory.BrokenAction> BROKEN_ACTION = new TriggerActionType<>(
            BrokenActionFactory.BrokenAction.class,
            BrokenActionFactory.BROKEN_ACTION_FACTORY,
            false
    );
    public static final TriggerEventType<BrokenLoginEvent> BROKEN_LOGIN = new TriggerEventType<>(
            "BROKEN_LOGIN", BrokenLoginEvent.class, BrokenLoginEvent::new,
            ImmutableList.of(
                    byDefault(PLATFORM_RESTRICTION),
                    byDefault(PROMO_EMISSION_DATE_RESTRICTION),
                    byDefault(PROMO_EMISSION_BUDGET_RESTRICTION),
                    notRequired(REGION_RESTRICTION)
            ),
            TriggerEventType.possibleActions(BROKEN_ACTION),
            LoginEventProcessingCoordinator.COORDINATOR_TYPE
    );

    @Autowired
    private TriggerUtils triggerUtils;
    @Autowired
    private TriggerManagementService triggerManagementService;
    @Autowired
    @TriggerMapper
    private ObjectMapper objectMapper;
    @Autowired
    private TriggerDao triggerDao;
    @Autowired
    private DiscountUtils discountUtils;

    static {
        given(MOCKED_TRIGGER.getId()).willReturn(0L);
    }

    public Trigger<ForceCreateCouponEvent> createForceCreateCouponTrigger(Promo promo) {
        return buildTrigger(promo, FORCE_CREATE_COUPON, sendByEmailAction());
    }

    public Trigger<ForceEmmitCouponEvent> createForceEmmitCouponTrigger(Promo promo) {
        return buildTrigger(promo, FORCE_EMMIT_COUPON, emmitCouponAction());
    }

    public Trigger<LoginEvent> createLoginTrigger(Promo promo) {
        return createLoginTrigger(promo, null, TriggerGroupType.MANDATORY_TRIGGERS);
    }

    public Trigger<LoginEvent> createLoginTrigger(Promo promo, Long regionId, TriggerGroupType triggerGroupType) {
        Trigger.Builder<LoginEvent> builder = Trigger.builder(TriggerEventTypes.LOGIN)
                .setPromoId(promo.getId())
                .setAction(sendByIdentityAction())
                .setTriggerGroupType(triggerGroupType);
        if (regionId != null) {
            builder.addRestriction(triggerUtils.getRestrictionFactory(REGION_RESTRICTION)
                    .create(null, regionId.toString()));
        }
        return triggerManagementService.addTrigger(builder.build());
    }

    public Trigger<BrokenLoginEvent> brokenLoginEventTrigger(Promo promo) {
        Trigger.Builder<BrokenLoginEvent> builder = Trigger.builder(BROKEN_LOGIN)
                .setPromoId(promo.getId())
                .setAction(brokenSendByIdentityAction())
                .setTriggerGroupType(TriggerGroupType.MANDATORY_TRIGGERS);
        return triggerManagementService.addTrigger(builder.build());
    }


    public static Customizer<OrderStatusUpdatedEvent> orderRestriction(OrderStatusPredicate status) {
        return restriction(f -> f.triggerUtils.getRestrictionFactory(ORDER_STATUS_RESTRICTION)
                .create(null, status.getCoreOrderStatus().getCode()));
    }

    @SuppressWarnings("unchecked")
    @SafeVarargs
    public final Trigger<OrderStatusUpdatedEvent> createOrderStatusUpdatedTrigger(
            Promo promo, TriggerAction<? super OrderStatusUpdatedEvent> action, TriggerGroupType mandatoryTriggers,
            Customizer<? super OrderStatusUpdatedEvent>... customisation
    ) {
        return buildTrigger(promo, ORDER_STATUS_UPDATED, action, mandatoryTriggers, Stream.concat(
                Arrays.stream(customisation),
                Stream.of(defaultOrderStatusRestriction())
        ).toArray(Customizer[]::new));
    }

    @SafeVarargs
    public final Trigger<OrderStatusUpdatedEvent> createOrderStatusUpdatedTriggerForCoin(
            Promo promo, Customizer<? super OrderStatusUpdatedEvent>... customisation
    ) {
        return createOrderStatusUpdatedTriggerForCoin(promo, TriggerGroupType.MANDATORY_TRIGGERS, customisation);
    }

    @SafeVarargs
    public final Trigger<OrderStatusUpdatedEvent> createOrderStatusUpdatedTriggerForCoin(
            Promo promo, TriggerGroupType mandatoryTriggers,
            Customizer<? super OrderStatusUpdatedEvent>... customisation
    ) {
        return createOrderStatusUpdatedTrigger(promo, createCoinAction("{}"), mandatoryTriggers, customisation);
    }

    @SafeVarargs
    public final Trigger<OrderStatusUpdatedEvent> createOrderStatusUpdatedTriggerForCoin(
            Promo promo, TriggerGroupType mandatoryTriggers, String actionBody,
            Customizer<? super OrderStatusUpdatedEvent>... customisation
    ) {
        return createOrderStatusUpdatedTrigger(promo, createCoinAction(actionBody), mandatoryTriggers, customisation);
    }

    public static <T extends BaseTriggerEvent> Customizer<T> groupType(
            TriggerGroupType triggerGroupType
    ) {
        return (b, tf) -> b.setTriggerGroupType(triggerGroupType);
    }

    @SafeVarargs
    private <T extends BaseTriggerEvent> Trigger<T> buildTrigger(
            Promo promo, TriggerEventType<T> eventType, TriggerAction<? super T> action,
            Customizer<? super T>... customisation
    ) {
        return buildTrigger(promo, eventType, action, TriggerGroupType.MANDATORY_TRIGGERS, customisation);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @SafeVarargs
    private <T extends BaseTriggerEvent> Trigger<T> buildTrigger(
            Promo promo, TriggerEventType<T> eventType, TriggerAction<? super T> action,
            TriggerGroupType triggerGroupType,
            Customizer<? super T>... customisation
    ) {
        return makeExceptionsUnchecked(() -> {
            Trigger.Builder<T> builder = Trigger.builder(eventType)
                    .setPromoId(promo.getId())
                    .setAction(action)
                    .setTriggerGroupType(triggerGroupType);

            for (Customizer<? super T> customizer : customisation) {
                customizer.customization(builder, this);
            }

            Trigger<T> trigger = builder.build();
            Trigger<T> result = triggerManagementService.addTrigger(trigger);
            return (Trigger) triggerDao.getTriggerById(result.getId());
        });
    }

    public static Customizer<OrderStatusUpdatedEvent> orderAmountRestriction(
            BigDecimal from, BigDecimal to
    ) {
        return restriction(f -> f.triggerUtils.getRestrictionFactory(ORDER_AMOUNT_RESTRICTION)
                .create(null, f.objectMapper.writeValueAsString(new AmountRangeDto(from, to))));
    }

    public static Customizer<OrderStatusUpdatedEvent> loyalUserRestriction(
            AmountRangeDto ordersWithCoins, AmountRangeDto percentOfRevoked
    ) {
        return restriction(f -> f.triggerUtils.getRestrictionFactory(LOYAL_USER_RESTRICTION)
                .create(null, f.objectMapper.writeValueAsString(new LoyalUserRestrictionDto(
                        ordersWithCoins,
                        percentOfRevoked
                ))));
    }

    public Trigger<OrderStatusUpdatedEvent> createCoinTriggerWithBlackboxRestriction(
            Promo promo,
            BlackboxInfoRestrictionDto blackboxInfoRestrictionDto
    ) {
        return createOrderStatusUpdatedTriggerForCoin(promo, blackboxRestriction(blackboxInfoRestrictionDto));
    }

    public Trigger<OrderStatusUpdatedEvent> createCoinTriggerWithClientDeviceRestriction(
            Promo promo,
            ClientDeviceRestrictionFactory.ClientDeviceRestrictionDto clientDeviceRestrictionDto
    ) {
        return createOrderStatusUpdatedTriggerForCoin(promo, clientDeviceRestriction(clientDeviceRestrictionDto));
    }

    public Trigger<OrderStatusUpdatedEvent> createCoinTriggerWithPrepaidOrderRestriction(
            Promo promo
    ) {
        return createOrderStatusUpdatedTriggerForCoin(promo, prepaidOrderRestriction());
    }

    public Trigger<OrderStatusUpdatedEvent> createCoinTriggerWithRefusalPredictedRestriction(
            Promo promo
    ) {
        return createOrderStatusUpdatedTriggerForCoin(promo, refusalPredictedRestriction());
    }

    private static Customizer<OrderStatusUpdatedEvent> blackboxRestriction(
            BlackboxInfoRestrictionDto blackboxInfoRestrictionDto
    ) {
        return restriction(
                f -> f.triggerUtils.getRestrictionFactory(BLACKBOX_INFO_RESTRICTION)
                        .create(null, blackboxInfoRestrictionDto.toJson(f.objectMapper))
        );
    }

    private static Customizer<OrderStatusUpdatedEvent> clientDeviceRestriction(
            ClientDeviceRestrictionFactory.ClientDeviceRestrictionDto clientDeviceRestrictionDto
    ) {
        return restriction(
                f -> f.triggerUtils.getRestrictionFactory(CLIENT_DEVICE_RESTRICTION)
                        .create(null, clientDeviceRestrictionDto.toJson(f.objectMapper))
        );
    }

    public static Customizer<OrderStatusUpdatedEvent> prepaidOrderRestriction() {
        return restriction(
                factory -> factory.triggerUtils.getRestrictionFactory(PREPAID_ORDER_RESTRICTION)
                        .create(null, null)
        );
    }

    public static Customizer<OrderStatusUpdatedEvent> refusalPredictedRestriction() {
        return restriction(
                factory -> factory.triggerUtils.getRestrictionFactory(REFUSAL_PREDICTED_RESTRICTION)
                        .create(null, null)
        );
    }

    public static Customizer<OrderStatusUpdatedEvent> categoryRestriction(
            int... categoryIds
    ) {
        return categoryRestriction(BigDecimal.ZERO, categoryIds);
    }

    public static Customizer<OrderStatusUpdatedEvent> categoryRestriction(
            BigDecimal minTotal, int... categoryIds
    ) {
        return restriction(f -> {
            SetWithRelationDto<Integer> setWithRelationDto = new SetWithRelationDto<>();
            setWithRelationDto.setGivenSet(Arrays.stream(categoryIds).boxed().collect(Collectors.toSet()));
            setWithRelationDto.setSetRelation(SetRelation.ALL_INCLUDED_IN_SET);
            setWithRelationDto.setMinTotal(minTotal);
            return f.triggerUtils.getRestrictionFactory(ORDER_CATEGORY_RESTRICTION)
                    .create(null, f.objectMapper.writeValueAsString(setWithRelationDto));
        });
    }

    public static Customizer<OrderStatusUpdatedEvent> vendorsRestriction(
            long... vendorIds
    ) {
        return vendorsRestriction(BigDecimal.ZERO, vendorIds);
    }

    public static Customizer<OrderStatusUpdatedEvent> vendorsRestriction(
            BigDecimal minTotal, long... vendorIds
    ) {
        return restriction(f -> {
            SetWithRelationDto<Long> setWithRelationDto = new SetWithRelationDto<>();
            setWithRelationDto.setGivenSet(Arrays.stream(vendorIds).boxed().collect(Collectors.toSet()));
            setWithRelationDto.setSetRelation(SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED);
            setWithRelationDto.setMinTotal(minTotal);
            return f.triggerUtils.getRestrictionFactory(ORDER_VENDOR_RESTRICTION)
                    .create(null, f.objectMapper.writeValueAsString(setWithRelationDto));
        });
    }

    public static Customizer<OrderStatusUpdatedEvent> suppliersRestriction(long... supplierIds) {
        return suppliersRestriction(BigDecimal.ZERO, supplierIds);
    }

    public static Customizer<OrderStatusUpdatedEvent> suppliersRestriction(BigDecimal minTotal, long... supplierIds) {
        return restriction(f -> {
            SetWithRelationDto<Long> setWithRelationDto = new SetWithRelationDto<>();
            setWithRelationDto.setGivenSet(Arrays.stream(supplierIds).boxed().collect(Collectors.toSet()));
            setWithRelationDto.setSetRelation(SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED);
            setWithRelationDto.setMinTotal(minTotal);
            return f.triggerUtils.getRestrictionFactory(ORDER_SUPPLIERS_RESTRICTION)
                    .create(null, f.objectMapper.writeValueAsString(setWithRelationDto));
        });
    }

    public Trigger<OrderStatusUpdatedEvent> createOrderMskuTrigger(Promo promo, String... mskus) {
        return createOrderMskuTrigger(promo, BigDecimal.ZERO, null, mskus);
    }

    public Trigger<OrderStatusUpdatedEvent> createOrderMskuTrigger(
            Promo promo, BigDecimal minTotal, BigDecimal maxTotal, String... mskus
    ) {
        return buildTrigger(promo, ORDER_STATUS_UPDATED, sendByEmailAction(),
                defaultOrderStatusRestriction(),
                mskuRestriction(minTotal, null, mskus)
        );
    }

    public static Customizer<OrderStatusUpdatedEvent> mskuRestriction(
            String... mskus
    ) {
        return mskuRestriction(BigDecimal.ZERO, null, mskus);
    }

    public static Customizer<OrderStatusUpdatedEvent> mskuRestriction(
            BigDecimal minTotal, BigDecimal maxTotal, String... mskus
    ) {
        return restriction(f -> {
            SetWithRelationDto<String> setWithRelationDto = new SetWithRelationDto<>();
            setWithRelationDto.setGivenSet(Arrays.stream(mskus).collect(Collectors.toSet()));
            setWithRelationDto.setSetRelation(SetRelation.ALL_FROM_SET_SHOULD_BE_INCLUDED);
            setWithRelationDto.setMinTotal(minTotal);
            setWithRelationDto.setMaxTotal(maxTotal);
            return f.triggerUtils.getRestrictionFactory(ORDER_MSKU_RESTRICTION)
                    .create(null, f.objectMapper.writeValueAsString(setWithRelationDto));
        });
    }

    private static Customizer<OrderStatusUpdatedEvent> defaultOrderStatusRestriction() {
        return (b, tu) -> {
            boolean alreadyHasRestriction = b.getRestrictions().stream()
                    .anyMatch(r -> r instanceof OrderStatusRestrictionFactory.OrderStatusRestriction);
            if (!alreadyHasRestriction) {
                b.addRestriction(
                        tu.triggerUtils.getRestrictionFactory(ORDER_STATUS_RESTRICTION)
                                .create(null,
                                        EventFactory.DEFAULT_ORDER_STATUS_PREDICATE.getCoreOrderStatus().getCode())
                );
            }
        };
    }

    public Trigger<SubscriptionEvent> createSubscriptionTrigger(Promo promo) {
        return triggerManagementService.addTrigger(Trigger.builder(TriggerEventTypes.SUBSCRIPTION)
                .setPromoId(promo.getId())
                .setAction(sendByEmailAction())
                .setTriggerGroupType(TriggerGroupType.MANDATORY_TRIGGERS)
                .build());
    }

    public <T extends BaseTriggerEvent & EventWithRegion> Trigger<T> createSilenceGapRestriction(
            Trigger<T> trigger, Integer silenceGapHours
    ) {
        trigger = Trigger.builder(trigger, null)
                .addRestriction(triggerUtils
                        .getRestrictionFactory(SILENCE_GAP_RESTRICTION)
                        .create(null, silenceGapHours.toString()))
                .build();
        return triggerManagementService.updateTrigger(trigger);
    }

    private EmmitCouponActionFactory.EmmitCouponAction emmitCouponAction() {
        return makeExceptionsUnchecked(() -> triggerUtils.getActionFactory(TriggerActionTypes.EMMIT_COUPON_ACTION)
                .create(null, ""));
    }

    public SendCouponByEmailActionFactory.SendCouponByEmailAction sendByEmailAction() {
        return makeExceptionsUnchecked(
                () -> triggerUtils.getActionFactory(TriggerActionTypes.SEND_COUPON_BY_EMAIL_ACTION)
                        .create(null, objectMapper.writeValueAsString(new SendCouponActionDto(EMAIL_TEMPLATE_ID))));
    }

    public CreateCoinAction createCoinAction(String actionBody) {
        return makeExceptionsUnchecked(() -> triggerUtils.getActionFactory(TriggerActionTypes.CREATE_COIN_ACTION)
                .create(null, actionBody));
    }

    private SendCouponByIdentityActionFactory.SendCouponByIdentityAction sendByIdentityAction() {
        return makeExceptionsUnchecked(
                () -> triggerUtils.getActionFactory(TriggerActionTypes.SEND_COUPON_BY_IDENTITY_ACTION)
                        .create(null, objectMapper.writeValueAsString(new SendCouponActionDto(EMAIL_TEMPLATE_ID))));
    }

    private BrokenActionFactory.BrokenAction brokenSendByIdentityAction() {
        return makeExceptionsUnchecked(() -> triggerUtils.getActionFactory(BROKEN_ACTION)
                .create(null, ""));
    }

    public static Customizer<EventWithUserData> actionOnceRestriction() {
        return actionOnceRestriction(ActionOnceRestrictionType.CHECK_USER);
    }

    public static Customizer<EventWithUserData> authOnceRestriction(AuthType type) {
        return restriction(
                f -> f.triggerUtils.getRestrictionFactory(AUTH_RESTRICTION).create(null, type.getCode()));
    }

    public static Customizer<OrderStatusUpdatedEvent> userSegmentsRestriction(
            SetRelation setRelation, String... segments
    ) {
        return restriction(
                f -> {
                    SetWithRelationDto<String> setWithRelationDto = new SetWithRelationDto<>();
                    setWithRelationDto.setGivenSet(Arrays.stream(segments).collect(Collectors.toSet()));
                    setWithRelationDto.setSetRelation(setRelation);
                    setWithRelationDto.setMinTotal(BigDecimal.ZERO);
                    return f.triggerUtils.getRestrictionFactory(USER_SEGMENTS_RESTRICTION).create(
                            null, f.objectMapper.writeValueAsString(setWithRelationDto));
                });
    }

    public static Customizer<EventWithUserData> actionOnceRestriction(ActionOnceRestrictionType type) {
        return restriction(f -> f.triggerUtils.getRestrictionFactory(ACTION_ONCE_RESTRICTION)
                .create(null, "{ \"type\": \"" + type.getCode() + "\"}"));
    }

    private static <T extends TriggerEvent> Customizer<T> restriction(
            ExceptionUtils.FunctionWithException<TriggersFactory, TriggerRestriction<? super T>, Exception> supplier
    ) {
        return (b, tu) -> b.addRestriction(supplier.apply(tu));
    }

    @NotNull
    public static Customizer<OrderStatusUpdatedEvent> experimentRestriction(ExperimentsDto experiments) {
        return restriction(
                f -> f.triggerUtils.getRestrictionFactory(EXPERIMENT_RESTRICTION)
                        .create(null, f.objectMapper.writeValueAsString(experiments))
        );
    }

    public static Customizer<OrderStatusUpdatedEvent> negateExperimentRestriction(ExperimentsDto experiments) {
        return restriction(
                f -> f.triggerUtils.getRestrictionFactory(NEGATE_EXPERIMENT_RESTRICTION).create(
                        null, f.objectMapper.writeValueAsString(
                                new NegateDto(EXPERIMENT_RESTRICTION_FACTORY,
                                        f.objectMapper.writeValueAsString(experiments))
                        )
                )
        );
    }

    public static Customizer<OrderStatusUpdatedEvent> regionRestriction(String regions) {
        return restriction(tf -> tf.triggerUtils.getRestrictionFactory(REGION_RESTRICTION).create(null, regions));
    }


    public static Customizer<OrderStatusUpdatedEvent> negateRegionRestriction(String regions) {
        return restriction(tf ->
                tf.triggerUtils.getRestrictionFactory(NEGATE_REGION_RESTRICTION).create(
                        null, tf.objectMapper.writeValueAsString(new NegateDto(REGION_RESTRICTION_FACTORY, regions)))
        );
    }

    public TriggerEventData<?> createDefaultPromoTriggerEventData(Promo promo) {
        return TriggerEventData.createTriggerEventData(
                MOCKED_TRIGGER,
                promo,
                BaseAccount.DUMB,
                discountUtils.getRulesPayload()
        );
    }

    public interface Customizer<T extends TriggerEvent> {
        void customization(Trigger.Builder<? extends T> builder, TriggersFactory triggersFactory) throws Exception;
    }
}
