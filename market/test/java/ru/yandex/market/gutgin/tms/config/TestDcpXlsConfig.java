package ru.yandex.market.gutgin.tms.config;

import com.google.protobuf.BytesValue;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation.DatacampOffersValidation;
import ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation.OfferContentStateValidation;
import ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation.Validation;
import ru.yandex.market.gutgin.tms.service.GlobalVendorsCachingService;
import ru.yandex.market.gutgin.tms.service.goodcontent.ParamValueHelper;
import ru.yandex.market.gutgin.tms.utils.goodcontent.GoodParameterCreator;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.ParameterInfoBuilder;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;

@Configuration
public class TestDcpXlsConfig {
    public static final long PARAM_ID = 1;
    public static final long PARAM_ID_2 = 2;
    public static final String PARAM_NAME = "param1";
    public static final String PARAM_NAME_2 = "param2";

    @Bean
    DatacampOffersValidation datacampOffersValidation() {
        DatacampOffersValidation mock = Mockito.mock(DatacampOffersValidation.class);
        Mockito.when(mock.validate(Mockito.any())).thenAnswer(invocation -> Validation.Result.newBuilder().build());
        return mock;
    }

    @Bean
    CategoryDataKnowledge categoryDataKnowledge() {
        CategoryParametersService mock = Mockito.mock(CategoryParametersService.class);
        Mockito.when(mock.getParameters(Mockito.any()))
            .thenReturn(MboParameters.GetCategoryParametersResponse.getDefaultInstance());
        Mockito.when(mock.getParametersBytes(Mockito.any()))
                .thenReturn(BytesValue.newBuilder().build());
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
        categoryDataKnowledge.setCategoryParametersService(mock);
        categoryDataKnowledge.setCategoryDataRefreshersCount(1);
        return categoryDataKnowledge;
    }

    @Bean
    OfferContentStateValidation offerContentStateValidation() {
        OfferContentStateValidation mock = Mockito.mock(OfferContentStateValidation.class);
        Mockito.when(mock.validate(Mockito.any())).thenAnswer(invocation -> Validation.Result.newBuilder().build());
        return mock;
    }

    @Bean
    CategoryInfoProducer categoryInfoProducer() {
        CategoryInfoProducer categoryInfoProducer = Mockito.mock(CategoryInfoProducer.class);
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
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));
        return categoryInfoProducer;
    }

    @Bean
    GlobalVendorsCachingService globalVendorsCachingService() {
        GlobalVendorsCachingService globalVendorsCachingService = Mockito.mock(GlobalVendorsCachingService.class);
        Mockito.when(globalVendorsCachingService.getVendor(anyLong()))
            .thenReturn(Optional.of(MboVendors.GlobalVendor.newBuilder().setIsRequireGtinBarcodes(true).build()));
        return globalVendorsCachingService;
    }

    @Bean
    ParamValueHelper paramValueHelper() {
        ParamValueHelper paramValueHelper = Mockito.mock(ParamValueHelper.class);
        Mockito.when(paramValueHelper.extractVendor(anyLong(), anyList()))
            .thenReturn(GoodParameterCreator.ExtractParameterResult.value(
                ModelStorage.ParameterValue.newBuilder().setOptionId(12).build()
            ));
        return paramValueHelper;
    }
}
