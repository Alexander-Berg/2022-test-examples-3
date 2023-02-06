package ru.yandex.market.pvz.core.test.factory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;
import one.util.streamex.StreamEx;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceipt;
import ru.yandex.market.pvz.core.domain.oebs_receipt.OebsReceiptParams;
import ru.yandex.market.pvz.core.domain.oebs_receipt.downloaded_table.OebsTableDownloadManager;

public class TestOebsReceiptFactory {

    @Autowired
    private OebsTableDownloadManager oebsTableDownloadManager;

    public OebsReceipt create() {
        return create(OebsReceiptTestParams.builder().build());
    }

    public OebsReceipt create(OebsReceiptTestParams params) {
        return create(
                OebsDownloadedTableTestParams.builder()
                        .receipts(List.of(params))
                        .build()
        ).get(0);
    }

    public List<OebsReceipt> create(OebsDownloadedTableTestParams params) {
        return oebsTableDownloadManager.downloadOebsReceipts(
                params.getPath(),
                params.getModificationDate(),
                buildReceipts(params.getReceipts())
        );
    }

    public OebsReceiptParams buildReceipt(OebsReceiptTestParams params) {
        return OebsReceiptParams.builder()
                .oebsNumber(params.getOebsNumber())
                .sum(params.getSum())
                .paymentOrderNumber(params.getPaymentOrderNumber())
                .paymentOrderDate(params.getPaymentOrderDate())
                .virtualAccountNumber(params.getVirtualAccountNumber())
                .uploadedToYtAt(params.getUploadedToYtAt())
                .build();
    }

    public List<OebsReceiptParams> buildReceipts(List<OebsReceiptTestParams> params) {
        return StreamEx.of(params).map(this::buildReceipt).toList();
    }

    @Data
    @Builder
    public static class OebsDownloadedTableTestParams {

        @Builder.Default
        private String path = "//tmp/" + UUID.randomUUID();

        @Builder.Default
        private Instant modificationDate = Instant.EPOCH;

        @Builder.Default
        private List<TestOebsReceiptFactory.OebsReceiptTestParams> receipts = List.of(
                TestOebsReceiptFactory.OebsReceiptTestParams.builder().build()
        );

    }

    @Data
    @Builder
    public static class OebsReceiptTestParams {

        public static final String DEFAULT_PAYMENT_ORDER_NUMBER = "123332132";
        public static final LocalDate DEFAULT_PAYMENT_ORDER_DATE = LocalDate.of(2020, 11, 18);
        public static final String DEFAULT_VIRTUAL_ACCOUNT_NUMBER = "ACCOUNT_987654321";

        @Builder.Default
        private String oebsNumber = String.valueOf(RandomUtils.nextLong());

        @Builder.Default
        private BigDecimal sum = BigDecimal.valueOf(RandomUtils.nextLong(1, 100_000), 2);

        @Builder.Default
        private String paymentOrderNumber = DEFAULT_PAYMENT_ORDER_NUMBER;

        @Builder.Default
        private LocalDate paymentOrderDate = DEFAULT_PAYMENT_ORDER_DATE;

        @Builder.Default
        private String virtualAccountNumber = DEFAULT_VIRTUAL_ACCOUNT_NUMBER;

        @Builder.Default
        private Instant uploadedToYtAt = null;

    }

}
