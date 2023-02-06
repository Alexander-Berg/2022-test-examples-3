package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class DuplicateOffersValidationTest extends BaseDcpExcelValidationTest {

    @Before
    public void setUp() {
        validation = new DuplicateOffersValidation();
    }

    @Test
    public void whenEmptyShopSkuShouldIgnore() {
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", null)
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        check(valid, Collections.emptyList(), messages -> assertThat(messages).isEmpty());
    }

    @Test
    public void whenDuplicatesInShopSkuShouldInvalidate() {
        String duplicateShopSku = "DuplicateShopSku";
        List<GcRawSku> invalid = Stream.of(duplicateShopSku, duplicateShopSku, duplicateShopSku)
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        check(valid, invalid, messages -> {
            assertThat(messages).hasSize(1);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.duplicateShopSku");
            assertThat(messages).extracting(MessageInfo::getLevel)
                    .containsOnly(MessageInfo.Level.ERROR);
        });
    }

    @Test
    public void whenMoreThanOneDuplicatedShopSkuShouldCreateMessageForEachShopSkuOnlyOnce() {
        String duplicateShopSku1 = "DuplicateShopSku1";
        String duplicateShopSku2 = "DuplicateShopSku2";
        List<GcRawSku> invalid = Stream.of(duplicateShopSku1, duplicateShopSku1, duplicateShopSku2, duplicateShopSku2)
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        check(valid, invalid, messages -> {
            assertThat(messages).hasSize(2);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.duplicateShopSku");
            assertThat(messages).extracting(MessageInfo::getLevel)
                    .containsOnly(MessageInfo.Level.ERROR);
        });
    }
}
