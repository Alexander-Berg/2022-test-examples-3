package ru.yandex.market.wms.common.spring.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.google.common.collect.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.common.model.enums.ReceiptType;
import ru.yandex.market.wms.common.spring.dao.entity.Receipt;
import ru.yandex.market.wms.common.spring.dao.entity.ReceiptDetailItemIdentities;
import ru.yandex.market.wms.common.spring.dto.ReceiptSkuDto;
import ru.yandex.market.wms.common.spring.dto.SkuDto;
import ru.yandex.market.wms.common.spring.dto.StorerDto;
import ru.yandex.market.wms.receiving.ReceivingIntegrationTest;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;

public class ReceiptServiceIntegrationTest extends ReceivingIntegrationTest {

    @Autowired
    private ReceiptService receiptService;

    @Test
    @DatabaseSetup("/db/service/receipt-service/cis/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/cis/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void upsertReceiptDetailsCis() {
        List<ReceiptSkuDto> detailsDto = new ArrayList<>();
        SkuDto rov0000000000000000358 = SkuDto.builder()
                .sku("ROV0000000000000000358")
                .storer(StorerDto.builder().storerKey("465852").build())
                .build();

        ReceiptDetailItemIdentities cis = getUniversalIdTemplate(Lists.newArrayList("CIS123"), Collections.emptyList());

        List<ReceiptDetailItemIdentities> identities = Collections.singletonList(cis);
        ReceiptSkuDto dto = ReceiptSkuDto.builder()
                .sku(rov0000000000000000358)
                .receivedQty(BigDecimal.ZERO)
                .expectedQty(BigDecimal.ONE)
                .identities(identities)
                .unitPrice(BigDecimal.ONE)
                .name("")
                .build();
        detailsDto.add(dto);
        receiptService.upsertReceiptDetails(
                Receipt.builder().receiptKey("0000012345").build(), detailsDto, "USER");
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/uit/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/uit/after.xml", assertionMode = NON_STRICT_UNORDERED)
    void upsertReceiptDetailsUit() {
        List<ReceiptSkuDto> detailsDto = new ArrayList<>();
        SkuDto rov0000000000000000358 = SkuDto.builder()
                .sku("ROV0000000000000000358")
                .storer(StorerDto.builder().storerKey("465852").build())
                .build();
        ReceiptDetailItemIdentities uit = getUniversalIdTemplate(Lists.newArrayList(), List.of("UIT123"));

        List<ReceiptDetailItemIdentities> identities = Collections.singletonList(uit);
        ReceiptSkuDto dto = ReceiptSkuDto.builder()
                .sku(rov0000000000000000358)
                .receivedQty(BigDecimal.ZERO)
                .expectedQty(BigDecimal.ONE)
                .identities(identities)
                .unitPrice(BigDecimal.ONE)
                .name("")
                .build();
        detailsDto.add(dto);
        receiptService.upsertReceiptDetails(
                Receipt.builder().receiptKey("0000012345").build(), detailsDto, "USER");
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/cis-with-uit/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/cis-with-uit/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void upsertReceiptDetailsCisWithUit() {
        List<ReceiptSkuDto> detailsDto = new ArrayList<>();
        SkuDto rov0000000000000000358 = SkuDto.builder()
                .sku("ROV0000000000000000358")
                .storer(StorerDto.builder().storerKey("465852").build())
                .name("")
                .build();
        ReceiptDetailItemIdentities cisAndUit =
                getUniversalIdTemplate(Lists.newArrayList("CIS123"), List.of("UIT123"));

        List<ReceiptDetailItemIdentities> identities = Collections.singletonList(cisAndUit);
        ReceiptSkuDto dto = ReceiptSkuDto.builder()
                .sku(rov0000000000000000358)
                .receivedQty(BigDecimal.ZERO)
                .expectedQty(BigDecimal.ONE)
                .identities(identities)
                .unitPrice(BigDecimal.ONE)
                .name("")
                .build();
        detailsDto.add(dto);
        receiptService.upsertReceiptDetails(
                Receipt.builder().receiptKey("0000012345").build(), detailsDto, "USER");
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/multiple-cis-with-uit/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/multiple-cis-with-uit/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void upsertReceiptDetailsMultipleCisWithUit() {
        List<ReceiptSkuDto> detailsDto = new ArrayList<>();
        SkuDto rov0000000000000000358 = SkuDto.builder()
                .sku("ROV0000000000000000358")
                .storer(StorerDto.builder().storerKey("465852").build())
                .build();

        ReceiptDetailItemIdentities cisAndUit =
                getUniversalIdTemplate(Lists.newArrayList("CIS123", "CIS456"), List.of("UIT123"));

        List<ReceiptDetailItemIdentities> identities = Collections.singletonList(cisAndUit);
        ReceiptSkuDto dto = ReceiptSkuDto.builder()
                .sku(rov0000000000000000358)
                .receivedQty(BigDecimal.ZERO)
                .expectedQty(BigDecimal.ONE)
                .identities(identities)
                .unitPrice(BigDecimal.ONE)
                .name("")
                .build();
        detailsDto.add(dto);
        receiptService.upsertReceiptDetails(
                Receipt.builder().receiptKey("0000012345").build(), detailsDto, "USER");
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/multiple-cis-no-uit/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/multiple-cis-no-uit/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void upsertReceiptDetailsMultipleCisNoUit() {
        List<ReceiptSkuDto> detailsDto = new ArrayList<>();
        SkuDto rov0000000000000000358 = SkuDto.builder()
                .sku("ROV0000000000000000358")
                .storer(StorerDto.builder().storerKey("465852").build())
                .build();


        ReceiptDetailItemIdentities cis = getUniversalIdTemplate(
                Lists.newArrayList("CIS123", "CIS456"), Collections.emptyList());
        List<ReceiptDetailItemIdentities> identities = Collections.singletonList(cis);
        ReceiptSkuDto dto = ReceiptSkuDto.builder()
                .sku(rov0000000000000000358)
                .receivedQty(BigDecimal.ZERO)
                .expectedQty(BigDecimal.ONE)
                .identities(identities)
                .unitPrice(BigDecimal.ONE)
                .name("")
                .build();
        detailsDto.add(dto);
        receiptService.upsertReceiptDetails(
                Receipt.builder().receiptKey("0000012345").build(), detailsDto, "USER");
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/cis-with-uit-two-structs/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/cis-with-uit-two-structs/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void upsertReceiptDetailsCisWithUitMultipleStructs() {
        List<ReceiptSkuDto> detailsDto = new ArrayList<>();
        SkuDto rov0000000000000000358 = SkuDto.builder()
                .sku("ROV0000000000000000358")
                .storer(StorerDto.builder().storerKey("465852").build())
                .build();
        ReceiptDetailItemIdentities cisAndUit = getUniversalIdTemplate(
                Lists.newArrayList("CIS123"), List.of("UIT123"));
        ReceiptDetailItemIdentities cisAndUit1 = getUniversalIdTemplate(
                Lists.newArrayList("CIS456"), List.of("UIT456"));
        List<ReceiptDetailItemIdentities> identities = new ArrayList<>();
        identities.add(cisAndUit1);
        identities.add(cisAndUit);
        ReceiptSkuDto dto = ReceiptSkuDto.builder()
                .sku(rov0000000000000000358)
                .receivedQty(BigDecimal.ZERO)
                .expectedQty(BigDecimal.ONE)
                .identities(identities)
                .unitPrice(BigDecimal.ONE)
                .name("")
                .build();
        detailsDto.add(dto);
        receiptService.upsertReceiptDetails(
                Receipt.builder().receiptKey("0000012345").build(), detailsDto, "USER");
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/delete/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/delete/after.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void delete() {
        Receipt receipt = Receipt.builder()
                .receiptKey("0000005555")
                .build();
        receiptService.deleteRegistry(receipt);
    }

    @Test
    void getOrderIdReturnsDifferentType() {
        Receipt receipt = Receipt.builder()
                .receiptKey("0000005555")
                .type(ReceiptType.ADDITIONAL)
                .build();
        assertions.assertThat(receiptService.getOrderIdReturns(receipt, null)).isNull();
    }

    @Test
    @DatabaseSetup("/db/service/receipt-service/get-order-returns/before.xml")
    @ExpectedDatabase(value = "/db/service/receipt-service/get-order-returns/before.xml",
            assertionMode = NON_STRICT_UNORDERED)
    void getOrderIdReturns() {
        Receipt receipt = Receipt.builder()
                .receiptKey("0000005555")
                .type(ReceiptType.CUSTOMER_RETURN)
                .build();
        assertions.assertThat(receiptService.getOrderIdReturns(receipt, "box")).isEqualTo("order");
    }

    private ReceiptDetailItemIdentities getUniversalIdTemplate(List<String> cis, List<String> uits) {
        ReceiptDetailItemIdentities universalId = new ReceiptDetailItemIdentities();
        universalId.addCis(cis);
        universalId.addUits(uits);
        return universalId;
    }
}
