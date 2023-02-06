package ru.yandex.market.tpl.billing.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.market.mbi.tariffs.client.TariffClientMetaConverter;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.misc.io.RuntimeIoException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

public final class TariffsUtil {

    private static final ObjectMapper OBJECT_MAPPER =  new ObjectMapper()
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);

    private static final TariffClientMetaConverter META_CONVERTER = new TariffClientMetaConverter(OBJECT_MAPPER);

    /**
     * Загрузить тарифы из переданного файла
     */
    public static List<TariffDTO> loadTariffsFromPath(String path) {
        try {
            TypeFactory typeFactory = OBJECT_MAPPER.getTypeFactory();
            CollectionType collectionType = typeFactory.constructCollectionType(List.class, TariffDTO.class);

            List<TariffDTO> tariffs = OBJECT_MAPPER.readValue(
                    TariffsUtil.class.getResource(path),
                    collectionType
            );

            return tariffs.stream()
                    .map(tariff -> {
                        Class<?> metaClass = META_CONVERTER.getMetaClass(tariff.getServiceType());
                        List<Object> convertedMeta = tariff.getMeta()
                                .stream()
                                .map(obj -> OBJECT_MAPPER.convertValue(obj, metaClass))
                                .collect(Collectors.toList());
                        tariff.setMeta(convertedMeta);
                        return tariff;
                    })
                    .toList();
        } catch (IOException e) {
            throw new RuntimeIoException(e);
        }
    }

    /**
     * Замокать результат вызова похода в тарифницу на тарифы, которые загрузятся из пераданного пути
     */
    public static void mockTariffResponse(TariffService tariffService, String path) {
        doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }
            return TariffsUtil.loadTariffsFromPath(path);
        })).when(tariffService).findTariffs(any(TariffFindQuery.class));
    }

    /**
     * Замокать результат нескольких вызовов похода в тарифницу за тарифами (по разным услугам),
     * которые загрузятся из соответствующих путей
     */
    public static void mockManyTariffResponses(
            TariffService tariffService,
            Map<ServiceTypeEnum, String> pathByTariffService
    ) {
        doAnswer(invocation -> new TariffsIterator((pageNumber, batchSize) -> {
            if (pageNumber != 0) {
                return List.of();
            }

            TariffFindQuery findQuery = invocation.getArgument(0);
            ServiceTypeEnum serviceType = findQuery.getServiceType();

            return TariffsUtil.loadTariffsFromPath(pathByTariffService.get(serviceType));
        })).when(tariffService).findTariffs(any(TariffFindQuery.class));
    }
}
