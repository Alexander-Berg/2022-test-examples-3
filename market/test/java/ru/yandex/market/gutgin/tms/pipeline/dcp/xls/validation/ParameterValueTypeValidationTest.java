package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.gutgin.tms.config.TestDcpXlsConfig;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.ParameterInfoBuilder;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue;
import ru.yandex.market.partner.content.common.message.MessageInfo;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static ru.yandex.market.gutgin.tms.config.TestDcpXlsConfig.PARAM_ID;
import static ru.yandex.market.gutgin.tms.config.TestDcpXlsConfig.PARAM_ID_2;
import static ru.yandex.market.gutgin.tms.config.TestDcpXlsConfig.PARAM_NAME;
import static ru.yandex.market.gutgin.tms.config.TestDcpXlsConfig.PARAM_NAME_2;

@ContextConfiguration(classes = {TestDcpXlsConfig.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class ParameterValueTypeValidationTest extends BaseDcpExcelValidationTest {
    @Autowired
    private CategoryInfoProducer categoryInfoProducer;

    @Before
    public void setUp() {
        categoryInfoProducer = Mockito.mock(CategoryInfoProducer.class);
        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(
                    ParameterInfoBuilder.asNumeric()
                        .setId(PARAM_ID)
                        .setName(PARAM_NAME)
                        .setXslName("")
                        .setImportContentType(ImportContentType.DCP_EXCEL)
                        .build()
                )
                .addParameter(
                    ParameterInfoBuilder.asNumeric()
                        .setId(PARAM_ID_2)
                        .setName(PARAM_NAME_2)
                        .setXslName("")
                        .setImportContentType(ImportContentType.DCP_EXCEL)
                        .build()
                )
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        validation = new ParameterValueTypeValidation(categoryInfoProducer);
    }

    @Test
    public void shouldFailOnTextInGroupId() {
        List<GcRawSku> invalid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setGroupId("text"))
            .collect(Collectors.toList());

        check(EMPTY, invalid, messages -> {
            assertThat(messages).hasSameSizeAs(invalid);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.integer_expected");
        });
    }

    @Test
    public void shouldFailOnFractionalInGroupId() {
        List<GcRawSku> invalid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setGroupId("123.45"))
            .collect(Collectors.toList());

        check(EMPTY, invalid, messages -> {
            assertThat(messages).hasSameSizeAs(invalid);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.integer_expected");
        });
    }

    @Test
    public void shouldOkOnIntegerInGroupId() {
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setGroupId("123"))
            .collect(Collectors.toList());

        check(valid, EMPTY, NO_MESSAGES);
    }

    @Test
    public void skipSkuWithInnvalidGroupIdAndNoShopSku() {
        List<GcRawSku> input = Stream.of(null, "", "ShopSku3")
                .map(this::createGcRawSku)
                .peek(gcRawSku -> gcRawSku.getData().setGroupId("T-20803V46"))
                .collect(Collectors.toList());

        //1 и 2 RawSku пропускаем, потому что у них нет shop_sku, а без него мы не можем сформировать сообщение об
        //ошибке, для 3 RawSku shop_sku есть, поэтому создаем 1 ошибку integer_expected
        check(input.subList(0, 2), input.subList(2, 3), messages -> {
            assertThat(messages).hasSize(1);
            assertThat(messages).extracting(MessageInfo::getCode)
                    .containsOnly("ir.partner_content.dcp.excel.validation.integer_expected");
            assertThat(messages).extracting(MessageInfo::getParams).extracting(params -> params.get("shopSku"))
                    .containsOnly("ShopSku3");
        });
    }

    @Test
    public void shouldFailOnTextInNumericParameter() {
        List<GcRawSku> invalid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(createRawParamValues("text")))
            .collect(Collectors.toList());

        check(EMPTY, invalid, messages -> {
            assertThat(messages).hasSameSizeAs(invalid);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.number_expected");
        });
    }

    @Test
    public void shouldOkOnFractionalInNumericParameter() {
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(createRawParamValues("123.45", "123,45")))
            .collect(Collectors.toList());

        check(valid, EMPTY, NO_MESSAGES);
    }

    @Test
    public void shouldOkOnIntegerInNumericParameter() {
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(createRawParamValues("123")))
            .collect(Collectors.toList());

        check(valid, EMPTY, NO_MESSAGES);
    }

    @Test
    public void mixedTextAndNumbers() {
        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(createRawParamValues("123")))
            .collect(Collectors.toList());

        List<GcRawSku> invalid = Stream.of("ShopSku4", "ShopSku5", "ShopSku6")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(createRawParamValues("text")))
            .collect(Collectors.toList());

        check(valid, invalid, messages -> {
            assertThat(messages).hasSameSizeAs(invalid);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.number_expected");
        });
    }

    @Test
    public void shouldHaveMessagesForEachError() {
        List<GcRawSku> invalid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(
                Arrays.asList(
                    createRawParamValue(PARAM_ID, PARAM_NAME, "text1"),
                    createRawParamValue(PARAM_ID_2, PARAM_NAME_2, "text2")
                )
            ))
            .collect(Collectors.toList());

        check(EMPTY, invalid, messages -> {
            assertThat(messages).hasSize(invalid.size() * 2);
            assertThat(messages).extracting(MessageInfo::getCode)
                .containsOnly("ir.partner_content.dcp.excel.validation.number_expected");
        });
    }

    @Test
    public void shouldOkWithJustStringParameter() {
        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(
                    ParameterInfoBuilder.asString()
                        .setId(PARAM_ID)
                        .setName(PARAM_NAME)
                        .setXslName("")
                        .setImportContentType(ImportContentType.DCP_EXCEL)
                        .build()
                )
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> valid = Stream.of("ShopSku1", "ShopSku2", "ShopSku3")
            .map(this::createGcRawSku)
            .peek(gcRawSku -> gcRawSku.getData().setRawParamValues(createRawParamValues("text")))
            .collect(Collectors.toList());

        check(valid, EMPTY, NO_MESSAGES);
    }

    private List<RawParamValue> createRawParamValues(String... values) {
        return Arrays.stream(values)
            .map((value -> this.createRawParamValue(PARAM_ID, PARAM_NAME, value)))
            .collect(Collectors.toList());
    }

    private RawParamValue createRawParamValue(long paramId, String paramName, String text) {
        RawParamValue rawParamValue = new RawParamValue();
        rawParamValue.setParamId(paramId);
        rawParamValue.setParamName(paramName);
        rawParamValue.setValue(text);

        return rawParamValue;
    }
}
