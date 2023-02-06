package ru.yandex.market.tpl.api.advice;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Data;
import lombok.NoArgsConstructor;
import one.util.streamex.StreamEx;

import ru.yandex.market.tpl.common.personal.client.HasPersonalData;

@Data
@NoArgsConstructor
public class HasPersonalDataIndented implements HasPersonalData {

    List<HasPersonalDataTestImpl> personalDataItems;

    @Override
    public <T> StreamEx<T> streamHolders(Function<HasPersonalData, Stream<T>> streamHolderSupplier, Class<T> availableClass) {
        if (personalDataItems == null) {
            return StreamEx.empty();
        }
        return StreamEx.of(personalDataItems).nonNull().flatMap(streamHolderSupplier);
    }
}
