package ru.yandex.market.mbo.db.utils;

import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.InheritedParameter;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author danfertev
 * @since 05.10.2018
 */
public class ParameterTestHelper {
    public static final long CATEGORY_ID1 = 100L;
    public static final long CATEGORY_ID2 = 200L;
    public static final long CATEGORY_ID3 = 300L;
    public static final String ENUM1 = "enum1";
    public static final String STRING1 = "string1";
    public static final String NUMERIC1 = "numeric1";
    public static final String NUMERIC_ENUM1 = "numeric_enum1";
    public static final String BOOLEAN1 = "boolean1";

    public static final AtomicLong ID_GENERATOR = new AtomicLong(0L);

    private ParameterTestHelper() {
    }

    public static CategoryParam getNameParam(long categoryId) {
        return CategoryParamBuilder
            .newBuilder(KnownIds.NAME_PARAM_ID, XslNames.NAME, Param.Type.STRING)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam getVendorParam(long categoryId) {
        return CategoryParamBuilder
            .newBuilder(KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR, Param.Type.STRING)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static InheritedParameter getInheritedVendorParam(long categoryId) {
        Parameter parent = CategoryParamBuilder
            .newBuilder(KnownIds.VENDOR_PARAM_ID, XslNames.VENDOR, Param.Type.STRING)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
        InheritedParameter inheritedParameter = new InheritedParameter(parent);
        inheritedParameter.setCategoryHid(categoryId);
        return inheritedParameter;
    }

    public static CategoryParam getEnumParam(long categoryId) {
        long id = ID_GENERATOR.incrementAndGet();
        return CategoryParamBuilder
            .newBuilder(id, "enum" + id, Param.Type.ENUM)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam getEnumParam(long categoryId, String xslName) {
        return CategoryParamBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), xslName, Param.Type.ENUM)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam getStringParam(long categoryId, String xslName) {
        return CategoryParamBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), xslName, Param.Type.STRING)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam getNumericParam(long categoryId, String xslName) {
        return CategoryParamBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), xslName, Param.Type.NUMERIC)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam getNumericEnumParam(long categoryId, String xslName) {
        return CategoryParamBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), xslName, Param.Type.NUMERIC_ENUM)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam getBooleanParam(long categoryId, String xslName) {
        return CategoryParamBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), xslName, Param.Type.BOOLEAN)
            .setLevel(CategoryParam.Level.MODEL)
            .setCategoryHid(categoryId)
            .setPublished(true)
            .setUseForGuru(true)
            .build();
    }

    public static CategoryParam copyParam(long categoryId, CategoryParam param) {
        return CategoryParamBuilder
            .newBuilder(ID_GENERATOR.incrementAndGet(), param.getXslName(), param.getType())
            .setLevel(param.getLevel())
            .setCategoryHid(categoryId)
            .setPublished(param.isPublished())
            .build();
    }

    public static Option getOption(String name) {
        return OptionBuilder.newBuilder(ID_GENERATOR.incrementAndGet()).addName(name).build();
    }

    public static Option getOption(String name, long paramId) {
        return OptionBuilder.newBuilder(ID_GENERATOR.incrementAndGet())
            .addName(name)
            .setParamId(paramId)
            .build();
    }

    public static GlobalVendor getGlobalVendor(long globalVendorId) {
        GlobalVendor globalVendor = new GlobalVendor();
        globalVendor.setId(globalVendorId);
        globalVendor.addName(WordUtil.defaultWord(String.valueOf(globalVendorId)));
        globalVendor.setPublished(true);
        return globalVendor;
    }

    public static OptionImpl getLocalVendor(CategoryParam vendorParam, GlobalVendor globalVendor) {
        long globalVendorId = globalVendor.getId();
        long localVendorId = ID_GENERATOR.incrementAndGet();
        Option option = new OptionImpl(Option.OptionType.VENDOR);
        option.addName(WordUtil.defaultWord(globalVendor.getDefaultName()));
        option.setId(globalVendorId);
        option.setPublished(globalVendor.isPublished());
        option.setParamId(vendorParam.getId());
        OptionImpl localVendor = new OptionImpl(option, Option.OptionType.VENDOR);
        localVendor.setId(localVendorId);
        localVendor.setParamId(vendorParam.getId());
        localVendor.setPublished(globalVendor.isPublished());
        return localVendor;
    }
}
