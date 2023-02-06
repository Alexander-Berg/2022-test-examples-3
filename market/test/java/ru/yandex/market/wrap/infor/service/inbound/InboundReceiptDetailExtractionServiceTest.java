package ru.yandex.market.wrap.infor.service.inbound;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import ru.yandex.market.wrap.infor.entity.ReceiptDetail;
import ru.yandex.market.wrap.infor.entity.ReceiptDetailItem;
import ru.yandex.market.wrap.infor.entity.ReceiptStatusType;
import ru.yandex.market.wrap.infor.entity.LocStatus;
import ru.yandex.market.wrap.infor.model.InboundReceiptDetailKey;
import ru.yandex.market.wrap.infor.model.InboundReceiptDetailRow;
import ru.yandex.market.wrap.infor.model.InboundReceiptDetailRowBuilder;
import ru.yandex.market.wrap.infor.repository.ReceiptDetailRepository;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@SuppressWarnings("checkstyle:methodlength")
class InboundReceiptDetailExtractionServiceTest {
    private static final String STORER_KEY = "TEST_STORER";
    private static final String RECEIPT_KEY = "TEST_RECEIPT";
    private static final String ORDER_NUMBER = null;
    private static final String MASTER_SKU = "MASTER_SKU";
    private static final String MASTER_SKU1 = "MASTER_SKU1";
    private static final String BOM_PREFIX = "BOM";
    private static final String BOM1 = "BOM1";
    private static final String BOM2 = "BOM2";
    private static final BigDecimal SEVERAL_ITEMS = BigDecimal.valueOf(3);

    private ReceiptDetailRepository mockRepository = Mockito.mock(ReceiptDetailRepository.class);
    private SingleReceiptDetailExtractionService extractionService =
        new SingleReceiptDetailExtractionService(mockRepository);

