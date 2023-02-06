package ru.yandex.market.mbi.tariffs.service.meta;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.type.TypeFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbi.tariffs.FunctionalTest;
import ru.yandex.market.mbi.tariffs.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.service.meta.converter.AbstractMetaConverter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;

/**
 * Тесты для {@link MetaConverterService}
 */
@ParametersAreNonnullByDefault
public class MetaConverterServiceTest extends FunctionalTest {

    private static final String GENERAL_FAILED_DATA = "general";
    private static final String FULFILLMENT_FAILED_DATA = "fulfillment";
    private static final String FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA = "multiplier";
    private static final String RETURNED_ORDERS_STORAGE_FAILED_DATA = "returnedOrdersStorage";
    private static final String MIN_FEE_FAILED_DATA = "minFee";
    private static final String SORTING_FAILED_DATA = "sorting";
    private static final String MIN_DAILY_FAILED_DATA = "minDaily";
    private static final String FF_XDOC_SUPPLY_FAILED_DATA = "ffXdocSupply";
    private static final String SUPPLIER_FEE_FAILED_DATA = "supplierFee";
    private static final String DISPOSAL_FAILED_DATA = "disposal";
    private static final String CANCELLED_EXPRESS_ORDER_FEE_FAILED_DATA = "cancelledExpressOrderFee";
    private static final String CANCELLED_ORDER_FEE_FAILED_DATA = "cancelledOrderFee";
    private static final String EXPRESS_DELIVERY_FAILED_DATA = "expressDelivery";
    private static final String CASH_ONLY_ORDER_FAILED_DATA = "cashOnlyOrder";
    private static final String COURIER_SHIFT_FAILED_DATA = "courierShift";
    private static final String PVZ_REWARD_FAILED_DATA = "pvzReward";
    private static final String PVZ_FLAT_FAILED_DATA = "pvzFlat";
    private static final String PVZ_BRANDED_DECORATION_FAILED_DATA = "pvzBrandedDecoration";
    private static final String COURIER_SHIFT_COMMON_FAILED_DATA = "courierCommon";
    private static final String AGENCY_COMMISSION_FAILED_DATA = "agencyCommission";

    static final String UNKNOWN_BILLING_UNIT = "Unknown billingUnit. Available : ";
    @Autowired
    private MetaConverterService metaConverterService;

    @Test
    @DisplayName("Тест на то, что все ServiceType имеют конвертеры")
    void testAllServicesHaveConverters() {
        List<ServiceTypeEnum> servicesWithoutDeserializers = Arrays.stream(ServiceTypeEnum.values())
                .filter(serviceType -> metaConverterService.getConverter(serviceType) == null)
                .collect(Collectors.toList());

        assertThat(
                "Service types " + servicesWithoutDeserializers + " have not converter",
                servicesWithoutDeserializers,
                hasSize(0)
        );
    }

    @DisplayName("Проверка десериализации меты")
    @ParameterizedTest(name = "[{index}] проверка меты для {0}")
    @MethodSource("deserializeMetaData")
    void testDeserialize(
            ServiceTypeEnum serviceType,
            String json
    ) throws Exception {
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        List<Object> listMeta = objectMapper.readValue(json, typeFactory.constructCollectionType(List.class,
                Object.class));
        AbstractMetaConverter<?> converter = metaConverterService.getConverter(serviceType);
        converter.deserialize(listMeta);
    }

