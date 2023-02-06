package ru.yandex.market.gutgin.tms.onetime;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.matching.CategoryModelMatcher;
import ru.yandex.market.gutgin.tms.matching.PartnerModelsMatcher;
import ru.yandex.market.gutgin.tms.matching.VendorModelMatcher;
import ru.yandex.market.gutgin.tms.matching.alias.AliasAndParametersMatcher;
import ru.yandex.market.gutgin.tms.matching.alias.AliasMatchersBuilder;
import ru.yandex.market.gutgin.tms.matching.alias.FindFirstByAliasMatcher;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelExporterHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration_api.http.service.ModelStorageServiceMock;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.partner.content.common.entity.Model;

import static org.mockito.Mockito.mock;

/**
 Класс для дебага матчинга в беттере.
 */
@Ignore
public class PartnerModelsMatcherTest {
//    public static final String categorySizeMeasureServiceHost = "http://mbo-http-exporter.yandex.net:8084/categorySizeMeasure/";
//    public static final String categoryParametersServiceHost = "http://mbo-http-exporter.yandex.net:8084/categoryParameters/";
//    public static final String httpExporterProtoUrl = "http://mbo-http-exporter.yandex.net:8084/category-models-proto";
//    private static final String modelStorageUrl = "http://mbo-card-api.http.yandex.net:33714/modelStorage/";

    public static final String categorySizeMeasureServiceHost = "";
    public static final String categoryParametersServiceHost = "";
    public static final String httpExporterProtoUrl = "";
    private static final String modelStorageUrl = "";

    private PartnerModelsMatcher partnerModelsMatcher;


    private static final String MODEL = "{\"name\": \"Браслет из золота 551070452\", \"@class\": \"ru.yandex.market.partner.content.common.entity.Model\", \"aliases\": [], \"skuList\": [{\"fake\": false, \"@class\": \"ru.yandex.market.partner.content.common.entity.Sku\", \"shopSku\": \"000011803\", \"rowIndex\": 137, \"imageUrlList\": [\"https://pmdn.sokolov.io/pics/75/6A/99DFA7AD1316B444D29A0624FCA6.jpg\"], \"parameterList\": [{\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"NUMERIC\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 14430692, \"numericValue\": 19.0, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"NUMERIC\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 6284234, \"numericValue\": 3.08, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"STRING\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 15728430, \"stringValue\": \"Браслет из золота\", \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"STRING\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 7351757, \"stringValue\": \"551070452-19\", \"serialVersionUID\": 1}], \"serialVersionUID\": 1}], \"sourceId\": 0, \"categoryId\": 91277, \"imageUrlList\": [], \"parameterList\": [{\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 6284235, \"optionId\": 6284241, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 7893318, \"optionId\": 12734125, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 13859374, \"optionId\": 13859382, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 14426107, \"optionId\": 14426112, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 16754062, \"optionId\": 16754063, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 16754062, \"optionId\": 16754064, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"ENUM\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 16797162, \"optionId\": 16797164, \"serialVersionUID\": 1}, {\"type\": [\"ru.yandex.market.partner.content.common.entity.ParameterType\", \"BOOL\"], \"@class\": \"ru.yandex.market.partner.content.common.entity.ParameterValue\", \"paramId\": 16109688, \"optionId\": 16109689, \"boolValue\": true, \"serialVersionUID\": 1}], \"serialVersionUID\": 1}";

    @Before
    public void setUp() {
        ModelStorageServiceStub modelStorageServiceStub = new ModelStorageServiceStub();
        modelStorageServiceStub.setHost(modelStorageUrl);
        modelStorageServiceStub.setUserAgent("junit a-shar@");

        ModelStorageServiceMock modelStorageServiceMock = new ModelStorageServiceMock();

        ModelStorageHelper storageHelper = new ModelStorageHelper(modelStorageServiceStub, modelStorageServiceMock);

        CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
//            @Value("${mbo.http-exporter.url}/categoryParameters/") String categoryParametersServiceHost
        categoryParametersService.setHost(categoryParametersServiceHost);

        CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();
//            @Value("${mbo.http-exporter.url}/categorySizeMeasure/") String categorySizeMeasureServiceHost
        categorySizeMeasureService.setHost(categorySizeMeasureServiceHost);

        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
        categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
        categoryDataKnowledge.setCategoryDataRefreshersCount(1);
        categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
        categoryDataKnowledge.afterPropertiesSet();

        BookCategoryHelper bookCategoryHelper = mock(BookCategoryHelper.class);
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledge, bookCategoryHelper);

        ModelExporterHelper modelExporterHelper = new ModelExporterHelper(httpExporterProtoUrl);

        partnerModelsMatcher = new PartnerModelsMatcher(CategoryModelMatcher.buildInOrder(
            VendorModelMatcher.builder(),
            AliasMatchersBuilder.of(storageHelper, this::selectMatcherForCategory, categoryDataHelper)
        ), modelExporterHelper, 3600000);

        partnerModelsMatcher.init();

    }

    @Test
    public void getMatchedModel() throws IOException, ExecutionException {
        ObjectMapper mapper = createDefaultMapper();
        Model model = mapper.readValue(MODEL.getBytes(), Model.class);
        Optional<Long> matchedModel = partnerModelsMatcher.getMatchedModel(model);
        System.out.println(matchedModel);
    }

    private static ObjectMapper createDefaultMapper() {
        return new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }



    private CategoryModelMatcher selectMatcherForCategory(long categoryId,
                                                          AliasAndParametersMatcher matcherByParams,
                                                          FindFirstByAliasMatcher matcherByAlias,
                                                          CategoryDataHelper categoryDataHelper) {
        if (categoryDataHelper.getCategoryData(categoryId).isAllowSamePartnerModelNames()) {
            return matcherByParams;
        } else {
            return matcherByAlias;
        }
    }

}
