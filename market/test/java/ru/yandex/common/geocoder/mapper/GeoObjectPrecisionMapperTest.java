package ru.yandex.common.geocoder.mapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import yandex.maps.proto.search.precision.PrecisionOuterClass;

import ru.yandex.common.geocoder.model.response.Precision;


class GeoObjectPrecisionMapperTest {

    @DisplayName("Преобразование из PrecisionProto в Precision")
    @ParameterizedTest(name = "Проверка возможности преобразования Precision.{0} в PrecisionProto.{1}")
    @CsvFileSource(resources = "precision_mapper.csv")
    void fromPrecisionProtoToPrecisionTest(Precision precision, PrecisionOuterClass.Precision protoPrecision) {
        Assertions.assertThat(precision).isEqualTo(GeoObjectProtoMapper.map(protoPrecision));
    }
}