    private static Stream<Arguments> deserializeMetaData() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.DISTRIBUTION,
                        /*language=json*/ "" +
                                "[{" +
                                "   \"info\": \"test\", " +
                                "   \"type\": \"relative\", " +
                                "   \"color\": \"blue\", " +
                                "   \"amount\": 0.018, " +
                                "   \"orderStatus\": \"delivered\", " +
                                "   \"currency\": \"RUB\", " +
                                "   \"categoryId\": 198119, " +
                                "   \"tariffName\": \"CEHAC\", " +
                                "   \"billingUnit\":  \"order\"" +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "" +
                                "[ " +
                                "  { " +
                                "    \"amount\": 800, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\":  \"item\"," +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": 125000, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 10000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\":  \"item\"," +
                                "    \"ordinal\": 2, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 35000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\":  \"item\"," +
                                "    \"ordinal\": 3, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 250, " +
                                "    \"weightTo\": 40000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 40000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\":  \"item\"," +
                                "    \"ordinal\": 4, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 350, " +
                                "    \"weightTo\": 100000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 0, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\":  \"item\"," +
                                "    \"ordinal\": 5, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  } " +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_WITHDRAW,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 1200, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  } " +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 40, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": 125000, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 40, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 2, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 500, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 3, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 250, " +
                                "    \"weightTo\": 40000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 500, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 4, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 350, " +
                                "    \"weightTo\": 100000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 0, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 5, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  } " +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 0, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"categoryId\": 198119, " +
                                "    \"daysOnStockFrom\": 0, " +
                                "    \"daysOnStockTo\": 60, " +
                                "    \"multiplier\": 0 " +
                                "  }, " +
                                "  { " +
                                "    \"categoryId\": 198119, " +
                                "    \"daysOnStockFrom\": 60, " +
                                "    \"daysOnStockTo\": 90, " +
                                "    \"multiplier\": 1 " +
                                "  }, " +
                                "  { " +
                                "    \"categoryId\": 198119, " +
                                "    \"daysOnStockFrom\": 90, " +
                                "    \"daysOnStockTo\": 120, " +
                                "    \"multiplier\": 3.6 " +
                                "  }, " +
                                "  { " +
                                "    \"categoryId\": 90401, " +
                                "    \"daysOnStockFrom\": 100, " + // пересекаются в разных категориях [90, 120)[100, 1000)
                                "    \"daysOnStockTo\": 1000, " +
                                "    \"multiplier\": 5 " +
                                "  } " +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.DELIVERY_TO_CUSTOMER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": 125000, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 2, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 3, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 250, " +
                                "    \"weightTo\": 40000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  " +
                                "{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 4, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  } " +
                                "]"),
                Arguments.of(
                        ServiceTypeEnum.DELIVERY_TO_CUSTOMER_RETURN,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": 125000, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 2, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 3, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 250, " +
                                "    \"weightTo\": 40000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 4, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null " +
                                " " +
                                " } " +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FIXED_TARIFFS,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.GLOBAL_AGENCY_COMMISSION,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 250, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.GLOBAL_DELIVERY,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 900, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.INTAKE,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"cubic_meter\"," +
                                "    \"isActive\" : true," +
                                "    \"volumeFrom\" : 100, " +
                                "    \"volumeTo\" : 200 " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_DAILY,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"daily\"" +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"billingUnit\": \"item\"," +
                                "    \"categoryId\" : 198119 " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_PARTNER,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\"," +
                                "    \"warehouseId\" : 123, " +
                                "    \"title\" : \"some title\"" +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "" +
                                "[" +
                                "  {" +
                                "    \"amount\": 300," +
                                "    \"type\": \"relative\"," +
                                "    \"currency\": \"RUB\"," +
                                "    \"billingUnit\": \"item\"," +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 90509," +
                                "    \"isExpress\": false" +
                                "  }," +
                                "  {" +
                                "    \"amount\": 300," +
                                "    \"type\": \"relative\"," +
                                "    \"currency\": \"RUB\"," +
                                "    \"billingUnit\": \"item\"," +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 90666," +
                                "    \"isExpress\": false" +
                                "  }," +
                                "  {" +
                                "    \"amount\": 300," +
                                "    \"type\": \"relative\"," +
                                "    \"currency\": \"RUB\"," +
                                "    \"billingUnit\": \"item\"," +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 198119," +
                                "    \"isExpress\": false" +
                                "  }," +
                                "  {" +
                                "    \"amount\": 300," +
                                "    \"type\": \"relative\"," +
                                "    \"currency\": \"RUB\"," +
                                "    \"billingUnit\": \"item\"," +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 198119," +
                                "    \"isExpress\": true" +
                                "  }" +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.GLOBAL_FEE,
                        /*language=json*/ "" +
                                "[{" +
                                "    \"amount\": 300," +
                                "    \"type\": \"relative\"," +
                                "    \"currency\": \"RUB\"," +
                                "    \"billingUnit\": \"item\"," +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 198119," +
                                "    \"isExpress\": true" +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.LOYALTY_PARTICIPATION_FEE,
                        /*language=json*/ "" +
                                "[{" +
                                "    \"amount\": 0, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 90401," +
                                "    \"isExpress\": false" +
                                "  }," +
                                "{" +
                                "    \"amount\": 0, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"orderStatus\": \"delivered\", " +
                                "    \"categoryId\": 90401," +
                                "    \"isExpress\": true" +
                                "  }" +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/ "" +
                                "[ " +
                                "    { " +
                                "      \"amount\": 250, " +
                                "      \"type\": \"absolute\", " +
                                "      \"currency\": \"RUB\", " +
                                "      \"billingUnit\": \"supply_box\", " +
                                "      \"supplyDirectionFrom\": \"ROSTOV_ND\", " +
                                "      \"supplyDirectionTo\": \"MSK\" " +
                                "    }, " +
                                "    { " +
                                "      \"amount\": 2900, " +
                                "      \"type\": \"absolute\", " +
                                "      \"currency\": \"RUB\", " +
                                "      \"billingUnit\": \"supply_pallet\", " +
                                "      \"supplyDirectionFrom\": \"ROSTOV_ND\", " +
                                "      \"supplyDirectionTo\": \"MSK\" " +
                                "    }, " +
                                "    { " +
                                "      \"amount\": 250, " +
                                "      \"type\": \"absolute\", " +
                                "      \"currency\": \"RUB\", " +
                                "      \"billingUnit\": \"supply_box\", " +
                                "      \"supplyDirectionFrom\": \"SPB\", " +
                                "      \"supplyDirectionTo\": \"MSK\" " +
                                "    }, " +
                                "    { " +
                                "      \"amount\": 2900, " +
                                "      \"type\": \"absolute\", " +
                                "      \"currency\": \"RUB\", " +
                                "      \"billingUnit\": \"supply_pallet\", " +
                                "      \"supplyDirectionFrom\": \"SPB\", " +
                                "      \"supplyDirectionTo\": \"MSK\" " +
                                "    } " +
                                "  ]"
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ "" +
                                "[ " +
                                "  { " +
                                "    \"amount\": 15, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\", " +
                                "    \"storageDays\": 7 " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 30, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\", " +
                                "    \"scPartnerId\": 73, " + // тариф применяется только для СЦ Бутово (SC_PEK_BUTOVO)
                                "    \"storageDays\": 14 " +
                                "  } " +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "type": "absolute",
                             "amount": 0,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "storageDays": 0
                        },
                        {
                             "type": "absolute",
                             "amount": 15,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "storageDays": 7
                        },
                        {
                             "type": "absolute",
                             "amount": 0,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "scPartnerId": 73,
                             "storageDays": 0
                        },
                        {
                             "type": "absolute",
                             "amount": 0,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "scPartnerId": 1005494,
                             "storageDays": 0
                        },
                        {
                             "type": "absolute",
                             "amount": 0,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "scPartnerId": 81,
                             "storageDays": 0
                        },
                        {
                             "type": "absolute",
                             "amount": 10,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "returnType": "RETURN",
                             "daysOnStockFrom": 0,
                             "daysOnStockTo": 8
                        },
                        {
                             "type": "absolute",
                             "amount": 15,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "returnType": "RETURN",
                             "daysOnStockFrom": 8,
                             "daysOnStockTo": 60
                        },
                        {
                             "type": "absolute",
                             "amount": 0,
                             "currency": "RUB",
                             "billingUnit": "order",
                             "returnType": "UNREDEEMED",
                             "daysOnStockFrom": 0,
                             "daysOnStockTo": 9
                        }]
                        """
                ),
                Arguments.of(
                        ServiceTypeEnum.SELF_REQUESTED_DISPOSAL,
                        /*language=json*/ "" +
                                "[" +
                                "  { " +
                                "    \"amount\": 15, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"dimensionsTo\": 150 " +
                                "  }," +
                                "  { " +
                                "    \"amount\": 15, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"dimensionsTo\": null " +
                                "  }" +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.CANCELLED_EXPRESS_ORDER_FEE,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": 125000, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null, " +
                                "    \"maxValue\": null " +
                                "}," +
                                "{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": null, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null, " +
                                "    \"maxValue\": null " +
                                "}," +
                                "{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}" +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.CANCELLED_ORDER_FEE,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.EXPRESS_DELIVERY,
                        /*language=json*/ "" +
                                "[" +
                                "{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": null, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null, " +
                                "    \"maxValue\": null " +
                                "}" +
                                "]"
                ),
                Arguments.of(
                        ServiceTypeEnum.CASH_ONLY_ORDER,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 100, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"categoryId\": 90401," +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 100, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"payoutFrequency\": \"BI_WEEKLY\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 100, " +
                                "    \"type\": \"relative\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                        /*language=json*/ "" +
                                "[{ " +
                                "    \"amount\": 10000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\" " +
                                "}]"
                ),
                Arguments.of(
                        ServiceTypeEnum.CROSSREGIONAL_DELIVERY,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 1, " +
                                "    \"priceTo\": 125000, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null, " +
                                "    \"areaFrom\": null," +
                                "    \"areaTo\": null " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 2, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 150, " +
                                "    \"weightTo\": 15000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null, " +
                                "    \"areaFrom\": 1," +
                                "    \"areaTo\": 2 " +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 3, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": 250, " +
                                "    \"weightTo\": 40000, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null, " +
                                "    \"areaFrom\": 1," +
                                "    \"areaTo\": 1 " +
                                "  }, " +
                                "  " +
                                "{ " +
                                "    \"amount\": 15000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"ordinal\": 4, " +
                                "    \"priceTo\": null, " +
                                "    \"dimensionsTo\": null, " +
                                "    \"weightTo\": null, " +
                                "    \"minValue\": null," +
                                "    \"maxValue\": null, " +
                                "    \"areaFrom\": null," +
                                "    \"areaTo\": null " +
                                "  } " +
                                "]"
                ),
                Arguments.of(ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"shift\", " +
                                "    \"shiftType\": \"SMALL_GOODS\", " +
                                "    \"tariffType\": \"MIN\", " +
                                "    \"fromDistance\": 0, " +
                                "    \"toDistance\": null," +
                                "    \"tariffZone\": \"MOSCOW\"" +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 30, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"point_visited\", " +
                                "    \"shiftType\": \"BULKY_CARGO\", " +
                                "    \"tariffType\": \"PVZ\", " +
                                "    \"fromDistance\": 0, " +
                                "    \"toDistance\": 100," +
                                "    \"tariffZone\": \"SPB\"" +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\", " +
                                "    \"shiftType\": \"WITHDRAW\", " +
                                "    \"tariffType\": \"BUSINESS\", " +
                                "    \"fromDistance\": 100, " +
                                "    \"toDistance\": 101," +
                                "    \"tariffZone\": \"OTHER\"" +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\", " +
                                "    \"shiftType\": \"SMALL_GOODS\", " +
                                "    \"tariffType\": \"LOCKER_ORDER\", " +
                                "    \"fromDistance\": 100, " +
                                "    \"toDistance\": 101," +
                                "    \"tariffZone\": \"MOSCOW\"" +
                                "  } " +
                                "]"),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "NONE"
                        }]
                        """
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 12,
                            "type": "relative",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": 3
                        }, {
                            "amount": 10,
                            "type": "relative",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 3,
                            "toMonthAge": 6,
                            "gmvFrom": 50,
                            "gmvTo": 100
                        }, {
                            "amount": 8,
                            "type": "relative",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 6,
                            "gmvFrom": 0,
                            "gmvTo": 100
                        }, {
                            "amount": 40,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "NONE"
                        }]
                        """
                ),
                Arguments.of(ServiceTypeEnum.PVZ_BRANDED_DECORATION,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "pvz_decoration",
                            "pvzTariffZone": "MOSCOW"
                        }]
                        """
                ),
                Arguments.of(ServiceTypeEnum.COURIER_DROPOFF_RETURN,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"shift\", " +
                                "    \"tariffType\": \"MIN\", " +
                                "    \"fromDistance\": 0, " +
                                "    \"toDistance\": null," +
                                "    \"tariffZone\": \"MOSCOW\"" +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 30, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"point_visited\", " +
                                "    \"tariffType\": \"PVZ\", " +
                                "    \"fromDistance\": 0, " +
                                "    \"toDistance\": 100," +
                                "    \"tariffZone\": \"SPB\"" +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\", " +
                                "    \"tariffType\": \"BUSINESS\", " +
                                "    \"fromDistance\": 100, " +
                                "    \"toDistance\": 101," +
                                "    \"tariffZone\": \"OTHER\"" +
                                "  }, " +
                                "  { " +
                                "    \"amount\": 3000, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"order\", " +
                                "    \"tariffType\": \"LOCKER_ORDER\", " +
                                "    \"fromDistance\": 100, " +
                                "    \"toDistance\": 101," +
                                "    \"tariffZone\": \"MOSCOW\"" +
                                "  } " +
                                "]"
                ),
                CourierFineMetaConverterTestUtil.deserializeMetaData(),
                FfStorageTurnoverMetaConverterTestUtil.deserializeMetaData()
        );
    }

    @DisplayName("Тест на не успешную десериализацию меты")
    @ParameterizedTest(name = "[{index}]: {3}")
    @MethodSource("testDeserializeFailedData")
    void testDeserializeFailed(
            ServiceTypeEnum serviceType,
            String json,
            String expectedMsg,
            String typeOfTests
    ) throws Exception {
        TypeFactory typeFactory = objectMapper.getTypeFactory();
        List<Object> listMeta = objectMapper.readValue(
                json,
                typeFactory.constructCollectionType(List.class, Object.class)
        );
        AbstractMetaConverter<?> converter = metaConverterService.getConverter(serviceType);
        InvalidMetaException invalidMetaException = Assertions.assertThrows(
                InvalidMetaException.class,
                () -> converter.deserialize(listMeta),
                expectedMsg
        );
        assertThat(invalidMetaException.getMessage(), startsWith(expectedMsg));
    }

    private static Stream<Arguments> testDeserializeFailedData() {
        return Stream.of(
                        deserializeFailedGeneral(),
                        deserializeFailedFulfillment(),
                        deserializeFailedFfStorageBillingMultiplier(),
                        deserializeFailedReturnedOrderStorage(),
                        deserializeFailedMinFee(),
                        deserializeFailedSorting(),
                        deserializeFailedMinDaily(),
                        deserializeFailedFfXdocSupply(),
                        deserializeFailedSupplierFee(),
                        deserializeFailedSupplierDisposal(),
                        deserializeFailedCancelledExpressOrderFee(),
                        deserializeFailedCancelledOrderFee(),
                        deserializeFailedExpressDelivery(),
                        deserializeFailedCashOnlyOrder(),
                        deserializeFailedCourierShift(),
                        deserializeFailedPvzReward(),
                        deserializeFailedPvzBrandedDecoration(),
                        deserializeFailedPvzFlat(),
                        deserializedFailedCourierShiftCommon(),
                        CourierFineMetaConverterTestUtil.deserializeFailed(),
                        FfStorageTurnoverMetaConverterTestUtil.deserializeFailed(),
                        deserializeFailedAgencyCommission()
                )
                .flatMap(Function.identity());
    }

    private static Stream<Arguments> deserializeFailedGeneral() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "[{}]",
                        fieldNotNull("amount"),
                        GENERAL_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "[{\"amount\": -1}]",
                        "The field 'amount' must be not negative",
                        GENERAL_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "[{\"amount\": 1}]",
                        fieldNotNull("currency"),
                        GENERAL_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"\"}]",
                        "The field 'currency' must be not empty",
                        GENERAL_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\"}]",
                        fieldNotNull("type"),
                        GENERAL_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedFulfillment() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.FF_PROCESSING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("billingUnit"),
                        FULFILLMENT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"billingUnit\": \"daily\"}]",
                        UNKNOWN_BILLING_UNIT,
                        FULFILLMENT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.DELIVERY_TO_CUSTOMER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"billingUnit\": \"item\"}]",
                        fieldNotNull("ordinal"),
                        FULFILLMENT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.DELIVERY_TO_CUSTOMER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"billingUnit\": \"item\", \"ordinal\": -1}]",
                        "The field 'ordinal' must be bigger than zero",
                        FULFILLMENT_FAILED_DATA
                ),

                Arguments.of(
                        ServiceTypeEnum.DELIVERY_TO_CUSTOMER_RETURN,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"ordinal\": 1, \"billingUnit\": \"order\"}]",
                        fieldNotNull("orderStatus"),
                        FULFILLMENT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_WITHDRAW,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"ordinal\": 1, \"billingUnit\": \"item\"}," +
                                "{\"amount\": 100, \"currency\": \"RUB\", \"type\": \"absolute\", \"ordinal\": 1, \"billingUnit\": \"item\"}]",
                        "The meta contains duplicated fields ordinal : [1]",
                        FULFILLMENT_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedFfStorageBillingMultiplier() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("categoryId"),
                        FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"amount\": 1, " +
                                "    \"type\": \"absolute\", " +
                                "    \"currency\": \"RUB\", " +
                                "    \"billingUnit\": \"item\", " +
                                "    \"categoryId\": 1, " +
                                "    \"daysOnStockFrom\": 120, " +
                                "    \"daysOnStockTo\": 1000, " +
                                "    \"multiplier\": 5 " +
                                "  } " +
                                "]",
                        "The category [1] doesnt exists",
                        FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"categoryId\": 90401, " +
                                "    \"daysOnStockTo\": 120, " +
                                "    \"multiplier\": 5 " +
                                "  } " +
                                "]",
                        fieldNotNull("daysOnStockFrom"),
                        FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"categoryId\": 90401, " +
                                "    \"daysOnStockFrom\": 130, " +
                                "    \"daysOnStockTo\": 120, " +
                                "    \"multiplier\": 1.5 " +
                                "  } " +
                                "]",
                        "The field 'daysOnStockFrom' can not be bigger then the field 'daysOnStockTo'",
                        FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"categoryId\": 90401, " +
                                "    \"daysOnStockFrom\": 120, " +
                                "    \"daysOnStockTo\": 120, " +
                                "    \"multiplier\": -1 " +
                                "  } " +
                                "]",
                        "The field 'multiplier' must be not negative",
                        FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_STORAGE_BILLING_MULTIPLIER,
                        /*language=json*/ "[ " +
                                "  { " +
                                "    \"categoryId\": 90401, " +
                                "    \"daysOnStockFrom\": 0, " +
                                "    \"daysOnStockTo\": 50, " +
                                "    \"multiplier\": 0 " +
                                "  }, " +
                                "  { " +
                                "    \"categoryId\": 90401, " +
                                "    \"daysOnStockFrom\": 40, " +
                                "    \"daysOnStockTo\": 70, " +
                                "    \"multiplier\": 1 " +
                                "  } " +
                                "]",
                        "Multiplier's day intervals with same category must not intersect." +
                                " But in category 90401 they do: [0, 50) [40, 70)",
                        FF_STORAGE_BILLING_MULTIPLIER_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedReturnedOrderStorage() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "amount": 1,
                             "type": "absolute",
                             "currency": "RUB",
                             "storageDays": -1
                        }]
                        """,
                        "The field 'storageDays' must be not negative",
                        RETURNED_ORDERS_STORAGE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "amount": 1,
                             "type": "absolute",
                             "currency": "RUB",
                             "storageDays": 1
                        }]
                        """,
                        fieldNotNull("billingUnit"),
                        RETURNED_ORDERS_STORAGE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "amount": 1,
                             "type": "absolute",
                             "currency": "RUB",
                             "storageDays": 1,
                             "billingUnit": "item"
                        }]
                        """,
                        UNKNOWN_BILLING_UNIT,
                        RETURNED_ORDERS_STORAGE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "amount": 1,
                             "type": "absolute",
                             "currency": "RUB",
                             "storageDays": 1,
                             "billingUnit": "order"
                        },
                        {
                             "amount": 10,
                             "type": "absolute",
                             "currency": "RUB",
                             "storageDays": 1,
                             "billingUnit": "order"
                        }]
                        """,
                        "The meta contains duplicated fields (storageDays + scPartnerId) : [(1,null)]",
                        RETURNED_ORDERS_STORAGE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "amount": 100,
                             "type": "absolute",
                             "currency": "RUB",
                             "daysOnStockFrom": 130,
                             "daysOnStockTo": 120
                        }]
                        """,
                        "The field 'daysOnStockFrom' can not be bigger then or equal the field 'daysOnStockTo'",
                        RETURNED_ORDERS_STORAGE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.RETURNED_ORDERS_STORAGE,
                        /*language=json*/ """
                        [{
                             "amount": 100,
                             "type": "absolute",
                             "currency": "RUB",
                             "daysOnStockFrom": -1,
                             "daysOnStockTo": 10
                        }]
                        """,
                        "The field 'daysOnStockFrom' must be not negative",
                        RETURNED_ORDERS_STORAGE_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedMinFee() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("categoryId"),
                        MIN_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 987}]",
                        "The category [987] doesnt exists",
                        MIN_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401}]",
                        fieldNotNull("billingUnit"),
                        MIN_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"billingUnit\": \"daily\"}]",
                        UNKNOWN_BILLING_UNIT,
                        MIN_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"billingUnit\": \"item\"}]",
                        fieldNotNull("orderStatus"),
                        MIN_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"billingUnit\": \"item\", \"orderStatus\": \"delivered\"}," +
                                "{\"amount\": 100, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"billingUnit\": \"item\", \"orderStatus\": \"delivered\"}]",
                        "The meta contains duplicated fields categoryId : [90401]",
                        MIN_FEE_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedSorting() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.SORTING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("intakeType"),
                        SORTING_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.SORTING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"intakeType\": \"intake\"}]",
                        fieldNotNull("billingUnit"),
                        SORTING_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.SORTING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"intakeType\": \"intake\", \"billingUnit\": \"item\"}]",
                        UNKNOWN_BILLING_UNIT,
                        SORTING_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.SORTING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"intakeType\": \"intake\", \"billingUnit\": \"order\"}]",
                        fieldNotNull("orderStatus"),
                        SORTING_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.SORTING,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"intakeType\": \"intake\", \"billingUnit\": \"order\", \"orderStatus\" : \"delivered\"}," +
                                "{\"amount\": 100, \"currency\": \"RUB\", \"type\": \"absolute\", \"intakeType\": \"intake\", \"billingUnit\": \"order\", \"orderStatus\" : \"delivered\"}]",
                        "The meta contains duplicated fields intakeType : [intake]",
                        SORTING_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedMinDaily() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.MIN_DAILY,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("billingUnit"),
                        MIN_DAILY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.MIN_DAILY,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"billingUnit\" : \"order\"}]",
                        UNKNOWN_BILLING_UNIT,
                        MIN_DAILY_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedCancelledExpressOrderFee() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.CANCELLED_EXPRESS_ORDER_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"ordinal\": 1 }]",
                        fieldNotNull("billingUnit"),
                        CANCELLED_EXPRESS_ORDER_FEE_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedCancelledOrderFee() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.CANCELLED_ORDER_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("billingUnit"),
                        CANCELLED_ORDER_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.CANCELLED_ORDER_FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"billingUnit\" : \"order\"}]",
                        UNKNOWN_BILLING_UNIT,
                        CANCELLED_ORDER_FEE_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedExpressDelivery() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.EXPRESS_DELIVERY,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\" ," +
                                " \"ordinal\": 1 }]",
                        fieldNotNull("billingUnit"),
                        EXPRESS_DELIVERY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.EXPRESS_DELIVERY,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\" }]",
                        fieldNotNull("billingUnit"),
                        EXPRESS_DELIVERY_FAILED_DATA
                )
        );
    }


    private static Stream<Arguments> deserializeFailedCashOnlyOrder() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.CASH_ONLY_ORDER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"relative\", \"categoryId\" : 90401}]",
                        fieldNotNull("billingUnit"),
                        CASH_ONLY_ORDER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.CASH_ONLY_ORDER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"relative\", \"billingUnit\" : \"item\"}]",
                        fieldNotNull("categoryId"),
                        CASH_ONLY_ORDER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.CASH_ONLY_ORDER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"relative\", \"billingUnit\" : \"order\", \"categoryId\" : 90401}]",
                        UNKNOWN_BILLING_UNIT,
                        CASH_ONLY_ORDER_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.CASH_ONLY_ORDER,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"relative\", \"billingUnit\" : \"item\", \"categoryId\" : 90401}," +
                                "{\"amount\": 2, \"currency\": \"RUB\", \"type\": \"relative\", \"billingUnit\" : \"item\", \"categoryId\" : 90401}]",
                        "The meta contains duplicated fields (categoryId) : [90401]",
                        CASH_ONLY_ORDER_FAILED_DATA
                )

        );
    }

    private static Stream<Arguments> deserializeFailedAgencyCommission(){

        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                        /*language=json*/
                        "[{" +
                            "\"amount\": 1," +
                            "\"currency\": \"RUB\"," +
                            "\"type\": \"relative\"" +
                        "}]",
                        fieldNotNull("billingUnit"),
                        AGENCY_COMMISSION_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                        /*language=json*/
                        "[{" +
                            "\"amount\": 1," +
                            "\"currency\": \"RUB\"," +
                            "\"type\": \"relative\"," +
                            "\"billingUnit\" : \"order\"" +
                        "}]",
                        UNKNOWN_BILLING_UNIT,
                        AGENCY_COMMISSION_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                            /*language=json*/
                            "[{" +
                                "\"amount\": 1," +
                                "\"currency\": \"RUB\"," +
                                "\"type\": \"relative\"," +
                                "\"billingUnit\" : \"item\"," +
                                "\"payoutFrequency\" : \"BI_WEEKLY\"" +
                            "}," +
                            "{" +
                                "\"amount\": 2," +
                                "\"currency\": \"RUB\"," +
                                "\"type\": \"relative\"," +
                                "\"billingUnit\" : \"item\"," +
                                "\"payoutFrequency\" : \"BI_WEEKLY\"" +
                            "}]",
                        "The meta contains duplicated fields (payoutFrequency) : [BI_WEEKLY]",
                        AGENCY_COMMISSION_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.AGENCY_COMMISSION,
                        /*language=json*/
                            "[{\"amount\": 1," +
                                "\"currency\": \"RUB\"," +
                                "\"type\": \"relative\"," +
                                "\"billingUnit\" : \"item\"," +
                                "\"payoutFrequency\" : \"BI_WEEKLY\"" +
                            "}," +
                            "{" +
                                "\"amount\": 2," +
                                "\"currency\": \"RUB\"," +
                                "\"type\": \"relative\"," +
                                "\"billingUnit\" : \"item\"" +
                            "}]",
                        "Multiple Meta can only be with nonnul payoutFrequency.",
                        AGENCY_COMMISSION_FAILED_DATA
                )
        );

    }

    private static Stream<Arguments> deserializeFailedFfXdocSupply() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("supplyDirectionFrom"),
                        FF_XDOC_SUPPLY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"supplyDirectionFrom\": \"MSK\"}]",
                        fieldNotNull("supplyDirectionTo"),
                        FF_XDOC_SUPPLY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/
                        "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"supplyDirectionFrom\": \"MSK\", \"supplyDirectionTo\": \"MSK\"}]",
                        "The field 'supplyDirectionFrom' must be not equals the field 'supplyDirectionTo'",
                        FF_XDOC_SUPPLY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/
                        "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"supplyDirectionFrom\": \"MSK\", \"supplyDirectionTo\": \"ROSTOV_ND\"}]",
                        fieldNotNull("billingUnit"),
                        FF_XDOC_SUPPLY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/
                        "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"supplyDirectionFrom\": \"MSK\", \"supplyDirectionTo\": \"ROSTOV_ND\", \"billingUnit\" : \"order\"}]",
                        UNKNOWN_BILLING_UNIT,
                        FF_XDOC_SUPPLY_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FF_XDOC_SUPPLY,
                        /*language=json*/
                        "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"supplyDirectionFrom\": \"MSK\", \"supplyDirectionTo\": \"ROSTOV_ND\", \"billingUnit\" : \"supply_box\"}," +
                                "{\"amount\": 100, \"currency\": \"RUB\", \"type\": \"absolute\", \"supplyDirectionFrom\": \"MSK\", \"supplyDirectionTo\": \"ROSTOV_ND\", \"billingUnit\" : \"supply_box\"}]",
                        "The meta contains duplicated fields (directionFrom + directionTo + billingUnit) : [(MSK,ROSTOV_ND,supply_box)]",
                        FF_XDOC_SUPPLY_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedSupplierDisposal() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.SELF_REQUESTED_DISPOSAL,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"billingUnit\": \"order\"}]",
                        UNKNOWN_BILLING_UNIT,
                        DISPOSAL_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.SELF_REQUESTED_DISPOSAL,
                        /**/ "[{\"amount\": 15, \"type\": \"absolute\", \"currency\": \"RUB\", \"billingUnit\": \"item\", \"dimensionsTo\": -10}]",
                        "The field 'dimensionsTo' must be not negative",
                        DISPOSAL_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedSupplierFee() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\"}]",
                        fieldNotNull("categoryId"),
                        SUPPLIER_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 987}]",
                        "The category [987] doesnt exists",
                        SUPPLIER_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401}]",
                        fieldNotNull("orderStatus"),
                        SUPPLIER_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"orderStatus\":  \"delivered\"}]",
                        fieldNotNull("billingUnit"),
                        SUPPLIER_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"orderStatus\":  \"delivered\", \"billingUnit\": \"order\"}]",
                        UNKNOWN_BILLING_UNIT,
                        SUPPLIER_FEE_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.FEE,
                        /*language=json*/ "[{\"amount\": 1, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"orderStatus\":  \"delivered\", \"billingUnit\": \"item\", \"isExpress\": false }," +
                                "{\"amount\": 100, \"currency\": \"RUB\", \"type\": \"absolute\", \"categoryId\" : 90401, \"orderStatus\":  \"delivered\", \"billingUnit\": \"item\", \"isExpress\":  false}]",
                        "The meta contains duplicated fields (categoryId + isExpress) : [(90401,false)]",
                        SUPPLIER_FEE_FAILED_DATA
                )

        );
    }

    private static Stream<Arguments> deserializeFailedCourierShift() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": -1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'amount' must be not negative",
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": null, " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("currency"),
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": null, " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("type"),
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"supply_box\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        UNKNOWN_BILLING_UNIT,
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  100," +
                                "\"toDistance\":  1," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'distanceFrom' can not be bigger than the field 'distanceTo'",
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  null" +
                                "}]",
                        fieldNotNull("tariffZone"),
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  null," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("tariffType"),
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  null," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("shiftType"),
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  -1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'fromDistance' must be not negative",
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  -100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'toDistance' must be bigger than zero",
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[" +
                                "{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  null," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}," +
                                "{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"relative\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  30," +
                                "\"toDistance\":  50," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}" +
                                "]",
                        """
                                Distance's intervals with same tariffZone, shiftType, sortingCenterId and tariffType
                                mustn't intersect. But with tariffZone: MOSCOW, shiftType: SMALL_GOODS,
                                sortingCenterId: null and tariffType: LOCKER_ORDER they do:
                                [1, null]
                                [30, 50]
                                """,
                        COURIER_SHIFT_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"," +
                                "\"sortingCenterId\": -1" +
                                "}]",
                        "The field 'sortingCenterId' must be not negative",
                        COURIER_SHIFT_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedPvzReward() {
        return Stream.of(
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item"
                        }]
                        """,
                        UNKNOWN_BILLING_UNIT,
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order"
                        }]
                        """,
                        fieldNotNull("pvzBrandingType"),
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL"
                        }]
                        """,
                        fieldNotNull("pvzTariffZone"),
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": -1
                        }]
                        """,
                        "The field 'fromMonthAge' must be not negative",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": -1
                        }]
                        """,
                        "The field 'toMonthAge' must be not negative",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": 1,
                            "gmvFrom": -1,
                            "gmvTo": 1
                        }]
                        """,
                        "The field 'gmvFrom' must be not negative",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": 1,
                            "gmvFrom": 0,
                            "gmvTo": -1
                        }]
                        """,
                        "The field 'gmvTo' must be not negative",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 3,
                            "toMonthAge": 1
                        }]
                        """,
                        "fromMonthAge must be strictly lower than toMonthAge",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 1,
                            "toMonthAge": 3,
                            "gmvFrom": 3,
                            "gmvTo": 0
                        }]
                        """,
                        "gmvFrom must be strictly lower than gmvTo",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "NONE"
                        }, {
                            "amount": 1000,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "NONE"
                        }]
                        """,
                        "PvzReward tariff must contains not more than 1 NONE meta",
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": 4,
                            "gmvFrom": 0,
                            "gmvTo": 100
                        }, {
                            "amount": 1000,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 3,
                            "toMonthAge": 6,
                            "gmvFrom": 100,
                            "gmvTo": 200
                        }]
                        """,
                        """
                        Metas contains intersections on pvz month ages.
                        TariffZone : MOSCOW,
                        Intersected month ages: [0, 4) and [3, 6)
                        """,
                        PVZ_REWARD_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_REWARD,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": 3,
                            "gmvFrom": 0,
                            "gmvTo": 100
                        }, {
                            "amount": 1000,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "order",
                            "pvzBrandingType": "FULL",
                            "pvzTariffZone": "MOSCOW",
                            "fromMonthAge": 0,
                            "toMonthAge": 3,
                            "gmvFrom": 50,
                            "gmvTo": 200
                        }]
                        """,
                        """
                        Metas contains intersections on pvz GMV limits in one age zone.
                        TariffZone : MOSCOW,
                        Age: [0, 3),
                        Intersected limits: [0, 100) and [50, 200)
                        """,
                        PVZ_REWARD_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedPvzFlat() {
        return Stream.of(
                Arguments.of(ServiceTypeEnum.PVZ_CARD_COMPENSATION,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item"
                        }]
                        """,
                        UNKNOWN_BILLING_UNIT,
                        PVZ_FLAT_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializeFailedPvzBrandedDecoration() {
        return Stream.of(
                Arguments.of(ServiceTypeEnum.PVZ_BRANDED_DECORATION,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "item"
                        }]
                        """,
                        UNKNOWN_BILLING_UNIT,
                        PVZ_BRANDED_DECORATION_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_BRANDED_DECORATION,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "pvz_decoration"
                        }]
                        """,
                        fieldNotNull("pvzTariffZone"),
                        PVZ_BRANDED_DECORATION_FAILED_DATA
                ),
                Arguments.of(ServiceTypeEnum.PVZ_BRANDED_DECORATION,
                        /*language=json*/ """
                        [{
                            "amount": 100,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "pvz_decoration",
                            "pvzTariffZone": "MOSCOW"
                        }, {
                            "amount": 1000,
                            "type": "absolute",
                            "currency": "RUB",
                            "billingUnit": "pvz_decoration",
                            "pvzTariffZone": "MOSCOW"
                        }]
                        """,
                        "The meta contains duplicated fields pvzTariffZone : [MOSCOW]",
                        PVZ_BRANDED_DECORATION_FAILED_DATA
                )
        );
    }

    private static Stream<Arguments> deserializedFailedCourierShiftCommon() {
        return Stream.of(
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": -1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'amount' must be not negative",
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": null, " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("currency"),
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": null, " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("type"),
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"supply_box\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        UNKNOWN_BILLING_UNIT,
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  100," +
                                "\"toDistance\":  1," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'distanceFrom' can not be bigger than the field 'distanceTo'",
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  null" +
                                "}]",
                        fieldNotNull("tariffZone"),
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"tariffType\":  null," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        fieldNotNull("tariffType"),
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  -1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'fromDistance' must be not negative",
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  -100," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}]",
                        "The field 'toDistance' must be bigger than zero",
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_DROPOFF_RETURN,
                        /*language=json*/ "[" +
                                "{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  null," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}," +
                                "{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"relative\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  30," +
                                "\"toDistance\":  50," +
                                "\"tariffZone\":  \"MOSCOW\"" +
                                "}" +
                                "]",
                        """
                                Distance's intervals with same tariffZone, sortingCenterId and tariffType
                                mustn't intersect. But with tariffZone: MOSCOW,
                                sortingCenterId: null and tariffType: LOCKER_ORDER they do:
                                [1, null]
                                [30, 50]
                                """,
                        COURIER_SHIFT_COMMON_FAILED_DATA
                ),
                Arguments.of(
                        ServiceTypeEnum.COURIER_SHIFT,
                        /*language=json*/ "[{" +
                                "\"amount\": 1, " +
                                "\"currency\": \"RUB\", " +
                                "\"type\": \"absolute\", " +
                                "\"billingUnit\":  \"order\"," +
                                "\"tariffType\":  \"LOCKER_ORDER\"," +
                                "\"shiftType\":  \"SMALL_GOODS\"," +
                                "\"fromDistance\":  1," +
                                "\"toDistance\":  100," +
                                "\"tariffZone\":  \"MOSCOW\"," +
                                "\"sortingCenterId\": -1" +
                                "}]",
                        "The field 'sortingCenterId' must be not negative",
                        COURIER_SHIFT_COMMON_FAILED_DATA
                )
        );
    }


    @Nonnull
    protected static String fieldNotNull(String fieldName) {
        return "The field '" + fieldName + "' must be not null";
    }
}
