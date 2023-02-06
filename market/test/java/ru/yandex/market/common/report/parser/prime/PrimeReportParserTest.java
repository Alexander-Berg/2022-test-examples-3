package ru.yandex.market.common.report.parser.prime;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.springframework.core.convert.converter.Converter;
import org.springframework.test.context.ContextConfiguration;
import ru.yandex.market.common.report.model.json.common.Filter;
import ru.yandex.market.common.report.model.json.common.Model;
import ru.yandex.market.common.report.model.json.common.Vendor;
import ru.yandex.market.common.report.model.json.prime.PrimeSearchResult;
import ru.yandex.market.common.report.model.json.prime.Search;
import ru.yandex.market.common.report.parser.json.PrimeSearchResultParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Created by belmatter on 14.06.17.
 */
@RunWith(Parameterized.class)
@ContextConfiguration("classpath:common-market/common-market.xml")
public class PrimeReportParserTest {

    @Parameterized.Parameter(0)
    public String path;

    @Parameterized.Parameter(1)
    public Converter converter;

    @Parameterized.Parameter(2)
    public Matcher matcher;

    private List result;

    @Parameterized.Parameters
    public static List<Object[]> testData(){
        return Arrays.asList(
                new Object[]{"/files/primeReportIphone.json", new OffersConverter(), hasSize(6)},
                new Object[]{"/files/primeReportIphone.json", new ModelsConverter(), hasSize(4)},
                new Object[]{"/files/primeReportIphone.json", new FilterConverter(), hasSize(11)}
        );
    }

    @Before
    public void setUp() throws IOException {
        PrimeSearchResultParser<List<TestOffer>> parser = new PrimeSearchResultParser<>(converter);
        result = parser.parse(PrimeReportParserTest.class.getResourceAsStream(path));
    }

    @Test
    public void testParsing() {
        assertThat(result, matcher);
    }


    public static class OffersConverter implements Converter<PrimeSearchResult, List<TestOffer>> {

        @Override
        public List<TestOffer> convert(PrimeSearchResult source) {
            return source.getSearch().orElse(new Search()).getResults().stream()
                    .filter(x -> x.getEntity().orElse("").equals("offer"))
                    .map(x -> {
                        TestOffer testOffer = new TestOffer();
                        testOffer.setModelId(x.getModel().orElse(new Model()).getId().orElse(-1L));
                        String wareId = x.getWareId()
                                .orElseThrow(() -> new IllegalStateException(
                                        "Got invalid offer from report, cannot work with it"));
                        testOffer.setWareId(wareId);
                        testOffer.setVendorName(x.getVendor().orElse(new Vendor()).getName().orElse(null));
                        return testOffer;
                    }).collect(Collectors.toList());
        }
    }

    public static class ModelsConverter implements Converter<PrimeSearchResult, List<TestModel>> {

        @Override
        public List<TestModel> convert(PrimeSearchResult source) {
            return source.getSearch().orElse(new Search()).getResults().stream()
                    .filter(x -> x.getEntity().orElse("").equals("product"))
                    .map(x -> {
                        TestModel testModel = new TestModel();
                        testModel.setModelName(x.getTitles().get().getRaw().get());
                        testModel.setCategoryName(x.getCategories().stream().findFirst().get().getName().get());
                        testModel.setCategoryType(x.getCategories().stream().findFirst().get().getType().get());
                        return testModel;
                    }).collect(Collectors.toList());
        }
    }

    public static class FilterConverter implements Converter<PrimeSearchResult, List<Filter>>{

        @Override
        public List<Filter> convert(PrimeSearchResult source) {
            return source.getFilters();
        }
    }

    public static class TestOffer {
        private String vendorName;
        private Long modelId;
        private String wareId;

        public String getVendorName() {
            return vendorName;
        }

        void setVendorName(String vendorName) {
            this.vendorName = vendorName;
        }

        public Long getModelId() {
            return modelId;
        }

        void setModelId(Long modelId) {
            this.modelId = modelId;
        }

        public String getWareId() {
            return wareId;
        }

        void setWareId(String wareId) {
            this.wareId = wareId;
        }
    }

    public static class TestModel{
        private String modelName;

        private String categoryName;

        private String categoryType;

        public String getCategoryName() {
            return categoryName;
        }

        public void setCategoryName(String categoryName) {
            this.categoryName = categoryName;
        }

        public String getCategoryType() {
            return categoryType;
        }

        public void setCategoryType(String categoryType) {
            this.categoryType = categoryType;
        }

        public String getModelName() {

            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }
    }

}
