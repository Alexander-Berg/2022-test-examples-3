package ru.yandex.market.logistics.lom.converter;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.google.protobuf.AbstractMessage;
import com.google.protobuf.Internal;
import com.google.protobuf.ProtobufInternals;
import com.google.protobuf.UnknownFieldSet;
import com.google.protobuf.util.JsonFormat;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.jeasy.random.FieldPredicates;
import org.jeasy.random.api.Randomizer;
import org.jeasy.random.randomizers.AbstractRandomizer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import yandex.market.combinator.common.Common;
import yandex.market.combinator.v0.CombinatorOuterClass;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.model.enums.PartnerType;
import ru.yandex.market.logistics.management.entity.type.LogisticSegmentType;
import ru.yandex.market.logistics.management.entity.type.ServiceCodeName;
import ru.yandex.market.logistics.management.entity.type.ServiceType;

@DisplayName("Тест на несовместимые изменения market/combinator/proto/grpc/combinator.proto")
class CombinatorRouteConverterTest extends AbstractContextualTest {
    /**
     * Типы точек (сегментов), которые не участвуют в прямом маршруте.
     */
    private static final Set<LogisticSegmentType> EXCLUDED_POINT_TYPES = Set.of(
        LogisticSegmentType.BACKWARD_WAREHOUSE,
        LogisticSegmentType.BACKWARD_MOVEMENT
    );
    private static final List<String> POINT_TYPES = Stream.of(LogisticSegmentType.values())
        .filter(segmentType -> !EXCLUDED_POINT_TYPES.contains(segmentType))
        .map(Enum::name)
        .map(String::toLowerCase)
        .collect(Collectors.toList());

    private static final List<String> SERVICE_CODES = Stream.of(ServiceCodeName.values())
        .map(Enum::name)
        .collect(Collectors.toList());

    private static final List<String> PARTNER_TYPES = Stream.of(PartnerType.values())
        .map(Enum::name)
        .collect(Collectors.toList());

