package ru.yandex.market.core.feed.validation.result;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.core.feed.offer.united.UnitedOffer;
import ru.yandex.market.core.indexer.model.IndexerError;
import ru.yandex.market.core.indexer.model.IndexerErrorLevel;
import ru.yandex.market.core.indexer.model.OfferPosition;
import ru.yandex.market.core.supplier.model.IndexerErrorInfo;
import ru.yandex.market.core.supplier.model.OfferInfo;

import static ru.yandex.common.util.collections.CollectionFactory.list;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildEmptyUnitedOffer;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildRequiredUnitedOffer;
import static ru.yandex.market.core.offer.model.united.UnitedOfferTestUtil.buildUnitedOffer;

/**
 * Класс утилита для тестирования результатов валидации
 * Date: 03.12.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
public class ValidationResultTestUtil {

    private ValidationResultTestUtil() {
    }

    @Nonnull
    public static Collection<OfferInfo> getValidationResultWithoutErrorTestCollection() {
        return getValidationResultWithoutErrorTestCollection(true);
    }

    @Nonnull
    public static Collection<OfferInfo> getValidationResultWithoutErrorTestCollection(boolean withValidPicUrl) {
        return List.of(
                buildValidationResult("offer1",
                        buildUnitedOffer(null, "offer1", withValidPicUrl),
                        Collections.emptyList()),
                buildValidationResult("offer4",
                        buildRequiredUnitedOffer(null, "offer4", withValidPicUrl),
                        Collections.emptyList()),
                buildValidationResult("offer5",
                        buildUnitedOffer(null, "offer5", withValidPicUrl),
                        Collections.emptyList())
        );
    }

    @Nonnull
    public static Collection<OfferInfo> getValidationResultOnlyErrorTestCollection() {
        return List.of(
                buildValidationResult("300",
                        null,
                        list(
                                buildIndexerErrorInfo(
                                        "300",
                                        "35B",
                                        "Не указана характеристика товара age",
                                        "Укажите эту характеристику с помощью элемента param.",
                                        null,
                                        IndexerErrorLevel.WARNING,
                                        OfferPosition.of(55, 9)
                                ),
                                buildIndexerErrorInfo(
                                        "300",
                                        "35B",
                                        "Не указана характеристика товара price",
                                        "Укажите эту характеристику с помощью элемента param.",
                                        null,
                                        IndexerErrorLevel.WARNING,
                                        OfferPosition.of(66, 9)
                                )
                        )),
                buildValidationResult("301",
                        null,
                        list(
                                buildIndexerErrorInfo(
                                        "301",
                                        "35B",
                                        "Не указана характеристика товара age",
                                        "Укажите эту характеристику с помощью элемента param.",
                                        null,
                                        IndexerErrorLevel.ERROR,
                                        OfferPosition.of(66, 18)
                                )
                        )),
                buildValidationResult("",
                        null,
                        list(
                                buildIndexerErrorInfo(
                                        "",
                                        "451",
                                        "Не все предложения удастся опубликовать по модели CPA",
                                        "Поправьте предложения которые нельзя опубликовать.",
                                        null,
                                        IndexerErrorLevel.ERROR,
                                        OfferPosition.of(0, 0)
                                )
                        )),
                buildValidationResult("",
                        null,
                        list(
                                buildIndexerErrorInfo(
                                        "",
                                        "393",
                                        "Есть одинаковые предложения",
                                        "Дубликаты нужно удалить.",
                                        "age",
                                        IndexerErrorLevel.FATAL,
                                        OfferPosition.of(77, 9)
                                )
                        ))
        );
    }

    @Nonnull
    public static Collection<OfferInfo> getValidationResultTestCollection(boolean withEmptyOffer,
                                                                          boolean withValidPicUrl
    ) {
        return List.of(
                buildValidationResult("300",
                        buildUnitedOffer(0, "300", withValidPicUrl),
                        list(
                                buildIndexerErrorInfo(
                                        "300",
                                        "35B",
                                        "Не указана характеристика товара age",
                                        "Укажите эту характеристику с помощью элемента param.",
                                        null,
                                        IndexerErrorLevel.WARNING,
                                        OfferPosition.of(55, 9)
                                ),
                                buildIndexerErrorInfo(
                                        "300",
                                        "35B",
                                        "Не указана характеристика товара price",
                                        "Укажите эту характеристику с помощью элемента param.",
                                        null,
                                        IndexerErrorLevel.WARNING,
                                        OfferPosition.of(66, 9)
                                )
                        )),
                buildValidationResult("301",
                        buildRequiredUnitedOffer(0, "301", withValidPicUrl),
                        list(
                                buildIndexerErrorInfo(
                                        "301",
                                        "35B",
                                        "Не указана характеристика товара age",
                                        "Укажите эту характеристику с помощью элемента param.",
                                        null,
                                        IndexerErrorLevel.ERROR,
                                        OfferPosition.of(66, 18)
                                )
                        )),
                buildValidationResult("303",
                        buildUnitedOffer(0, "303", withValidPicUrl),
                        Collections.emptyList()
                ),
                buildValidationResult("",
                        withEmptyOffer ? buildEmptyUnitedOffer(null, null) : null,
                        list(
                                buildIndexerErrorInfo(
                                        "",
                                        "451",
                                        "Не все предложения удастся опубликовать по модели CPA",
                                        "Поправьте предложения которые нельзя опубликовать.",
                                        null,
                                        IndexerErrorLevel.ERROR,
                                        OfferPosition.of(0, 0)
                                )
                        )),
                buildValidationResult("",
                        withEmptyOffer ? buildEmptyUnitedOffer(null, null) : null,
                        list(
                                buildIndexerErrorInfo(
                                        "",
                                        "393",
                                        "Есть одинаковые предложения",
                                        "Дубликаты нужно удалить.",
                                        "age",
                                        IndexerErrorLevel.FATAL,
                                        OfferPosition.of(77, 9)
                                )
                        ))
        );
    }

    @Nonnull
    public static IndexerError buildIndexerError(String shopSku,
                                                 String code,
                                                 String position,
                                                 IndexerErrorLevel level,
                                                 String details) {
        return new IndexerError.Builder()
                .setShopSku(shopSku)
                .setCode(code)
                .setPosition(position)
                .setLevel(level)
                .setDetails(details)
                .build();
    }

    @Nonnull
    public static OfferInfo buildValidationResult(String shopSku,
                                                  UnitedOffer unitedOffer,
                                                  List<IndexerErrorInfo> errorInfos) {
        return OfferInfo.builder()
                .withShopSku(shopSku)
                .withUnitedOffer(unitedOffer)
                .withIndexerErrorInfos(errorInfos.stream()
                        .filter(error -> error.getLevel() != IndexerErrorLevel.WARNING && !shopSku.isEmpty())
                        .collect(Collectors.toList()))
                .withIndexerWarningInfos(errorInfos.stream()
                        .filter(error -> error.getLevel() == IndexerErrorLevel.WARNING || shopSku.isEmpty())
                        .collect(Collectors.toList()))
                .build();
    }

    @Nonnull
    public static IndexerErrorInfo buildIndexerErrorInfo(String shopSku,
                                                         String verdictCode,
                                                         String description,
                                                         String recommendation,
                                                         String wrongValue,
                                                         IndexerErrorLevel level,
                                                         OfferPosition position) {
        return IndexerErrorInfo.builder()
                .withShopSku(shopSku)
                .withVerdictCode(verdictCode)
                .withDescription(description)
                .withRecommendation(recommendation)
                .withWrongValue(wrongValue)
                .withLevel(level)
                .withPosition(position)
                .build();
    }
}
