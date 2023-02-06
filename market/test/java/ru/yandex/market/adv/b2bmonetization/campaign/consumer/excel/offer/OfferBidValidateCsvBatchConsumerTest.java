package ru.yandex.market.adv.b2bmonetization.campaign.consumer.excel.offer;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.adv.b2bmonetization.AbstractMonetizationTest;
import ru.yandex.market.adv.b2bmonetization.campaign.database.entity.offer.file.PartnerType;
import ru.yandex.market.adv.b2bmonetization.campaign.factory.excel.consumer.OfferBidConsumerFactory;
import ru.yandex.market.adv.b2bmonetization.campaign.model.ErrorInfo;
import ru.yandex.market.adv.b2bmonetization.campaign.model.offer.OfferBidError;
import ru.yandex.market.adv.b2bmonetization.campaign.model.offer.OfferBidInfo;
import ru.yandex.market.adv.b2bmonetization.campaign.yt.entity.ShopOffer;
import ru.yandex.market.adv.b2bmonetization.constant.ErrorKeys;
import ru.yandex.market.adv.model.file.ExcelFileInfo;
import ru.yandex.market.adv.service.excel.ExcelFileService;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

/**
 * Date: 15.02.2022
 * Project: b2bmarketmonetization
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
class OfferBidValidateCsvBatchConsumerTest extends AbstractMonetizationTest {

    private static final BigDecimal MAX_BID = new BigDecimal("100");
    private static final BigDecimal MIN_BID = new BigDecimal("0.5");

    private static final Comparator<OfferBidError> OFFER_BID_ERROR_COMPARATOR = (actual, expected) -> {
        boolean equalBidInfo = actual.getOfferBidInfo().equals(expected.getOfferBidInfo());
        boolean equalCollection = CollectionUtils.isEqualCollection(actual.getErrorInfos(),
                expected.getErrorInfos());

        return equalBidInfo && equalCollection ? 0 : -1;
    };

    @Autowired
    private OfferBidConsumerFactory offerBidConsumerFactory;
    @Autowired
    private ExcelFileService excelFileService;

    @DisplayName("При неверном формате excel файла ничего не удалось прочитать.")
    @Test
    void consume_wrongExcelFormat_nothing() {
        OfferBidValidateCsvBatchConsumer offerBidValidateConsumer =
                offerBidConsumerFactory.createOfferBidValidateConsumer(412L, PartnerType.DBS);

        excelFileService.read(getExcelFileInfo("consume_wrongExcelFormat_nothing.xlsm"), offerBidValidateConsumer);

        Assertions.assertThat(offerBidValidateConsumer.getOfferErrorInfos())
                .isEmpty();
        Assertions.assertThat(offerBidValidateConsumer.getTotalOfferCount())
                .isEqualTo(0);
        Assertions.assertThat(offerBidValidateConsumer.getErrorOfferCount())
                .isEqualTo(0);
        Assertions.assertThat(offerBidValidateConsumer.getWarnOfferCount())
                .isEqualTo(0);
        Assertions.assertThat(offerBidValidateConsumer.getMinBid())
                .isNull();
        Assertions.assertThat(offerBidValidateConsumer.getMaxBid())
                .isNull();
    }

    @DisplayName("При попытке прочитать не excel файл, вернулось исключение.")
    @Test
    void consume_notExcelFile_exception() {
        Assertions.assertThatThrownBy(() ->
                        excelFileService.read(
                                getExcelFileInfo("consume_notExcelFile_exception.xlsm"),
                                offerBidConsumerFactory.createOfferBidValidateConsumer(412L, PartnerType.DBS)
                        )
                )
                .isInstanceOf(UncheckedIOException.class);
    }

    @DisplayName("Если файл не нашелся, вернулось исключение.")
    @Test
    void consume_fileNotFound_exception() {
        Assertions.assertThatThrownBy(() ->
                        excelFileService.read(
                                getExcelFileInfo("consume_fileNotFound_exception.xlsm"),
                                offerBidConsumerFactory.createOfferBidValidateConsumer(412L, PartnerType.DBS)
                        )
                )
                .isInstanceOf(UncheckedIOException.class);
    }

    @DisplayName("При верном формате excel файла содержит более 100 ошибок.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/consume_correctFile_allError_shop_offer"
            ),
            before = "OfferBidValidateCsvBatchConsumer/json/yt/consume_correctFile_allError.before.json"
    )
    @Test
    void consume_correctFile_allError() {
        run("consume_correctFile_allError_",
                () -> {
                    OfferBidValidateCsvBatchConsumer offerBidValidateConsumer =
                            offerBidConsumerFactory.createOfferBidValidateConsumer(412L, PartnerType.DBS);

                    excelFileService.read(
                            getExcelFileInfo("consume_correctFile_allError.xlsm"),
                            offerBidValidateConsumer
                    );

                    List<OfferBidError> offerBidErrors = new ArrayList<>();
                    for (int i = 1; i <= 100; i++) {
                        offerBidErrors.add(
                                createOfferBidError(i, "", "", null, null,
                                        List.of(
                                                ErrorInfo.builder()
                                                        .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                        .params(
                                                                Map.of(
                                                                        "max_bid", MAX_BID.toPlainString(),
                                                                        "min_bid", MIN_BID.toPlainString(),
                                                                        "bid", ""
                                                                )
                                                        )
                                                        .build(),
                                                ErrorInfo.builder()
                                                        .errorKey(ErrorKeys.OFFER_EMPTY_SKU)
                                                        .params(Map.of())
                                                        .build()
                                        )
                                )
                        );
                    }

                    Assertions.assertThat(offerBidValidateConsumer.getOfferErrorInfos())
                            .usingElementComparator(OFFER_BID_ERROR_COMPARATOR)
                            .containsExactlyInAnyOrderElementsOf(offerBidErrors);

                    Assertions.assertThat(offerBidValidateConsumer.getTotalOfferCount())
                            .isEqualTo(251);
                    Assertions.assertThat(offerBidValidateConsumer.getErrorOfferCount())
                            .isEqualTo(240);
                    Assertions.assertThat(offerBidValidateConsumer.getWarnOfferCount())
                            .isEqualTo(5);
                    Assertions.assertThat(offerBidValidateConsumer.getMinBid())
                            .isEqualTo(new BigDecimal("0.55"));
                    Assertions.assertThat(offerBidValidateConsumer.getMaxBid())
                            .isEqualTo(new BigDecimal("0.65"));
                }
        );
    }

    @DisplayName("При верном формате excel файла вычитали все данные. DBS.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/consume_correctFile_allOffer_shop_offer"
            ),
            before = "OfferBidValidateCsvBatchConsumer/json/yt/consume_correctFile_allOffer.before.json"
    )
    @Test
    void consume_correctFile_allOffer_dbs() {
        consume_correctFile_allOffer(PartnerType.DBS);
    }

    @DisplayName("При верном формате excel файла вычитали все данные. FB.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = ShopOffer.class,
                    path = "//tmp/consume_correctFile_allOffer_blue_shop_offer"
            ),
            before = "OfferBidValidateCsvBatchConsumer/json/yt/consume_correctFile_allOffer.before.json"
    )
    @Test
    void consume_correctFile_allOffer_fb() {
        consume_correctFile_allOffer(PartnerType.FB);
    }

    private void consume_correctFile_allOffer(PartnerType partnerType) {
        run("consume_correctFile_allOffer_",
                () -> {
                    OfferBidValidateCsvBatchConsumer offerBidValidateConsumer =
                            offerBidConsumerFactory.createOfferBidValidateConsumer(412L, partnerType);

                    excelFileService.read(
                            getExcelFileInfo("consume_correctFile_allOffer.xlsm"),
                            offerBidValidateConsumer
                    );

                    Assertions.assertThat(offerBidValidateConsumer.getOfferErrorInfos())
                            .usingElementComparator(OFFER_BID_ERROR_COMPARATOR)
                            .containsExactlyInAnyOrder(
                                    createOfferBidError(4, "", "", null, null,
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                            .params(
                                                                    Map.of(
                                                                            "max_bid", MAX_BID.toPlainString(),
                                                                            "min_bid", MIN_BID.toPlainString(),
                                                                            "bid", ""
                                                                    )
                                                            )
                                                            .build(),
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_EMPTY_SKU)
                                                            .params(Map.of())
                                                            .build()
                                            )
                                    ),
                                    createOfferBidError(5, "", "", "21.94", "437.52",
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_EMPTY_SKU)
                                                            .params(Map.of())
                                                            .build()
                                            )
                                    ),
                                    createOfferBidError(7, "3212", "Твой товар 3", "7511.00", "442.52",
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                            .params(
                                                                    Map.of(
                                                                            "max_bid", MAX_BID.toPlainString(),
                                                                            "min_bid", MIN_BID.toPlainString(),
                                                                            "bid", "7511.00"
                                                                    )
                                                            )
                                                            .build()
                                            )
                                    ),
                                    createOfferBidError(8, "", "Твой товар 4", "4342.00", "443",
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                            .params(
                                                                    Map.of(
                                                                            "max_bid", MAX_BID.toPlainString(),
                                                                            "min_bid", MIN_BID.toPlainString(),
                                                                            "bid", "4342.00"
                                                                    )
                                                            )
                                                            .build(),
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_EMPTY_SKU)
                                                            .params(Map.of())
                                                            .build()
                                            )
                                    ),
                                    createOfferBidError(9, "", "Твой товар 5", null, "4443.3241",
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                            .params(
                                                                    Map.of(
                                                                            "max_bid", MAX_BID.toPlainString(),
                                                                            "min_bid", MIN_BID.toPlainString(),
                                                                            "bid", ""
                                                                    )
                                                            )
                                                            .build(),
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_EMPTY_SKU)
                                                            .params(Map.of())
                                                            .build()
                                            )
                                    ),
                                    createOfferBidError(10, "5444", "Твой товар 6", null, null,
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                            .params(
                                                                    Map.of(
                                                                            "max_bid", MAX_BID.toPlainString(),
                                                                            "min_bid", MIN_BID.toPlainString(),
                                                                            "bid", ""
                                                                    )
                                                            )
                                                            .build()
                                            )
                                    ),
                                    createOfferBidError(11, "5445", "Твой товар 7", null, "446.52",
                                            List.of(
                                                    ErrorInfo.builder()
                                                            .errorKey(ErrorKeys.OFFER_INVALID_BID)
                                                            .params(
                                                                    Map.of(
                                                                            "max_bid", MAX_BID.toPlainString(),
                                                                            "min_bid", MIN_BID.toPlainString(),
                                                                            "bid", ""
                                                                    )
                                                            )
                                                            .build()
                                            )
                                    )
                            );
                    Assertions.assertThat(offerBidValidateConsumer.getTotalOfferCount())
                            .isEqualTo(13);
                    Assertions.assertThat(offerBidValidateConsumer.getErrorOfferCount())
                            .isEqualTo(7);
                    Assertions.assertThat(offerBidValidateConsumer.getWarnOfferCount())
                            .isEqualTo(2);
                    Assertions.assertThat(offerBidValidateConsumer.getMinBid())
                            .isEqualTo(new BigDecimal("0.50"));
                    Assertions.assertThat(offerBidValidateConsumer.getMaxBid())
                            .isEqualTo(new BigDecimal("100.00"));
                }
        );
    }

    private ExcelFileInfo getExcelFileInfo(String systemName) {
        return ExcelFileInfo.builder()
                .systemName("campaign/consumer/excel/offer/OfferBidValidateCsvBatchConsumer/excel/" + systemName)
                .build();
    }

    private OfferBidError createOfferBidError(long position, @Nullable String shopSku, @Nullable String name,
                                              @Nullable String bid, @Nullable String price,
                                              Collection<ErrorInfo> errorInfos) {
        return OfferBidError.builder()
                .offerBidInfo(
                        OfferBidInfo
                                .builder()
                                .position(position)
                                .sku(shopSku)
                                .name(name)
                                .bid(bid == null ? null : new BigDecimal(bid))
                                .price(price == null ? null : new BigDecimal(price))
                                .build()
                )
                .errorInfos(errorInfos)
                .build();
    }
}
