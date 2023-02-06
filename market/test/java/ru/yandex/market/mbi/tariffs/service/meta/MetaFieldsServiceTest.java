package ru.yandex.market.mbi.tariffs.service.meta;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.model.AbstractField;
import ru.yandex.market.mbi.tariffs.model.AgencyCommissionJsonSchema;
import ru.yandex.market.mbi.tariffs.model.CancelledOrderFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.model.CashOnlyOrderJsonSchema;
import ru.yandex.market.mbi.tariffs.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.model.CourierFineJsonSchema;
import ru.yandex.market.mbi.tariffs.model.CourierShiftCommonJsonSchema;
import ru.yandex.market.mbi.tariffs.model.CourierShiftJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DisposalJsonSchema;
import ru.yandex.market.mbi.tariffs.model.DistributionJsonSchema;
import ru.yandex.market.mbi.tariffs.model.FfPartnerTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.FfStorageBillingMultiplierTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.FfStorageTurnoverJsonSchema;
import ru.yandex.market.mbi.tariffs.model.FixedTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.FulfillmentTariffsJsonSchema;
import ru.yandex.market.mbi.tariffs.model.GlobalAgencyCommissionJsonSchema;
import ru.yandex.market.mbi.tariffs.model.GlobalDeliveryJsonSchema;
import ru.yandex.market.mbi.tariffs.model.GlobalFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.model.IntakeTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.MinDailyTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.MinFeeJsonSchema;
import ru.yandex.market.mbi.tariffs.model.NumberField;
import ru.yandex.market.mbi.tariffs.model.PvzTariffBrandedDecorationJsonShema;
import ru.yandex.market.mbi.tariffs.model.PvzTariffFlatJsonSchema;
import ru.yandex.market.mbi.tariffs.model.PvzTariffRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ReturnedOrderStorageJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ScKGTRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ScMinimalRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ScRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.model.SortingTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.SupplierCategoryFeeTariffJsonSchema;
import ru.yandex.market.mbi.tariffs.model.XdocSupplyTariffJsonSchema;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты для {@link MetaFieldsService}
 */
@ParametersAreNonnullByDefault
public class MetaFieldsServiceTest extends FunctionalTest {

    @Autowired
    private MetaFieldsService metaFieldsService;

    private static final Map<Class<?>, Set<String>> EXCLUDED_FIELDS_MAP = new HashMap<>();

    @BeforeAll
    public static void setUp() {
        EXCLUDED_FIELDS_MAP.putAll(
                Map.of(
                        FfStorageBillingMultiplierTariffJsonSchema.class,
                        getVisibleFieldsReflectionMap(CommonJsonSchema.class).keySet()
                ));
    }

    @Test
    void testAllServiceTypesHaveFieldsSupplier() {
        List<ServiceTypeEnum> serviceTypeWithoutFields = Arrays.stream(ServiceTypeEnum.values())
                .filter(it -> metaFieldsService.getMetaFields(it) == null)
                .collect(Collectors.toList());

        assertThat(
                "ServiceTypes " + serviceTypeWithoutFields + " have not fields",
                serviceTypeWithoutFields,
                empty()
        );
    }

    @ParameterizedTest
    @MethodSource("allFieldsData")
    @DisplayName("Проверка, что metaFieldsService отдает все поля с правильным типом")
    <T extends CommonJsonSchema> void testAllFieldsHaveRightType(
            ServiceTypeEnum serviceType,
            Class<T> jsonSchemaClazz
    ) {
        List<AbstractField> metaFields = metaFieldsService.getMetaFields(serviceType);
        Map<String, Type> fieldsReflectionMap = getVisibleFieldsReflectionMap(jsonSchemaClazz);

        for (AbstractField metaField : metaFields) {
            String name = metaField.getName();
            Type fieldType = fieldsReflectionMap.remove(name);
            assertThat("The field " + name + " is not found", fieldType, notNullValue());

            checkFieldType(metaField, fieldType);
        }

        assertThat("The fields doesn't presented: " + fieldsReflectionMap,
                fieldsReflectionMap.keySet(),
                empty()
        );
    }

    @SuppressWarnings("checkstyle:MissingSwitchDefault")
    private void checkFieldType(AbstractField metaField, Type fieldType) {
        switch (metaField.getType()) {
            case NUMBER -> {
                Boolean isAllowFloat = ((NumberField) metaField).getAllowFloat();
                if (isAllowFloat) {
                    checkType(BigDecimal.class, fieldType, metaField);
                } else {
                    assertThat(
                            "Bad type for field [" + metaField.getName() + "]. Expected " + Integer.class.getSimpleName() + " or " + Long.class.getSimpleName() + " , actual : " + fieldType,
                            fieldType.getTypeName(),
                            anyOf(
                                    equalTo(Integer.class.getTypeName()),
                                    equalTo(Long.class.getTypeName())
                            )
                    );
                }
            }
            case STRING -> checkType(String.class, fieldType, metaField);
            case BOOLEAN -> checkType(Boolean.class, fieldType, metaField);
            case CATEGORYID -> checkType(Long.class, fieldType, metaField);
        }
    }

