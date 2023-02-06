package ru.yandex.market.mbo.modelstorage.util;

import com.google.common.collect.ImmutableMap;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.kdepot.api.Entities;
import ru.yandex.market.mbo.core.kdepot.api.Entity;
import ru.yandex.market.mbo.core.modelstorage.util.Entity2CommonModelConverter;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.gwt.models.rules.ParametersBuilder;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author york
 * @since 18.05.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class Entity2CommonModelConverterTest {

    @Test
    public void testConversion() {
        CommonModel model = CommonModelBuilder.newBuilder().id(555)
            .startParameterValue()
                .paramId(KnownIds.VENDOR_PARAM_ID)
                .xslName(XslNames.VENDOR)
                .optionId(10L)
                .type(Param.Type.ENUM)
            .endParameterValue()
            .startParameterValue()
                .paramId(1L)
                .xslName("someenum")
                .optionId(15L)
                .type(Param.Type.ENUM)
            .endParameterValue()
            .published(true)
            .createdDate(new Date())
            .getModel();
        model.setBluePublished(true);

        Map<String, CategoryParam> parameters = new HashMap<>();

        ParametersBuilder.<Void>startParameters(params -> {
            params.forEach(cp -> parameters.put(cp.getXslName(), cp));
            return null;
        })
            .startParameter()
                .name("someenum")
                .xsl("someenum")
                .type(Param.Type.ENUM)
                .option(15L, "OPTION")
                .option(10L, "OPTION2")
            .endParameter()
        .endParameters();

        Map<Long, Long> global2LocalVendorsMap = ImmutableMap.<Long, Long>builder().put(10L, 100L).build();

        Entity entity = Entity2CommonModelConverter.convert(model, parameters, global2LocalVendorsMap);
        Assert.assertEquals("true",
            Entities.getFirstAttrValue(entity, XslNames.BLUE_PUBLISHED, "unknown"));

        Assert.assertEquals("2",
            Entities.getFirstAttrValue(entity, XslNames.PUBLISH_LEVEL, "unknown"));

        Assert.assertEquals("OPTION",
            Entities.getFirstAttrValue(entity, "someenum", "unknown"));

        Assert.assertEquals("100",
            Entities.getFirstAttrValue(entity, XslNames.VENDOR, "unknown"));

    }
}
