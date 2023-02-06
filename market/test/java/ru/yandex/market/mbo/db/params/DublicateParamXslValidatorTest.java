package ru.yandex.market.mbo.db.params;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.processing.ProcessingError;
import ru.yandex.market.mbo.common.processing.ProcessingResult;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.params.validators.DublicateParamXslValidator;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.TovarCategory;

import java.util.List;

/**
 * @author york
 * @since 07.06.2018
 */
@SuppressWarnings("checkstyle:magicNumber")
public class DublicateParamXslValidatorTest {

    private DublicateParamXslValidator dublicateParamXslValidator;

    @Before
    public void init() {
        dublicateParamXslValidator = new DublicateParamXslValidator();
        ParameterLoaderServiceStub parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryParam(createParam(1L, "xsl", Param.Type.ENUM));
        parameterLoaderService.addCategoryParam(createParam(1L, "xsl1", Param.Type.NUMERIC));
        parameterLoaderService.addCategoryParam(createParam(1L, "xsl2", Param.Type.NUMERIC));

        parameterLoaderService.addCategoryParam(createParam(2L, "xsl3", Param.Type.NUMERIC));
        parameterLoaderService.addCategoryParam(createParam(2L, "xsl1", Param.Type.STRING));
        parameterLoaderService.addCategoryParam(createParam(2L, "xsl", Param.Type.STRING));

        parameterLoaderService.addCategoryParam(createParam(3L, "xsl54", Param.Type.NUMERIC));
        dublicateParamXslValidator.setParameterLoaderService(parameterLoaderService);
    }

    @Test
    public void testValidator() {
        Assert.assertEquals(0, dublicateParamXslValidator.tovarCategoryCreated(create(1L), create(3L)).size());
        Assert.assertEquals(0, dublicateParamXslValidator.tovarCategoryDeleted(create(1L), create(3L)).size());
        Assert.assertEquals(0, dublicateParamXslValidator.tovarCategoryRestored(create(1L), create(3L)).size());
        Assert.assertEquals(0, dublicateParamXslValidator.tovarCategoryUpdated(create(1L), create(3L)).size());

        Assert.assertEquals(0, dublicateParamXslValidator.tovarCategoryMoved(
            create(2L), create(10L), create(3L)).size());

        List<ProcessingResult> resultList = dublicateParamXslValidator.tovarCategoryMoved(
            create(2L), create(3L), create(1L));

        Assert.assertEquals(2, resultList.size());
        for (ProcessingResult processingResult : resultList) {
            Assert.assertTrue(processingResult instanceof ProcessingError);
        }
    }

    private TovarCategory create(Long hid) {
        TovarCategory result = new TovarCategory();
        result.setHid(hid);
        result.setName(String.valueOf(hid));
        return result;
    }

    private Parameter createParam(Long hid, String xslName, Param.Type type) {
        Parameter parameter = new Parameter();
        parameter.setCategoryHid(hid);
        parameter.setXslName(xslName);
        parameter.setType(type);
        return parameter;
    }
}