    private static final Random RANDOM = new Random();
    private static final EasyRandomParameters EASY_RANDOM_PARAMETERS = new EasyRandomParameters()
        .overrideDefaultInitialization(true)
        .excludeField(FieldPredicates.ofType(UnknownFieldSet.class))
        .excludeField(FieldPredicates.named("memoizedSize")
            .and(FieldPredicates.ofType(int.class))
            .and(FieldPredicates.inClass(AbstractMessage.class))
        )
        .excludeField(FieldPredicates.named("paths_")
            .and(FieldPredicates.ofType(List.class))
            .and(FieldPredicates.inClass(CombinatorOuterClass.Route.class))
        )
        .excludeField(FieldPredicates.named("offers_")
            .and(FieldPredicates.ofType(List.class))
            .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryRoute.class))
        )
        .randomize(long.class, new AbstractRandomizer<>() {
                @Override
                public Long getRandomValue() {
                    return Math.abs(random.nextLong());
                }
            }
        )
        .randomize(int.class, nonNegativeIntegerRandomizer())
        .randomize(Internal.IntList.class, new AbstractRandomizer<>() {
                @Override
                public Internal.IntList getRandomValue() {
                    return ProtobufInternals.randomList(RANDOM, 2, 5);
                }
            }
        )
        .randomize(
            FieldPredicates.named("key_")
                .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryService.ServiceMeta.class))
                .and(FieldPredicates.ofType(Object.class)),
            stringTemplateRandomizer("key")
        )
        .randomize(
            FieldPredicates.named("value_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryService.ServiceMeta.class)),
            stringTemplateRandomizer("value")
        )
        .randomize(
            FieldPredicates.named("deliveryType_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Route.class)),
            new AbstractRandomizer<>() {
                @Override
                public Integer getRandomValue() {
                    return Math.abs(random.nextInt(Common.DeliveryType.values().length - 1));
                }
            }
        )
        .randomize(
            FieldPredicates.named("type_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryService.class)),
            nonNegativeIntegerRandomizer(ServiceType.values().length)
        )
        .randomize(
            FieldPredicates.named("seconds_")
                .and(FieldPredicates.ofType(long.class)),
            new AbstractRandomizer<Long>() {
                @Override
                public Long getRandomValue() {
                    return Instant.now().getEpochSecond();
                }
            }
        )
        .randomize(
            FieldPredicates.named("nanos_")
                .and(FieldPredicates.ofType(int.class)),
            new AbstractRandomizer<Integer>() {
                @Override
                public Integer getRandomValue() {
                    return Instant.now().getNano();
                }
            }
        )
        .randomize(
            FieldPredicates.named("segmentType_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Route.Point.class)),
            new AbstractRandomizer<String>() {
                @Override
                public String getRandomValue() {
                    return POINT_TYPES.get(random.nextInt(POINT_TYPES.size()));
                }
            }
        )
        .randomize(
            FieldPredicates.named("code_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryService.class)),
            new AbstractRandomizer<String>() {
                @Override
                public String getRandomValue() {
                    return SERVICE_CODES.get(random.nextInt(SERVICE_CODES.size()));
                }
            }
        )
        .randomize(
            FieldPredicates.named("shipmentDate_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("packagingTime_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("shipmentBySupplier_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("receptionByWarehouse_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("processingStartTime_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringSupplierDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("shipmentBySupplier_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringSupplierDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("receptionByWarehouse_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.StringSupplierDeliveryDates.class)),
            timeRandomizer()
        )
        .randomize(
            FieldPredicates.named("day_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Date.class)),
            new AbstractRandomizer<Integer>() {
                @Override
                public Integer getRandomValue() {
                    return LocalDate.now().getDayOfMonth();
                }
            }
        )
        .randomize(
            FieldPredicates.named("month_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Date.class)),
            new AbstractRandomizer<Integer>() {
                @Override
                public Integer getRandomValue() {
                    return LocalDate.now().getMonthValue();
                }
            }
        )
        .randomize(
            FieldPredicates.named("year_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Date.class)),
            new AbstractRandomizer<Integer>() {
                @Override
                public Integer getRandomValue() {
                    return LocalDate.now().getYear();
                }
            }
        )
        .randomize(
            FieldPredicates.named("hour_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Time.class)),
            new AbstractRandomizer<Integer>() {
                @Override
                public Integer getRandomValue() {
                    return LocalTime.now().getHour();
                }
            }
        )
        .randomize(
            FieldPredicates.named("minute_")
                .and(FieldPredicates.ofType(int.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Time.class)),
            new AbstractRandomizer<Integer>() {
                @Override
                public Integer getRandomValue() {
                    return LocalTime.now().getMinute();
                }
            }
        )
        .randomize(
            FieldPredicates.named("partnerType_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Route.Point.class)),
            new AbstractRandomizer<String>() {
                @Override
                public String getRandomValue() {
                    return PARTNER_TYPES.get(random.nextInt(PARTNER_TYPES.size()));
                }
            }
        )
        .randomize(
            FieldPredicates.named("partnerName_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.Route.Point.class)),
            stringTemplateRandomizer("Name")
        )
        .randomize(
            FieldPredicates.named("promise_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryRoute.class)),
            stringTemplateRandomizer("Promise")
        )
        .randomize(
            FieldPredicates.named("dsbsPointId_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.PointIds.class)),
            stringTemplateRandomizer("DsbsPointId")
        )
        .randomize(
            FieldPredicates.named("error_")
                .and(FieldPredicates.ofType(Object.class))
                .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryRoute.class)),
            stringTemplateRandomizer("Error")
        )
        .randomize(
             FieldPredicates.named("description_")
                 .and(FieldPredicates.ofType(Object.class))
                 .and(FieldPredicates.inClass(CombinatorOuterClass.RouteDebugMessage.class)),
             stringTemplateRandomizer("Description")
         )
        .randomize(
             FieldPredicates.named("routeId_")
                 .and(FieldPredicates.ofType(Object.class))
                 .and(FieldPredicates.inClass(CombinatorOuterClass.DeliveryRoute.class)),
             stringTemplateRandomizer("RouteId")
         )
        .collectionSizeRange(2, 5);
    private static final EasyRandom RANDOMIZER = new EasyRandom(EASY_RANDOM_PARAMETERS);

    private static final JsonFormat.Printer PRINTER = JsonFormat.printer()
        .preservingProtoFieldNames()
        .omittingInsignificantWhitespace();

    @Autowired
    private RouteConverter routeConverter;

    @Test
    @DisplayName("Комбинированный маршрут конвертируется во внутреннее представление без ошибок")
    void combinatorRouteConvertsCorrectlyToLomRepresentation() throws Exception {
        CombinatorOuterClass.DeliveryRoute combinatorRoute
            = fixPaths(RANDOMIZER.nextObject(CombinatorOuterClass.DeliveryRoute.class));

        Object wellKnownTypePrinters = clearWellKnownTypePrinters();
        String jsonRoute = PRINTER.print(combinatorRoute);
        setWellKnownTypePrinters(wellKnownTypePrinters);

        softly.assertThatCode(() -> routeConverter.convertToEntity(objectMapper.readTree(jsonRoute)))
            .doesNotThrowAnyException();
    }

    /**
     * Заполняем paths парами индексов массива points.
     */
    @Nonnull
    private CombinatorOuterClass.DeliveryRoute fixPaths(CombinatorOuterClass.DeliveryRoute route) {
        List<CombinatorOuterClass.Route.Path> paths = IntStream.range(0, route.getRoute().getPointsCount() - 1)
            .mapToObj(i -> CombinatorOuterClass.Route.Path.newBuilder().setPointFrom(i).setPointTo(i + 1).build())
            .collect(Collectors.toList());
        return route.toBuilder().setRoute(route.getRoute().toBuilder().addAllPaths(paths).build()).build();
    }

    /**
     * Грязный хак. Очищаем форматтеры для com.google.protobuf.Timestamp и com.google.protobuf.Duration.
     */
    @Nonnull
    private Object clearWellKnownTypePrinters() throws Exception {
        Field wellKnownTypePrintersField = getWellKnownTypePrintersField();
        Object wellKnownTypePrinters = wellKnownTypePrintersField.get(null);
        wellKnownTypePrintersField.set(null, Map.of());
        return wellKnownTypePrinters;
    }

    /**
     * Возвращаем форматтеры обратно.
     */
    private void setWellKnownTypePrinters(Object wellKnownTypePrinters) throws Exception {
        Field wellKnownTypePrintersField = getWellKnownTypePrintersField();
        wellKnownTypePrintersField.set(null, wellKnownTypePrinters);
    }

    @Nonnull
    private Field getWellKnownTypePrintersField() throws Exception {
        Field wellKnownTypePrintersField = Class.forName("com.google.protobuf.util.JsonFormat$PrinterImpl")
            .getDeclaredField("wellKnownTypePrinters");
        wellKnownTypePrintersField.setAccessible(true);
        FieldUtils.removeFinalModifier(wellKnownTypePrintersField);
        return wellKnownTypePrintersField;
    }

    @Nonnull
    private static Randomizer<Integer> nonNegativeIntegerRandomizer() {
        return new AbstractRandomizer<>() {
            @Override
            public Integer getRandomValue() {
                return Math.abs(random.nextInt());
            }
        };
    }

    @Nonnull
    private static Randomizer<Integer> nonNegativeIntegerRandomizer(int seed) {
        return new AbstractRandomizer<>() {
            @Override
            public Integer getRandomValue() {
                return Math.abs(random.nextInt(seed));
            }
        };
    }

    @Nonnull
    private static Randomizer<String> timeRandomizer() {
        return new AbstractRandomizer<>() {
            @Override
            public String getRandomValue() {
                return LocalDate.now().toString();
            }
        };
    }

    @Nonnull
    private static Randomizer<String> stringTemplateRandomizer(String template) {
        return new AbstractRandomizer<>() {
            @Override
            public String getRandomValue() {
                return template;
            }
        };
    }
}
