package ru.yandex.market.psku.postprocessor.bazinga.errremove;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParserImpl;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategoryParametersServiceStub;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.CategorySizeMeasureServiceStub;
import ru.yandex.market.psku.postprocessor.bazinga.CategoryToYtTask;
import ru.yandex.market.psku.postprocessor.common.BaseDBTest;
import ru.yandex.market.psku.postprocessor.common.db.config.CommonDaoConfig;
import ru.yandex.market.psku.postprocessor.config.ManualTestConfig;
import ru.yandex.market.psku.postprocessor.dto.partnercontent.DcpCategoryConverter;
import ru.yandex.market.psku.postprocessor.dto.partnercontent.DcpCategoryPojo;

/** Для выполнения теста необходимо внести имя пользователя и токен для YT в ManualTestConfig */
@Ignore("Ручной тест")
@ContextConfiguration(classes = {ManualTestConfig.class, CommonDaoConfig.class, CategoryToYtTaskManualTest.TestConfig.class})
public class CategoryToYtTaskManualTest extends BaseDBTest {
    private final static String ENVIRONMENT = "testing";
    private final static String DIR_PATH_TEMPLATE = "//home/market/users/%s/category";
    private final static boolean LIMIT_LOADED_CATEGORIES = true;
    private final static int LIMIT = 10;

    private final static ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    @Qualifier("hahnYtApi")
    private Yt yt;

    @Autowired
    @Qualifier("yqlJdbcTemplate")
    private JdbcTemplate yqlJdbcTemplate;

    @Autowired
    private CategoryInfoProducer categoryInfoProducer;

    @Autowired
    private CategoryParametersFormParser categoryParametersFormParser;

    @Test
    public void run() {
        String dirPath = String.format(DIR_PATH_TEMPLATE, ManualTestConfig.YT_USER);
        CategoryToYtTask categoryToYtTask = new CategoryToYtTask(yt, dirPath, yqlJdbcTemplate, ENVIRONMENT, categoryInfoProducer, categoryParametersFormParser, LIMIT_LOADED_CATEGORIES, LIMIT);
        categoryToYtTask.execute(null);

        YPath tablePath = categoryToYtTask.getTablePath();
        List<DcpCategoryPojo> response = yqlJdbcTemplate.query(String.format("select CAST(response as string) as response from `%s` limit 1",
                        tablePath),
                (rs, rowNum) -> readValue(rs.getString("response")));
        DcpCategoryPojo taskResultPojo = response.get(0);

        long hid = Long.parseLong(taskResultPojo.getCategoryId());
        DcpCategoryPojo categoryPojo = DcpCategoryConverter.convert(categoryInfoProducer.extractCategoryInfo(hid,
                ImportContentType.DCP_UI), null);

        String taskResultJson = writeValueAsString(taskResultPojo);
        String categoryJson = writeValueAsString(categoryPojo);
        Assertions.assertThat(taskResultJson).isEqualTo(categoryJson);
    }

    private static String writeValueAsString(Object any) {
        try {
            return objectMapper.writeValueAsString(any);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static DcpCategoryPojo readValue(String value) {
        try {
            return objectMapper.readValue(value, DcpCategoryPojo.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class TestConfig {
        public static final String HTTP_EXPORTER_CACHED_BASE_URL = "http://mbo-http-exporter.tst.vs.market.yandex.net:8084/cached";
        public static final String HTTP_EXPORTER_BASE_URL = "http://mbo-http-exporter.tst.vs.market.yandex.net:8084";

        @Bean
        CategoryParametersService categoryParametersService() {
            CategoryParametersServiceStub categoryParametersService = new CategoryParametersServiceStub();
            categoryParametersService.setHost(HTTP_EXPORTER_CACHED_BASE_URL + "/categoryParameters/");
            categoryParametersService.setConnectionTimeoutMillis(300000);
            categoryParametersService.setTriesBeforeFail(1);

            return categoryParametersService;
        }

        @Bean
        CategorySizeMeasureService categorySizeMeasureService() {
            CategorySizeMeasureServiceStub categorySizeMeasureService = new CategorySizeMeasureServiceStub();
            categorySizeMeasureService.setHost(HTTP_EXPORTER_CACHED_BASE_URL + "/categorySizeMeasure/");
            return categorySizeMeasureService;
        }

        @Bean
        CategoryDataKnowledge categoryDataKnowledge(
                CategoryParametersService categoryParametersService,
                CategorySizeMeasureService categorySizeMeasureService
        ) {
            CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
            categoryDataKnowledge.setCategoryParametersService(categoryParametersService);
            categoryDataKnowledge.setCategoryDataRefreshersCount(1);
            categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureService);
            return categoryDataKnowledge;
        }

        @Bean
        CategoryParametersFormParser categoryParametersFormParser(
        ) {
            return new CategoryParametersFormParserImpl(HTTP_EXPORTER_BASE_URL);
        }

        @Bean
        CategoryInfoProducer categoryInfoProducer(
                CategoryDataKnowledge categoryDataKnowledge,
                CategoryParametersFormParser categoryParametersFormParser
        ) {
            return new CategoryInfoProducer(
                    categoryDataKnowledge, categoryParametersFormParser
            );
        }
    }
}
