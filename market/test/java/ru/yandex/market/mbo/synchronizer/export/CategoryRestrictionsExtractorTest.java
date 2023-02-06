package ru.yandex.market.mbo.synchronizer.export;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.Restrictions;
import ru.yandex.market.mbo.db.CategoryRestrictionService;
import ru.yandex.market.mbo.gwt.models.CategoryRestriction;
import ru.yandex.market.mbo.gwt.models.RestrictionMarketType;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.Restrictions.Category;
import static ru.yandex.market.mbo.Restrictions.Region;
import static ru.yandex.market.mbo.Restrictions.RegionalRestrictions;
import static ru.yandex.market.mbo.Restrictions.Restriction;
import static ru.yandex.market.mbo.Restrictions.RestrictionsData;
import static ru.yandex.market.mbo.Restrictions.Warning;
import static ru.yandex.market.mbo.synchronizer.export.CategoryRestrictionsExtractor.ProtobufConverter;

/**
 * @author ayratgdl
 * @date 26.11.15
 */
@SuppressWarnings("checkstyle:magicNumber")
public class CategoryRestrictionsExtractorTest {
    private static final String DIR = "";

    private CategoryRestrictionsExtractor extractor = null;
    private Path path = null;

    private RestrictionsData protoExample = null;
    private List<CategoryRestriction> modelExample = null;

    @Before
    public void setUp() {
        ExportRegistry registry = new ExportRegistry();
        registry.afterPropertiesSet();

        CategoryRestrictionService emptyService = mock(CategoryRestrictionService.class);
        when(emptyService.getAll()).thenReturn(new ArrayList<>());

        extractor = new CategoryRestrictionsExtractor();
        extractor.setRegistry(registry);
        extractor.setOutputFileName("category-restrictions.pb");
        extractor.setRestrictionService(emptyService);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        extractor.setExtractorWriterService(extractorWriterService);

        path = extractor.getOutputFile(DIR).toPath();

        protoExample = RestrictionsData.newBuilder()
                .addRestriction(
                        Restriction.newBuilder()
                                .addCategory(Category.newBuilder()
                                        .setId(1)
                                        .setIncludeSubtree(true))
                                .setName("rest1")
                                .addMarketType(Restrictions.RestrictionMarketType.MARKET_TYPE_BLUE)
                                .addRegionalRestriction(RegionalRestrictions.newBuilder()
                                                .addRegion(Region.newBuilder()
                                                        .setId(225)
                                                        .setIncludeSubtree(true))
                                                .addWarning(Warning.newBuilder()
                                                        .setId(1)
                                                        .setAgeStr("18")
                                                        .setText("warning")
                                                        .setShortText("shortText")
                                                        .setName("name1")
                                                        .setDefaultWarning(false)
                                                )
                                                .setShowContent(true)
                                                .setDisplayOnlyMatchedOffers(true)
                                                .setDelivery(true)
                                                .setBanned(true)
                                                .setOnBlue(true)
                                                .setOnWhite(false)
                                                .setDefaultClassificationHid(1L)
                                )
                )
                .addRestriction(
                        Restriction.newBuilder()
                                .addCategory(Category.newBuilder()
                                        .setId(2)
                                        .setIncludeSubtree(true))
                                .setName("rest2")
                                .addRegionalRestriction(RegionalRestrictions.newBuilder()
                                                .setShowContent(false)
                                                .setDisplayOnlyMatchedOffers(false)
                                                .setDelivery(true)
                                                .setBanned(false)
                                                .setOnBlue(true)
                                                .setOnWhite(true)
                                )
                )
                .build();

        modelExample = new ArrayList<>();
        modelExample.add(new CategoryRestriction().addCategoryId(1)
                        .setRestrictionName("rest1")
                        .addRegionId(225)
                        .addWarning(new CategoryRestriction.Warning()
                                .setId(1L).setAge("18").setText("warning").setShortText("shortText")
                                .setName("name1").setDefaultWarning(false))
                        .setShowContent(true)
                        .setDisplayOnlyMatchedOffers(true)
                        .setDelivery(true)
                        .setMarketTypes(Collections.singletonList(RestrictionMarketType.MARKET_TYPE_BLUE))
                        .setHideAllContent(true)
                        .setDefaultClassificationHid(1L)
        );
        modelExample.add(new CategoryRestriction()
                        .addCategoryId(2)
                        .setRestrictionName("rest2")
                        .setHideAllContent(false)
        );
    }

    @After
    public void tearDown() throws IOException {
        if (path != null) {
            Path realFile = ExporterUtils.getRealFile(path);
            if (Files.exists(realFile)) {
                Files.delete(realFile);
            }
        }
    }

    @Test
    public void testPerformFileCreated() throws Exception {
        extractor.perform(DIR);
        assertTrue("File of extract is created", Files.exists(ExporterUtils.getRealFile(path)));
    }

    @Test
    public void testPerformEmptyExtract() throws Exception {
        extractor.perform(DIR);
        try (InputStream in = ExporterUtils.getInputStream(path.toFile())) {
            RestrictionsData restrictions = RestrictionsData.parseFrom(in);
            assertTrue("Empty list restrictions", restrictions.getRestrictionList().isEmpty());
        }
    }

    @Test
    public void testProtobufConverterToProtobuf() {
        RestrictionsData protResult = ProtobufConverter.toProtobuf(modelExample);
        assertEquals(protoExample, protResult);
    }
}