    private void checkType(Class<?> expectedType, Type actualType, AbstractField metaField) {
        Assertions.assertEquals(
                actualType.getTypeName(),
                expectedType.getTypeName(),
                "Bad type for field х" + metaField.getName() + "ъ. Expected " + expectedType.getSimpleName() + ", " +
                        "actual : " + actualType);
    }

    private static Map<String, Type> getVisibleFieldsReflectionMap(Class<?> clazz) {
        Class<?> currentClass = clazz;
        List<Field> list = new ArrayList<>();
        while (currentClass != null) {
            list.addAll(Arrays.asList(currentClass.getDeclaredFields()));
            currentClass = currentClass.getSuperclass();
        }
        Set<String> excludedFields = EXCLUDED_FIELDS_MAP.getOrDefault(clazz, Set.of());
        return list
                .stream()
                .filter(field -> !excludedFields.contains(field.getName()))
                .collect(Collectors.toMap(Field::getName, Field::getType));
    }

    @Test
    @DisplayName("Проверка, что allFieldsData содержит все нужные serviceType")
    void testAllFieldsDataContainsAllServiceTypes() {
        Set<ServiceTypeEnum> serviceTypesFromMethod = allFieldsData().map(args -> args.get()[0])
                .map(serviceType -> (ServiceTypeEnum) serviceType)
                .collect(Collectors.toSet());

        Set<ServiceTypeEnum> serviceTypeEnums = Set.copyOf(Arrays.asList(ServiceTypeEnum.values()));

        Sets.SetView<ServiceTypeEnum> difference = Sets.difference(serviceTypeEnums, serviceTypesFromMethod);
        assertThat(difference, empty());
    }

    private static Stream<Arguments> allFieldsData() {
        return Stream.of(
                Arguments.of(ServiceTypeEnum.DISTRIBUTION, DistributionJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_PARTNER, FfPartnerTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_XDOC_SUPPLY, XdocSupplyTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FIXED_TARIFFS, FixedTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_PROCESSING, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_BILLING, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        FfStorageBillingMultiplierTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_WITHDRAW, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.DELIVERY_TO_CUSTOMER, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.DELIVERY_TO_CUSTOMER_RETURN, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.INTAKE, IntakeTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.MIN_DAILY, MinDailyTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.MIN_FEE, MinFeeJsonSchema.class),
                Arguments.of(ServiceTypeEnum.RETURNED_ORDERS_STORAGE, ReturnedOrderStorageJsonSchema.class),
                Arguments.of(ServiceTypeEnum.SORTING, SortingTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FEE, SupplierCategoryFeeTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.LOYALTY_PARTICIPATION_FEE, SupplierCategoryFeeTariffJsonSchema.class),
                Arguments.of(ServiceTypeEnum.CASH_ONLY_ORDER, CashOnlyOrderJsonSchema.class),
                Arguments.of(ServiceTypeEnum.EXPRESS_DELIVERY, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.CANCELLED_EXPRESS_ORDER_FEE, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_BRANDED_DECORATION, PvzTariffBrandedDecorationJsonShema.class),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD, PvzTariffRewardJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD_YADO, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD_DBS, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_CARD_COMPENSATION, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_CASH_COMPENSATION, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_DBS_INCOME, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_DBS_OUTCOME, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_DROPOFF, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_DROPOFF_RETURN, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.PVZ_RETURN, PvzTariffFlatJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_DROPOFF_RETURN, CourierShiftCommonJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_BULKY_CARGO, CourierShiftCommonJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_SMALL_GOODS, CourierShiftCommonJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_WITHDRAW, CourierShiftCommonJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_YANDEX_DRIVE, CourierShiftCommonJsonSchema.class),
                Arguments.of(ServiceTypeEnum.SC_KGT_REWARD, ScKGTRewardJsonSchema.class),
                Arguments.of(ServiceTypeEnum.SC_REWARD, ScRewardJsonSchema.class),
                Arguments.of(ServiceTypeEnum.SC_MINIMAL_REWARD, ScMinimalRewardJsonSchema.class),
                Arguments.of(ServiceTypeEnum.CANCELLED_ORDER_FEE, CancelledOrderFeeJsonSchema.class),
                Arguments.of(ServiceTypeEnum.CROSSREGIONAL_DELIVERY, FulfillmentTariffsJsonSchema.class),
                Arguments.of(ServiceTypeEnum.SELF_REQUESTED_DISPOSAL, DisposalJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_FINE, CourierFineJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_SHIFT, CourierShiftJsonSchema.class),
                Arguments.of(ServiceTypeEnum.COURIER_VELO_SHIFT, CourierShiftCommonJsonSchema.class),
                Arguments.of(ServiceTypeEnum.GLOBAL_DELIVERY, GlobalDeliveryJsonSchema.class),
                Arguments.of(ServiceTypeEnum.GLOBAL_AGENCY_COMMISSION, GlobalAgencyCommissionJsonSchema.class),
                Arguments.of(ServiceTypeEnum.GLOBAL_FEE, GlobalFeeJsonSchema.class),
                Arguments.of(ServiceTypeEnum.AGENCY_COMMISSION, AgencyCommissionJsonSchema.class),
                Arguments.of(ServiceTypeEnum.FF_STORAGE_TURNOVER, FfStorageTurnoverJsonSchema.class)
        );
    }
}
