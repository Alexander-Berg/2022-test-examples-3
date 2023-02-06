package ru.yandex.market.tpl.core.service.location.calculator.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.domain.usershift.RoutePoint;

@UtilityClass
public class RoutePointBuilderTestUtil {

    public List<RoutePoint> buildRoutePoints(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> buildRoutePoint())
                .collect(Collectors.toList());
    }

    public RoutePoint buildRoutePoint() {
        return buildRoutePoint(BigDecimal.ZERO, BigDecimal.ZERO, Instant.now());
    }

    public RoutePoint buildRoutePoint(double longitude, double latitude, Instant arrivalTime) {
        return buildRoutePoint(BigDecimal.valueOf(longitude), BigDecimal.valueOf(latitude), arrivalTime);
    }

    @SneakyThrows
    public RoutePoint buildRoutePoint(BigDecimal longitude, BigDecimal latitude, Instant arrivalTime) {
        Constructor<RoutePoint> declaredConstructor = RoutePoint.class.getDeclaredConstructor();
        declaredConstructor.trySetAccessible();
        RoutePoint routePoint = declaredConstructor.newInstance();

        setFieldValue(routePoint, longitude, BigDecimal.class, "setLongitude");
        setFieldValue(routePoint, latitude, BigDecimal.class, "setLatitude");
        setFieldValue(routePoint, arrivalTime, Instant.class, "setArrivalTime");

        return routePoint;
    }

    @SneakyThrows
    private void setFieldValue(RoutePoint model, Object fieldValue, Class fieldClazz, String methodName) {
        Method method = RoutePoint.class.getDeclaredMethod(methodName, fieldClazz);
        method.trySetAccessible();
        method.invoke(model, fieldValue);
    }
}