    static Stream<Arguments> data() {
        return Stream.of(
            Arguments.of(
                "Check single item, 1 expected, 1 received",
                getRows(
                    getStageRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE)
                ),
                Collections.emptyMap(),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE)
                )
            ),
            Arguments.of("Check single item, 1 expected, 1 damaged",
                getRows(
                    getStageRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE)
                ),
                Collections.emptyMap(),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE)
                )
            ),
            Arguments.of("Check single item, 1 expected, 0 received",
                getRows(
                    getStageRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO)
                ),
                Collections.emptyMap(),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO)
                )
            ),
            Arguments.of("Check two items, first: 1 expected, 1 received; second: 1 damaged check max status",
                getRows(
                    getStageRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE, ReceiptStatusType.CLOSED),
                    getStageRow(MASTER_SKU1, BigDecimal.ONE, BigDecimal.ZERO, ReceiptStatusType.CLOSED),
                    getDamagedRow(MASTER_SKU1, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED)
                ),
                Collections.emptyMap(),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE),
                    getDamageItem(MASTER_SKU1, BigDecimal.ONE),
                    getStageItem(MASTER_SKU1, BigDecimal.ONE, BigDecimal.ZERO)
                )
            ),
            Arguments.of("Check surplus",
                getRows(
                    getStageRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE, ReceiptStatusType.CLOSED),
                    getStageSurplusRow(MASTER_SKU, BigDecimal.ONE)
                ),
                Collections.emptyMap(),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE),
                    getSurplusItems(MASTER_SKU, 1)
                )
            ),
            Arguments.of("Check damaged with surplus",
                getRows(
                    getStageRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE, ReceiptStatusType.CLOSED),
                    getStageSurplusRow(MASTER_SKU, BigDecimal.ONE),
                    getDamagedRowWithSurplus(MASTER_SKU, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED)
                ),
                Collections.emptyMap(),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getSurplusDamagedItem(MASTER_SKU, 1),
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE),
                    getSurplusItems(MASTER_SKU, 1)
                )
            ),
            Arguments.of("Check multipart item",
                getRows(
                    getStageCompoundRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO),

                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    )
                ),
                getKitsForSingleMaster(
                    MASTER_SKU, 2
                ),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE)
                )
            ),
            Arguments.of("Check multipart item, one part damaged",
                getRows(
                    getStageCompoundRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO),

                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.HOLD, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    )
                ),
                getKitsForSingleMaster(
                    MASTER_SKU, 2
                ),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getDamageItem(MASTER_SKU, BigDecimal.ONE),
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO)
                )
            ),
            Arguments.of("Check multipart item, 1 full kit, 1 damaged, 1 missing ",
                getRows(
                    getStageCompoundRow(MASTER_SKU, SEVERAL_ITEMS, BigDecimal.ZERO),

                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.HOLD, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    )
                ),
                getKitsForSingleMaster(
                    MASTER_SKU, 2
                ),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getDamageItem(MASTER_SKU, BigDecimal.ONE),
                    getStageItem(MASTER_SKU, SEVERAL_ITEMS, BigDecimal.ONE)
                )
            ),
            Arguments.of("Check multipart item, 1 full kit, 1 surplus ",
                getRows(
                    getStageCompoundRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO),

                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), true
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), true
                    )
                ),
                getKitsForSingleMaster(
                    MASTER_SKU, 2
                ),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE),
                    getSurplusItems(MASTER_SKU, 1)
                )
            ),
            Arguments.of(
                "Check multipart item, 1 full kit, 1 surplus but damaged ",
                getRows(
                    getStageCompoundRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO),

                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.HOLD, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), true
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), true
                    )
                ),
                getKitsForSingleMaster(
                    MASTER_SKU, 2
                ),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getDamageItem(MASTER_SKU, BigDecimal.ONE),
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE)
                )
            ),
            Arguments.of(
                "Check both single and multi items ",
                getRows(
                    getStageCompoundRow(MASTER_SKU, BigDecimal.ONE, BigDecimal.ZERO),
                    getStageRow(MASTER_SKU1, BigDecimal.ONE, BigDecimal.ZERO),
                    getDamagedRow(MASTER_SKU1, BigDecimal.ONE),

                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), false
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM1, LocStatus.HOLD, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), true
                    ),
                    getComponentRow(MASTER_SKU,
                        BOM2, LocStatus.OK, BigDecimal.ONE, ReceiptStatusType.VERIFIED_CLOSED.getCode(), true
                    )
                ),
                getKitsForSingleMaster(
                    MASTER_SKU, 2
                ),
                ReceiptStatusType.VERIFIED_CLOSED,
                getItems(
                    getDamageItem(MASTER_SKU, BigDecimal.ONE),
                    getStageItem(MASTER_SKU, BigDecimal.ONE, BigDecimal.ONE),
                    getDamageItem(MASTER_SKU1, BigDecimal.ONE),
                    getStageItem(MASTER_SKU1, BigDecimal.ONE, BigDecimal.ZERO)
                )
            )
        );
    }


    @MethodSource("data")
    @ParameterizedTest(name = "{index} : {0}.")
    void runTest(
        String name,
        List<InboundReceiptDetailRow> mockRows,
        Map<InboundReceiptDetailKey, Set<String>> mockKits,
        ReceiptStatusType expectedStatus,
        List<ReceiptDetailItem> expectedItems
    ) {
        doReturn(mockRows).when(mockRepository).findItems(anyString());
        doReturn(mockKits).when(mockRepository).findKits(anyString());

        Optional<ReceiptDetail> details = extractionService.getDetailsForReceipt("DO");

        SoftAssertions.assertSoftly(softAssertions -> {
            softAssertions.assertThat(details.isPresent()).isEqualTo(!mockRows.isEmpty());
            if (details.isPresent()) {
                ReceiptStatusType actualStatus = details.get().getStatusType();
                softAssertions.assertThat(actualStatus).isEqualTo(expectedStatus);
                List<ReceiptDetailItem> actualItems = details.get().getItems();
                softAssertions.assertThat(actualItems.size()).isEqualTo(expectedItems.size());
                for (int i = 0; i < expectedItems.size(); i++) {
                    softAssertions.assertThat(actualItems.get(i)).isEqualTo(expectedItems.get(i));
                }
            }
        });

    }

    private static Map<InboundReceiptDetailKey, Set<String>> getKitsForSingleMaster(
        String masterSku, int componentsCount
    ) {
        Map<InboundReceiptDetailKey, Set<String>> result = new HashMap<>();
        Set<String> components = new HashSet<>();
        for (int i = 1; i <= componentsCount; i++) {
            components.add(BOM_PREFIX + i);
        }
        InboundReceiptDetailKey key = new InboundReceiptDetailKey(RECEIPT_KEY, masterSku, STORER_KEY, ORDER_NUMBER);
        result.put(key, components);
        return result;
    }

    private static List<InboundReceiptDetailRow> getRows(InboundReceiptDetailRow... rows) {
        return Arrays.asList(rows);
    }

    private static InboundReceiptDetailRow getStageCompoundRow(String sku, BigDecimal expected, BigDecimal received) {
        return getRow(
            sku, LocStatus.OK, expected, received, ReceiptStatusType.VERIFIED_CLOSED.getCode(), 2L, false
        );
    }

    private static InboundReceiptDetailRow getStageSurplusRow(String sku, BigDecimal received) {
        return getRow(sku, LocStatus.OK, BigDecimal.ZERO,
            received, ReceiptStatusType.VERIFIED_CLOSED.getCode(), 0L, true);
    }

    private static InboundReceiptDetailRow getStageRow(String sku,
                                                       BigDecimal expected,
                                                       BigDecimal received) {
        return getStageRow(sku, expected, received, ReceiptStatusType.VERIFIED_CLOSED);
    }

    private static InboundReceiptDetailRow getStageRow(String sku,
                                                       BigDecimal expected,
                                                       BigDecimal received,
                                                       ReceiptStatusType statusType) {
        return getRow(sku, LocStatus.OK, expected, received,
            statusType.getCode(), 0L, false);
    }

    private static InboundReceiptDetailRow getDamagedRow(String sku,
                                                         BigDecimal received) {
        return getDamagedRow(sku, received, ReceiptStatusType.VERIFIED_CLOSED);
    }

    private static InboundReceiptDetailRow getDamagedRow(String sku,
                                                         BigDecimal received,
                                                         ReceiptStatusType statusType) {
        return getRow(sku, LocStatus.HOLD, BigDecimal.ZERO, received,
            statusType.getCode(), 0L, false);
    }

    private static InboundReceiptDetailRow getDamagedRowWithSurplus(String sku,
                                                                    BigDecimal received,
                                                                    ReceiptStatusType statusType) {
        return getRow(sku, LocStatus.HOLD, BigDecimal.ZERO, received,
            statusType.getCode(), 0L, true);
    }

    private static InboundReceiptDetailRow getRow(String sku,
                                                  LocStatus tolocStatus,
                                                  BigDecimal expected,
                                                  BigDecimal received,
                                                  int status,
                                                  long componentsAmount,
                                                  boolean isSurplus) {
        return new InboundReceiptDetailRowBuilder(sku, tolocStatus, status, ORDER_NUMBER)
            .setStorerKey(STORER_KEY)
            .setReceiptKey(RECEIPT_KEY)
            .setExpected(expected)
            .setReceived(received)
            .setStatus(status)
            .setSurplus(isSurplus)
            .setComponentsAmount(componentsAmount)
            .build();
    }

    private static InboundReceiptDetailRow getComponentRow(String masterSku,
                                                           String sku,
                                                           LocStatus tolocStatus,
                                                           BigDecimal received,
                                                           int status,
                                                           boolean isSurplus) {
        return new InboundReceiptDetailRowBuilder(sku, tolocStatus, status, ORDER_NUMBER)
            .setStorerKey(STORER_KEY)
            .setReceiptKey(RECEIPT_KEY)
            .setMasterSku(masterSku)
            .setReceived(received)
            .setSurplus(isSurplus)
            .build();
    }


    private static List<ReceiptDetailItem> getItems(ReceiptDetailItem... items) {
        return new ArrayList<>(Arrays.asList(items));
    }

    private static ReceiptDetailItem getUnknownItem(String sku, BigDecimal received) {
        return getItem(sku, BigDecimal.ZERO, received, LocStatus.UNKNOWN);
    }

    private static ReceiptDetailItem getDamageItem(String sku, BigDecimal received) {
        return getItem(sku, BigDecimal.ZERO, received, LocStatus.HOLD);
    }

    private static ReceiptDetailItem getStageItem(String sku, BigDecimal expected, BigDecimal received) {
        return getItem(sku, expected, received, LocStatus.OK);
    }

    private static ReceiptDetailItem getItem(String sku, BigDecimal expected, BigDecimal received, LocStatus tolocStatus) {
        return new ReceiptDetailItem(
            new InboundReceiptDetailKey(
                RECEIPT_KEY,
                sku,
                STORER_KEY,
                ORDER_NUMBER
            ),
            expected,
            received,
            BigDecimal.ZERO,
            tolocStatus
        );
    }

    private static ReceiptDetailItem getSurplusItems(String sku, long received) {
        return new ReceiptDetailItem(
            new InboundReceiptDetailKey(
                RECEIPT_KEY,
                sku,
                STORER_KEY,
                ORDER_NUMBER
            ),
            BigDecimal.ZERO,
            BigDecimal.valueOf(received),
            BigDecimal.valueOf(received),
            LocStatus.OK
        );
    }

    private static ReceiptDetailItem getSurplusDamagedItem(String sku, long received) {
        return new ReceiptDetailItem(
            new InboundReceiptDetailKey(
                RECEIPT_KEY,
                sku,
                STORER_KEY,
                ORDER_NUMBER
            ),
            BigDecimal.ZERO,
            BigDecimal.valueOf(received),
            BigDecimal.valueOf(received),
            LocStatus.HOLD
        );
    }

}
