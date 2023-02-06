package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class HasShopSkuDcpExcelValidationTest extends BaseDcpExcelValidationTest {

    @Before
    public void setUp() {
        validation = new HasShopSkuValidation();
    }

    @Test
    public void whenHasShopSkuShouldBeValid() {
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        check(valid, EMPTY, NO_MESSAGES);
    }

    @Test
    public void whenNoShopSkuShouldBeInvalid() {
        List<GcRawSku> invalid = Stream.of("", null, "")
            .map(this::createGcRawSku)
            .collect(Collectors.toList());


        check(EMPTY, invalid, messages -> {
            assertThat(messages).hasSameSizeAs(invalid);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.noShopSkuInFile");
        });
    }

    @Test
    public void mixed() {
        List<GcRawSku> invalid = Stream.of("", null, "")
            .map(this::createGcRawSku)
            .collect(Collectors.toList());
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .collect(Collectors.toList());

        check(valid, invalid, messages -> {
            assertThat(messages).hasSameSizeAs(invalid);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.noShopSkuInFile");
        });
    }
}