package ru.yandex.market.checkout.checkouter.feature.type;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.CollectionFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.IntegerFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.MapFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.common.StringFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.logging.LoggingBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.logging.LoggingMapFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentBooleanFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentCollectionFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentComplexFeatureType;
import ru.yandex.market.checkout.checkouter.feature.type.permanent.PermanentIntegerFeatureType;

public class NamedFeatureTypeRegisterTest {
    @Test
    public void findFeaturesByName() {
        Arrays.stream(LoggingBooleanFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.loggingFeatureByName(value.getName())));

        Arrays.stream(LoggingMapFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.loggingFeatureByName(value.getName())));

        Arrays.stream(BooleanFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.commonFeatureByName(value.getName())));

        Arrays.stream(CollectionFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.commonFeatureByName(value.getName())));

        Arrays.stream(ComplexFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.commonFeatureByName(value.getName())));

        Arrays.stream(IntegerFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.commonFeatureByName(value.getName())));

        Arrays.stream(MapFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.commonFeatureByName(value.getName())));

        Arrays.stream(StringFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.commonFeatureByName(value.getName())));

        Arrays.stream(PermanentComplexFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.permanentFeatureByName(value.getName())));

        Arrays.stream(PermanentBooleanFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.permanentFeatureByName(value.getName())));

        Arrays.stream(PermanentCollectionFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.permanentFeatureByName(value.getName())));

        Arrays.stream(PermanentIntegerFeatureType.values()).forEach(value ->
                Assertions.assertEquals(value, NamedFeatureTypeRegister.permanentFeatureByName(value.getName())));
    }
}
