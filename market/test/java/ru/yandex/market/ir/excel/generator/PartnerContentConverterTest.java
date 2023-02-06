package ru.yandex.market.ir.excel.generator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.LongPredicate;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration_api.util.Conversion;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;


public class PartnerContentConverterTest {

    private PartnerContentConverter partnerContentConverter;

    private static final long BOOK_HID = 101L;
    private static final long NON_BOOK_HID = 102L;
    private static final String ISBN_1 = "978-3-16-148410-0";
    private static final String BARCODE_1 = "1234567890111";
    private static final ModelStorage.Model PSKU_1 = ModelStorage.Model.newBuilder()
        .setId(1L)
        .addTitles(ModelStorage.LocalizedString.newBuilder()
            .setValue("Title")
            .setIsoCode(Conversion.DEFAULT_LANGUAGE_ISO_CODE)
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(MainParamCreator.DESCRIPTION_ID)
            .setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue("Some")
                .setIsoCode(Conversion.DEFAULT_LANGUAGE_ISO_CODE)
                .build())
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(MainParamCreator.ISBN_PARAM_ID)
            .setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue(ISBN_1)
                .setIsoCode(Conversion.DEFAULT_LANGUAGE_ISO_CODE)
                .build())
            .build())
        .addParameterValues(ModelStorage.ParameterValue.newBuilder()
            .setParamId(ParameterValueComposer.BARCODE_ID)
            .setValueType(MboParameters.ValueType.STRING)
            .addStrValue(ModelStorage.LocalizedString.newBuilder()
                .setValue(BARCODE_1)
                .setIsoCode(Conversion.DEFAULT_LANGUAGE_ISO_CODE)
                .build())
            .build())
        .setCurrentType(ModelStorage.ModelType.SKU.name()).build();

    @Before
    public void setUp() {
        CategoryInfoProducer categoryInfoProducer = Mockito.mock(CategoryInfoProducer.class);
        CategoryDataKnowledgeMock categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
        BookCategoryHelper bookCategoryHelper = mock(BookCategoryHelper.class);
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledgeMock, bookCategoryHelper);
        partnerContentConverter = new PartnerContentConverter(categoryDataHelper);
        Mockito.when(bookCategoryHelper.isBookCategory(Mockito.eq(NON_BOOK_HID))).thenReturn(false);
        Mockito.when(bookCategoryHelper.isBookCategory(Mockito.eq(BOOK_HID))).thenReturn(true);
    }

    @Test
    public void convertMboPictureUrl() {
        assertThat(convertPicture("https://url/path/orig")).isEqualTo("https://url/path/orig");
        assertThat(convertPicture("//url/path/orig")).isEqualTo("https://url/path/orig");
    }

    @NotNull
    private String convertPicture(String pictureUrl) {
        return PartnerContentConverter.convertMboPictureUrl(createPicture(pictureUrl));
    }

    @NotNull
    private ModelStorage.Picture createPicture(String s) {
        return ModelStorage.Picture.newBuilder()
            .setUrl(s)
            .build();
    }
}
