package ru.yandex.market.tpl.core.service.location.calculator.util;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;

import ru.yandex.market.tpl.core.domain.usershift.location.GeoPoint;
import ru.yandex.market.tpl.core.service.location.calculator.dto.CoordinateDto;

@UtilityClass
public class CoordinateDtoBuilderTestUtil {

    public List<CoordinateDto> buildCoordinateDtos(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> buildCoordinateDto())
                .collect(Collectors.toList());
    }

    public CoordinateDto buildCoordinateDto() {
        return buildCoordinateDto(0, 0);
    }

    public CoordinateDto buildCoordinateDto(double latitude, double longitude) {
        return buildCoordinateDto(latitude, longitude, Instant.now());
    }

    public CoordinateDto buildCoordinateDto(double latitude, double longitude, Instant trackingTime) {
        return buildCoordinateDto(GeoPoint.ofLatLon(latitude, longitude), trackingTime);
    }

    @SneakyThrows
    public CoordinateDto buildCoordinateDto(GeoPoint geoPointDto, Instant trackingTime) {
        return new CoordinateDto(geoPointDto, trackingTime);
    }

    public List<CoordinateDto> buildMoscowTrack() {
        return List.of(
                buildCoordinateDto(55.75239157553176, 37.58628539404965),
                buildCoordinateDto(55.75236091968523, 37.60022009344747),
                buildCoordinateDto(55.75590360167856, 37.600092937530945),
                buildCoordinateDto(55.75863461339128, 37.59988106528994),
                buildCoordinateDto(55.76455638500314, 37.60580691616344),
                buildCoordinateDto(55.76797948076131, 37.61372612007092),
                buildCoordinateDto(55.77294294607459, 37.60894520036595),
                buildCoordinateDto(55.7869890973227, 37.59363750663719),
                buildCoordinateDto(55.79219174778563, 37.595388638372384),
                buildCoordinateDto(55.792420145496834, 37.60780217079436),
                buildCoordinateDto(55.791678790601225, 37.63817581533268),
                buildCoordinateDto(55.78008144750799, 37.63498151867729)
        );
    }

    //Граф точек https://yandex.ru/maps/193/voronezh/?from=tabbar&l=sat%2Cskl&ll=39.187710%2C51.808896&mode=whatshere&rl=39.192984%2C51.798587~0.000172%2C0.000492~0.000021%2C0.000373~0.000944%2C0.000373~0.011699%2C0.018621~-0.002017%2C-0.013487~-0.016616%2C-0.058599~-0.008240%2C-0.026061~0.006509%2C-0.046487~0.014248%2C-0.001574~-0.009742%2C-0.006084~-0.010037%2C-0.009622~-0.008926%2C0.004941~-0.017681%2C-0.000307~-0.004372%2C-0.006940~-0.033643%2C-0.000387~0.017831%2C0.005032~-0.018142%2C0.019887~0.042197%2C0.045532~0.014327%2C0.008498~0.017475%2C0.026996~0.016316%2C0.043445~0.008089%2C0.030818~-0.020603%2C-0.034224&source=serp_navig&whatshere%5Bpoint%5D=39.213265%2C51.834048&whatshere%5Bzoom%5D=13&z=14
    public List<CoordinateDto> buildVoronezhScTrack() {
        return List.of(
                buildCoordinateDto(51.798094, 39.191931),
                buildCoordinateDto(51.804824, 39.199929),
                buildCoordinateDto(51.816937, 39.208022),
                buildCoordinateDto(51.756966, 39.187814),
                buildCoordinateDto(51.728100, 39.178373),
                buildCoordinateDto(51.673759, 39.185542),
                buildCoordinateDto(51.672238, 39.199661),
                buildCoordinateDto(51.666154, 39.189962),
                buildCoordinateDto(51.656532, 39.179920),
                buildCoordinateDto(51.661513, 39.170999),
                buildCoordinateDto(51.661193, 39.153275),
                buildCoordinateDto(51.654226, 39.148940),
                buildCoordinateDto(51.653852, 39.115324),
                buildCoordinateDto(51.653852, 39.115324),
                buildCoordinateDto(51.658871, 39.133177),
                buildCoordinateDto(51.678738, 39.114959),
                buildCoordinateDto(51.724317, 39.157145),
                buildCoordinateDto(51.743396, 39.183152),
                buildCoordinateDto(51.803033, 39.204996),
                buildCoordinateDto(51.834048, 39.213265),
                buildCoordinateDto(51.798094, 39.191931)
        );
    }
}
