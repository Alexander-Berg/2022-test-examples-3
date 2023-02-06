package ru.yandex.market.partner.content.common;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessDataBucketData;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileData;
import ru.yandex.market.robot.db.ParameterValueComposer;
import ru.yandex.market.test.util.random.RandomBean;

public abstract class BaseDBStateGenerator extends BaseDbCommonTest {

    public static final int PARTNER_SHOP_ID = 123;
    protected static final long SEED = 1234567L;
    public static final long CATEGORY_ID = 1234L;
    protected static final int SOURCE_ID = 4321;
    protected static final long DESCRIPTION_ID = 15341921L;
    protected static final String DESCRIPTION_NAME = "description";
    public static final int FORMILIZED_ID = 452450909;

    public static final MboParameters.Category CATEGORY = buildCategory(CATEGORY_ID);

    protected EnhancedRandom generator;

    protected long requestId;
    protected long processId;
    protected ProcessFileData processFileData;

    protected long dataBucketId;
    protected ProcessDataBucketData processDataBucketData;

    @Before
    public void setUp() {
        generator = RandomBean.defaultRandom();
        generator.setSeed(SEED);

        createSource(SOURCE_ID, PARTNER_SHOP_ID);
    }

    public static MboParameters.Category buildCategory(long categoryId) {
        return MboParameters.Category.newBuilder()
                .setHid(categoryId)
                .addName(MboParameters.Word.newBuilder().setName("Category " + CATEGORY_ID).setLangId(225))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_ID).setXslName(ParameterValueComposer.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .addName(MboParameters.Word.newBuilder().setLangId(225).setName("производитель"))
                        .addOption(MboParameters.Option.newBuilder().addName(
                                MboParameters.Word.newBuilder().setLangId(225).setName("производитель-1"))
                                .setId(1000)
                                .build())
                        .setMandatoryForPartner(true)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.NAME_ID).setXslName(ParameterValueComposer.NAME)
                        .setValueType(MboParameters.ValueType.STRING)
                        .setMandatoryForPartner(true)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.BARCODE_ID).setXslName(ParameterValueComposer.BARCODE)
                        .setValueType(MboParameters.ValueType.STRING)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_CODE_ID).setXslName(ParameterValueComposer.VENDOR_CODE)
                        .setValueType(MboParameters.ValueType.STRING)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.ALIASES_ID).setXslName(ParameterValueComposer.ALIASES)
                        .setValueType(MboParameters.ValueType.STRING))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(DESCRIPTION_ID)
                        .setValueType(MboParameters.ValueType.STRING)
                        .setXslName(DESCRIPTION_NAME)
                        .setMandatoryForPartner(true)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(FORMILIZED_ID)
                        .setValueType(MboParameters.ValueType.NUMERIC)
                        .setXslName("FORMILIZED_ID")
                        .setMandatoryForPartner(false))
                .setTitleAvgWordsAmount(2)
                .setTitleAvgCharsAmount(20)
                .build();
    }
}
