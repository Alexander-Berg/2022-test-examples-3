package ru.yandex.common.geocoder.mapper;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import yandex.maps.proto.search.kind.KindOuterClass;

import ru.yandex.common.geocoder.model.response.Kind;


class GeoObjectKindMapperTest {

    @DisplayName("Преобразование из KindProto в Kind")
    @ParameterizedTest(name = "Проверка возможности преобразования Kind.{0} в KindProto.{1}")
    @CsvFileSource(resources = "kind_mapper.csv")
    void fromKindProtoToKindTest(Kind kind, KindOuterClass.Kind protoKind) {
        Assertions.assertThat(kind).isEqualTo(GeoObjectProtoMapper.map(protoKind));
    }
}
