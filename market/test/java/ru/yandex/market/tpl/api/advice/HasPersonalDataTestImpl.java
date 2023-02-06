package ru.yandex.market.tpl.api.advice;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import one.util.streamex.StreamEx;

import ru.yandex.market.tpl.common.personal.client.HasPersonalData;

@Data
@RequiredArgsConstructor
public class HasPersonalDataTestImpl extends AbstractClassForPdReturn implements HasPersonalData {

    List<HasPersonalFieldTestImpl> personalDataDtos;

    @Override
    public <T> StreamEx<T> streamHolders(Function<HasPersonalData, Stream<T>> streamHolderSupplier, Class<T> availableClass) {
        if (personalDataDtos == null) {
            return StreamEx.empty();
        }
        return StreamEx.of(personalDataDtos).nonNull().select(availableClass);
    }
}
