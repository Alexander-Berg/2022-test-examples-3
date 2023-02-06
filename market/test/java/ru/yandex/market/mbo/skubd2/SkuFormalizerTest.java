package ru.yandex.market.mbo.skubd2;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.ir.formalize.AllowedOptionChecker;
import ru.yandex.market.ir.http.Offer;
import ru.yandex.market.mbo.skubd2.formalize.Formalizer;
import ru.yandex.market.mbo.skubd2.load.dao.ParameterValue;
import ru.yandex.market.mbo.skubd2.load.dao.ValueAliasEntity;
import ru.yandex.market.mbo.skubd2.service.dao.AllowedOptionMap;
import ru.yandex.market.mbo.skubd2.service.dao.OfferInfo;
import ru.yandex.market.mbo.skubd2.service.dao.ValueAliasMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Shamil Ablyazov, <a href="mailto:a-shar@yandex-team.ru"/>.
 */
public class SkuFormalizerTest {
    private static final long COLOR_VENDOR_ID = 14871214;
    private static final long OPTION_RED = 14898056;
    private static final long OPTION_GREEN = 14898020;
    private static final long OPTION_GREY = 14896295;
    private static final long OPTION_DARK_GREY = 14897380;
    private static final long OPTION_KARAPUZ_WITH_MORPH = 1489738011;
    private static final long OPTION_KARAPUZ_TRULY = 1489738012;

    private static final AllowedOptionChecker DUMMY_CHECKER = (paramId, optionId) -> true;

    private Formalizer formalizer;

    @Before
    public void setUp() throws IOException {
        String categoryFileName = getClass().getResource("/proto_json/parameters_91491.json").getFile();
        String modelFileName = getClass().getResource("/proto_json/sku_91491.json").getFile();
//        formalizer = CategoryEntityUtils.buildFormalizer(categoryFileName);
        formalizer = CategoryEntityUtils.buildEmbeddedFormalizer(categoryFileName, modelFileName);
    }

    @Test
    public void testAllowedOptions() throws InterruptedException {
        List<Offer.YmlParam> ymlParams = new ArrayList<>();
        Offer.YmlParam.Builder ymlBuilder = Offer.YmlParam.newBuilder();
        ymlParams.add(ymlBuilder.setName("blabla").setValue("темно-серый").build());
        ymlBuilder.clear();
        ymlParams.add(ymlBuilder.setName("Цвет").setValue("никакой").build());
        ymlBuilder.clear();
        ymlParams.add(ymlBuilder.setName("Цвет товара").setValue("красный").build());

        OfferInfo offerInfo = new OfferInfo(0L, "Xiaomi Redmi 4A 2GB + 16GB темно-серый", ymlParams);
        List<ParameterValue> emptyFormalize = formalizer.formalize(
            offerInfo, new AllowedOptionMap(Collections.emptyList(), null), null);

        Assert.assertEquals(0, emptyFormalize.size());

        ImmutableList<ParameterValue> parameterValues = ImmutableList.of(
            ParameterValue.newEnumValue(COLOR_VENDOR_ID, OPTION_RED)
        );
        List<ParameterValue> formalize = formalizer.formalize(offerInfo,
            new AllowedOptionMap(parameterValues, null),
            null
        );

        Assert.assertEquals(1, formalize.size());
        assertParams(formalize, new long[]{COLOR_VENDOR_ID}, new long[]{OPTION_RED});
    }

    @Test
    public void testAllParams() throws InterruptedException {
        List<Offer.YmlParam> ymlParams = new ArrayList<>();
        Offer.YmlParam.Builder ymlBuilder = Offer.YmlParam.newBuilder();
        ymlParams.add(ymlBuilder.setName("цвет").setValue("зеленый").build());

        OfferInfo offerInfo = new OfferInfo(14206637L, "Xiaomi Redmi темно-серый", ymlParams);

        List<ParameterValue> formalize = formalizer.formalize(offerInfo, DUMMY_CHECKER, null);

        long[] paramIds = new long[formalize.size()];
        Arrays.fill(paramIds, COLOR_VENDOR_ID);
        assertParams(formalize, paramIds, new long[]{OPTION_GREEN});
    }

    @Test
    public void testMorphology() throws InterruptedException {
        List<ParameterValue> formalize = formalize(
            new OfferInfo(0L, "Xiaomi Redmi карапузиковый", Collections.emptyList())
        );

        long[] paramIds = new long[formalize.size()];
        Arrays.fill(paramIds, COLOR_VENDOR_ID);
        assertParams(formalize, paramIds, new long[]{OPTION_KARAPUZ_TRULY});

        List<ParameterValue> formalizeMorphology = formalize(
            new OfferInfo(0L, "Xiaomi Redmi карапузиковым", Collections.emptyList())
        );

        long[] paramIdsMorphology = new long[formalizeMorphology.size()];
        Arrays.fill(paramIdsMorphology, COLOR_VENDOR_ID);
        assertParams(formalizeMorphology, paramIdsMorphology, new long[]{OPTION_KARAPUZ_WITH_MORPH});
    }

    @Test
    public void testValueAlias() throws InterruptedException {
        ValueAliasMap valueAliasMap = new ValueAliasMap(
            ImmutableList.of(new ValueAliasEntity(COLOR_VENDOR_ID, OPTION_GREY, OPTION_DARK_GREY))
        );

        List<ParameterValue> formalize = formalizer.formalize(
            new OfferInfo(0L, "Xiaomi Redmi темно-серый", Collections.emptyList()),
            DUMMY_CHECKER, valueAliasMap
        );

        long[] paramIds = new long[formalize.size()];
        Arrays.fill(paramIds, COLOR_VENDOR_ID);
        assertParams(formalize, paramIds, new long[]{OPTION_GREY});
    }


    private List<ParameterValue> formalize(OfferInfo offerInfo) throws InterruptedException {
        return formalizer.formalize(offerInfo, DUMMY_CHECKER, null);
    }

    private void assertParams(List<ParameterValue> formalize, long[] paramIds, long[] optionIds) {
        int size = formalize.size();
        Assert.assertEquals(size, paramIds.length);
        Assert.assertEquals(size, optionIds.length);

        for (int i = 0; i < formalize.size(); i++) {
            ParameterValue extractedValue = formalize.get(i);
            Assert.assertEquals(extractedValue.getParameterId(), paramIds[i]);
            Assert.assertEquals(extractedValue.getOptionId(), optionIds[i]);
        }
    }
}
