package ru.yandex.market.ir.autogeneration_api.util;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.http.AutoGenerationApi;
import ru.yandex.market.ir.http.AutoGenerationApi.ParameterType;
import ru.yandex.market.ir.http.AutoGenerationApi.VendorModel;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.mbo.http.ModelStorage.Model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ModelConvertUtilsTest {

    @Test
    public void convertModelPicturesFromArray() {
        String url1 = "url";
        String sourceUrl1 = "sourceUrl";
        String url2 = "url2";
        String sourceUrl2 = "sourceUrl2";

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setXslName("BigPicture")
                    .setUrl("bigPictureUrl")
            )
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setUrl(url1)
                    .setUrlSource(sourceUrl1)
            )
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setXslName("XL-Picture_2")
                    .setUrl(url2)
                    .setUrlSource(sourceUrl2)
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<AutoGenerationApi.Image> expectedImages = Arrays.asList(
            AutoGenerationApi.Image.newBuilder()
                .setIndex(0)
                .setImageType(AutoGenerationApi.ImageType.OTHER)
                .setExistingContentUrl(url1)
                .setSourceUrl(sourceUrl1)
                .setContentType("image/jpeg")
                .build(),
            AutoGenerationApi.Image.newBuilder()
                .setIndex(1)
                .setImageType(AutoGenerationApi.ImageType.OTHER)
                .setExistingContentUrl(url2)
                .setSourceUrl(sourceUrl2)
                .setContentType("image/jpeg")
                .build()
        );

        Assert.assertEquals(expectedImages, vendorModel.getImageList());
    }

    @Test
    public void convertModelAndSkuPictures() {
        String modelUrl = "modelUrl";
        String modelSourceUrl = "modelSourceUrl";
        String url1 = "url1";
        String sourceUrl1 = "sourceUrl1";
        String url2 = "url2";
        String sourceUrl2 = "sourceUrl2";

        ModelStorage.Model model = ModelStorage.Model.newBuilder()
            .setId(10)
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setUrl(modelUrl)
                    .setUrlSource(modelSourceUrl)
            )
            .build();

        int skuIdFirst = 1;
        int skuIdSecond = 2;

        ModelStorage.Model sku1 = ModelStorage.Model.newBuilder()
            .setId(skuIdFirst)
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setUrl(url1)
                    .setUrlSource(sourceUrl1)
            )
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setUrl(url2)
                    .setUrlSource(sourceUrl2)
            )
            .build();

        ModelStorage.Model sku2 = ModelStorage.Model.newBuilder()
            .setId(skuIdSecond)
            .addPictures(
                ModelStorage.Picture.newBuilder()
                    .setUrl(url1)
                    .setUrlSource(sourceUrl1)
            )
            .build();

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            model,
            Arrays.asList(sku1, sku2),
            categoryDataMock
        );

        List<AutoGenerationApi.Image> expectedImages = Arrays.asList(
            AutoGenerationApi.Image.newBuilder()
                .setIndex(0)
                .setImageType(AutoGenerationApi.ImageType.OTHER)
                .setExistingContentUrl(modelUrl)
                .setSourceUrl(modelSourceUrl)
                .setContentType("image/jpeg")
                .build(),
            AutoGenerationApi.Image.newBuilder()
                .setIndex(1)
                .setImageType(AutoGenerationApi.ImageType.OTHER)
                .setExistingContentUrl(url1)
                .setSourceUrl(sourceUrl1)
                .setContentType("image/jpeg")
                .addVendorSkuId(skuIdFirst)
                .addVendorSkuId(skuIdSecond)
                .build(),
            AutoGenerationApi.Image.newBuilder()
                .setIndex(2)
                .setImageType(AutoGenerationApi.ImageType.OTHER)
                .setExistingContentUrl(url2)
                .setSourceUrl(sourceUrl2)
                .addVendorSkuId(skuIdFirst)
                .setContentType("image/jpeg")
                .build()
        );

        Assert.assertEquals(expectedImages, vendorModel.getImageList());
    }

    @Test
    public void convertModelCategory() {
        long categoryId = 123;

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .setCategoryId(categoryId);

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        Assert.assertEquals(categoryId, vendorModel.getCategoryId());
    }

    @Test
    public void convertModelVendor() {
        long vendorId = 123;

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .setVendorId(vendorId);

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        Assert.assertEquals(vendorId, vendorModel.getVendorId());
    }

    @Test
    public void convertModelId() {
        long modelId = 123;

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .setId(modelId);

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        Assert.assertEquals(modelId, vendorModel.getGuruModelId());
    }

    @Test
    public void convertModelTitles() {
        String ruTitle = "russianTitle";
        String enTitle = "englishTitle";

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addTitles(
                ModelStorage.LocalizedString.newBuilder()
                    .setValue(ruTitle)
                    .setIsoCode("ru")
            )
            .addTitles(
                ModelStorage.LocalizedString.newBuilder()
                    .setValue(enTitle)
                    .setIsoCode("en")
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<AutoGenerationApi.LocalizedString> expectedTitles = Arrays.asList(
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(ruTitle)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build(),
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(enTitle)
                .setLanguage(AutoGenerationApi.Language.ENGLISH)
                .build()
        );

        Assert.assertEquals(expectedTitles, vendorModel.getTitleList());
    }

    @Test
    public void convertModelAliasesFromField() {
        String alias1 = "alias1";
        String alias2 = "alias2";

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addAliases(
                ModelStorage.LocalizedString.newBuilder()
                    .setValue(alias1)
                    .setIsoCode("ru")
            )
            .addAliases(
                ModelStorage.LocalizedString.newBuilder()
                    .setValue(alias2)
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<AutoGenerationApi.LocalizedString> expectedAliases = Arrays.asList(
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(alias1)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build(),
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(alias2)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build()
        );

        Assert.assertEquals(expectedAliases, vendorModel.getAliasList());
    }

    @Test
    public void convertModelAliasesFromParameters() {
        String alias1 = "alias1";
        String alias2 = "alias2";

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(CategoryData.ALIASES)
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(alias1)
                            .setIsoCode("ru")
                    )
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(alias2)
                    )
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<AutoGenerationApi.LocalizedString> expectedAliases = Arrays.asList(
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(alias1)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build(),
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(alias2)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build()
        );

        Assert.assertEquals(expectedAliases, vendorModel.getAliasList());
    }

    @Test
    public void convertModelVendorCodes() {
        String vendorCode1 = "vendorCode1";
        String vendorCode2 = "vendorCode2";

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(CategoryData.VENDOR_CODE)
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(vendorCode1)
                            .setIsoCode("ru")
                    )
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(vendorCode2)
                            .setIsoCode("ru")
                    )
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<AutoGenerationApi.LocalizedString> expectedVendorCodes = Arrays.asList(
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(vendorCode1)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build(),
            AutoGenerationApi.LocalizedString.newBuilder()
                .setText(vendorCode2)
                .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                .build()
        );

        Assert.assertEquals(expectedVendorCodes, vendorModel.getVendorCodeList());
    }

    @Test
    public void convertModelBarcodes() {
        String barcode1 = "111111";
        String barcode2 = "222222";

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setXslName(CategoryData.BAR_CODE)
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(barcode1)
                            .setIsoCode("ru")
                    )
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(barcode2)
                            .setIsoCode("en")
                    )
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<String> expectedBarcodes = Arrays.asList(
            barcode1,
            barcode2
        );

        Assert.assertEquals(expectedBarcodes, vendorModel.getBarcodeList());
    }

    @Test
    public void convertModelParameters() {
        boolean boolValue = true;
        int booleanOptionId = 1;
        int enumOptionId = 12;
        int numericEnumOptionId = 34;
        double numericValue = 10.5;
        String stringValue1 = "stringValue1";
        String stringValue2 = "stringValue2";

        int boolParamId = 111;
        int enumParamId = 222;
        int numericEnumParamId = 333;
        int numericParamId = 123;
        int stringParamId = 456;

        ModelStorage.Model.Builder modelBuilder = ModelStorage.Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(boolParamId)
                    .setXslName("BooleanParam")
                    .setValueType(MboParameters.ValueType.BOOLEAN)
                    .setBoolValue(boolValue)
                    .setOptionId(booleanOptionId)
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(enumParamId)
                    .setXslName("EnumParam")
                    .setValueType(MboParameters.ValueType.ENUM)
                    .setOptionId(enumOptionId)
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(numericEnumParamId)
                    .setXslName("NumericEnumParam")
                    .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
                    .setOptionId(numericEnumOptionId)
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(numericParamId)
                    .setXslName("NumericParam")
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setNumericValue(Double.toString(numericValue))
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(stringParamId)
                    .setXslName("StringParam")
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(stringValue1)
                            .setIsoCode("ru")
                    )
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(stringValue2)
                            .setIsoCode("ru")
                    )
            );

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            modelBuilder.build(),
            Collections.emptyList(),
            categoryDataMock
        );

        List<AutoGenerationApi.ParameterValue> expectedParameterValues = Arrays.asList(
            AutoGenerationApi.ParameterValue.newBuilder()
                .setParamId(boolParamId)
                .setType(AutoGenerationApi.ParameterType.BOOLEAN)
                .setBoolValue(boolValue)
                .setOptionId(booleanOptionId)
                .build(),
            AutoGenerationApi.ParameterValue.newBuilder()
                .setParamId(enumParamId)
                .setType(AutoGenerationApi.ParameterType.ENUM)
                .setOptionId(enumOptionId)
                .build(),
            AutoGenerationApi.ParameterValue.newBuilder()
                .setParamId(numericEnumParamId)
                .setType(AutoGenerationApi.ParameterType.ENUM)
                .setOptionId(numericEnumOptionId)
                .build(),
            AutoGenerationApi.ParameterValue.newBuilder()
                .setParamId(numericParamId)
                .setType(AutoGenerationApi.ParameterType.NUMERIC)
                .setNumericValue(numericValue)
                .build(),
            AutoGenerationApi.ParameterValue.newBuilder()
                .setParamId(stringParamId)
                .setType(AutoGenerationApi.ParameterType.TEXT)
                .addText(AutoGenerationApi.LocalizedString.newBuilder()
                    .setText(stringValue1)
                    .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .addText(AutoGenerationApi.LocalizedString.newBuilder()
                    .setText(stringValue2)
                    .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build()
        );

        Assert.assertEquals(expectedParameterValues, vendorModel.getParameterValueList());
    }

    @Test
    public void convertSkuValueHypotheses() {
        boolean booleanValue = true;
        int booleanOptionId = 1;
        String booleanOptionName = "booleanOptionName";
        int enumOptionId = 12;
        String enumOptionName = "enumOptionName";
        int numericEnumOptionId = 34;
        double numericEnumValue = 567;
        String numericEnumOptionName = "numericEnumOptionName";
        double numericValue = 10.5;
        String stringValue1 = "stringValue1";
        String stringValue2 = "stringValue2";

        int booleanParamId = 111;
        int enumParamId = 222;
        int numericEnumParamId = 333;
        int numericParamId = 123;
        int stringParamId = 456;

        String booleanParamXslName = "BooleanParam";
        String enumParamXslName = "EnumParam";
        String numericEnumParamXslName = "NumericEnumParam";

        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(booleanParamId)
                    .setXslName(booleanParamXslName)
                    .setValueType(MboParameters.ValueType.BOOLEAN)
                    .setBoolValue(booleanValue)
                    .setOptionId(booleanOptionId)
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(enumParamId)
                    .setXslName(enumParamXslName)
                    .setValueType(MboParameters.ValueType.ENUM)
                    .setOptionId(enumOptionId)
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(numericEnumParamId)
                    .setXslName("NumericEnumParam")
                    .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
                    .setOptionId(numericEnumOptionId)
                    .setNumericValue(Double.toString(numericEnumValue))
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(numericParamId)
                    .setXslName("NumericParam")
                    .setValueType(MboParameters.ValueType.NUMERIC)
                    .setNumericValue(Double.toString(numericValue))
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(stringParamId)
                    .setXslName("StringParam")
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(stringValue1)
                            .setIsoCode("ru")
                    )
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(stringValue2)
                            .setIsoCode("ru")
                    )
            )
            .build();

        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        Mockito.when(categoryDataMock.isSkuParameter(booleanParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuParameter(enumParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuParameter(numericEnumParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuParameter(numericParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuParameter(stringParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuExtractInSkubdParameter(booleanParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuExtractInSkubdParameter(enumParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuExtractInSkubdParameter(numericEnumParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuExtractInSkubdParameter(numericParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuExtractInSkubdParameter(stringParamId)).thenReturn(true);

        Mockito.when(categoryDataMock.getParamById(booleanParamId))
            .thenReturn(
                MboParameters.Parameter.newBuilder()
                    .setId(booleanParamId)
                    .setXslName(booleanParamXslName)
                    .addOption(
                        MboParameters.Option.newBuilder()
                            .setId(booleanOptionId)
                            .addName(
                                MboParameters.Word.newBuilder()
                                    .setName(booleanOptionName)
                                    .build()
                            )
                            .build()
                    )
                .build()
            );
        Mockito.when(categoryDataMock.getParamById(enumParamId))
            .thenReturn(
                MboParameters.Parameter.newBuilder()
                    .setId(enumParamId)
                    .setXslName(enumParamXslName)
                    .addOption(
                        MboParameters.Option.newBuilder()
                            .setId(enumOptionId)
                            .addName(
                                MboParameters.Word.newBuilder()
                                    .setName(enumOptionName)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            );
        Mockito.when(categoryDataMock.getParamById(numericEnumParamId))
            .thenReturn(
                MboParameters.Parameter.newBuilder()
                    .setId(numericEnumParamId)
                    .setXslName(numericEnumParamXslName)
                    .addOption(
                        MboParameters.Option.newBuilder()
                            .setId(numericEnumOptionId)
                            .addName(
                                MboParameters.Word.newBuilder()
                                    .setName(numericEnumOptionName)
                                    .build()
                            )
                            .build()
                    )
                    .build()
            );

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            ModelStorage.Model.getDefaultInstance(),
            Collections.singletonList(sku),
            categoryDataMock
        );

        List<AutoGenerationApi.ParameterValueHypothesis> expectedValueHypotheses = Arrays.asList(
            AutoGenerationApi.ParameterValueHypothesis.newBuilder()
                .setParamId(booleanParamId)
                .setType(AutoGenerationApi.ParameterType.BOOLEAN)
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(booleanOptionName)
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build(),
            AutoGenerationApi.ParameterValueHypothesis.newBuilder()
                .setParamId(enumParamId)
                .setType(AutoGenerationApi.ParameterType.ENUM)
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(enumOptionName)
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build(),
            AutoGenerationApi.ParameterValueHypothesis.newBuilder()
                .setParamId(numericEnumParamId)
                .setType(AutoGenerationApi.ParameterType.ENUM)
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(Double.toString(numericEnumValue))
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build(),
            AutoGenerationApi.ParameterValueHypothesis.newBuilder()
                .setParamId(numericParamId)
                .setType(AutoGenerationApi.ParameterType.NUMERIC)
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(Double.toString(numericValue))
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build(),
            AutoGenerationApi.ParameterValueHypothesis.newBuilder()
                .setParamId(stringParamId)
                .setType(AutoGenerationApi.ParameterType.TEXT)
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(stringValue1)
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(stringValue2)
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build()
        );

        AutoGenerationApi.VendorSku vendorSku = vendorModel.getVendorSku(0);
        Assert.assertEquals(expectedValueHypotheses, vendorSku.getParameterValueHypothesisList());
    }

    @Test
    public void convertSkuParameters() {
        String titleParamValue = "title";
        String infoParamValue = "info";
        String valueHypothesis = "hypothesis";

        int titleParamId = 1;
        int infoParamId = 2;
        int valueHypothesisId = 3;

        ModelStorage.Model sku = ModelStorage.Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(titleParamId)
                    .setXslName("TitleParam")
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(titleParamValue)
                            .setIsoCode("ru")
                    )
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(infoParamId)
                    .setXslName("SkuInfoParam")
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(infoParamValue)
                            .setIsoCode("ru")
                    )
            )
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(valueHypothesisId)
                    .setXslName("ExtractInSkubdParam")
                    .setValueType(MboParameters.ValueType.STRING)
                    .addStrValue(
                        ModelStorage.LocalizedString.newBuilder()
                            .setValue(valueHypothesis)
                            .setIsoCode("ru")
                    )
            )
            .build();


        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        Mockito.when(categoryDataMock.isSkuParameter(infoParamId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuParameter(valueHypothesisId)).thenReturn(true);
        Mockito.when(categoryDataMock.isSkuExtractInSkubdParameter(valueHypothesisId)).thenReturn(true);

        AutoGenerationApi.VendorModel vendorModel = ModelConvertUtils.convertToVendorModel(
            ModelStorage.Model.getDefaultInstance(),
            Collections.singletonList(sku),
            categoryDataMock
        );

        List<AutoGenerationApi.ParameterValueHypothesis> expectedValueHypotheses = Collections.singletonList(
            AutoGenerationApi.ParameterValueHypothesis.newBuilder()
                .setParamId(valueHypothesisId)
                .setType(AutoGenerationApi.ParameterType.TEXT)
                .addText(
                    AutoGenerationApi.LocalizedString.newBuilder()
                        .setText(valueHypothesis)
                        .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build()
        );

        List<AutoGenerationApi.ParameterValue> expectedParameterValues = Collections.singletonList(
            AutoGenerationApi.ParameterValue.newBuilder()
                .setParamId(infoParamId)
                .setType(AutoGenerationApi.ParameterType.TEXT)
                .addText(AutoGenerationApi.LocalizedString.newBuilder()
                    .setText(infoParamValue)
                    .setLanguage(AutoGenerationApi.Language.RUSSIAN)
                )
                .build()
        );

        AutoGenerationApi.VendorSku vendorSku = vendorModel.getVendorSku(0);
        Assert.assertEquals(expectedValueHypotheses, vendorSku.getParameterValueHypothesisList());
        Assert.assertEquals(expectedParameterValues, vendorSku.getParameterValueList());
    }

    @Test
    public void convertIsSku() {
        CategoryData categoryDataMock = Mockito.mock(CategoryData.class);

        ModelStorage.Model model1 = Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(15354452L)
                    .setTypeId(ParameterType.BOOLEAN_VALUE)
                    .setBoolValue(true)
                    .setOptionId(15354453)
                    .setXslName(CategoryData.IS_SKU)
                    .build()
            )
            .build();
        AutoGenerationApi.VendorModel vendorModel1 = ModelConvertUtils.convertToVendorModel(
            model1,
            Collections.emptyList(),
            categoryDataMock
        );
        Assert.assertTrue(vendorModel1.getIsSku());

        ModelStorage.Model model2 = Model.newBuilder()
            .addParameterValues(
                ModelStorage.ParameterValue.newBuilder()
                    .setParamId(15354452L)
                    .setTypeId(ParameterType.BOOLEAN_VALUE)
                    .setBoolValue(false)
                    .setOptionId(15354453)
                    .setXslName(CategoryData.IS_SKU)
                    .build()
            )
            .build();
        AutoGenerationApi.VendorModel vendorModel2 = ModelConvertUtils.convertToVendorModel(
            model2,
            Collections.emptyList(),
            categoryDataMock
        );
        Assert.assertFalse(vendorModel2.getIsSku());

        ModelStorage.Model model3 = Model.getDefaultInstance();
        AutoGenerationApi.VendorModel vendorModel3 = ModelConvertUtils.convertToVendorModel(
            model3,
            Collections.emptyList(),
            categoryDataMock
        );
        Assert.assertEquals(VendorModel.getDefaultInstance().getIsSku(), vendorModel3.getIsSku());

    }
}