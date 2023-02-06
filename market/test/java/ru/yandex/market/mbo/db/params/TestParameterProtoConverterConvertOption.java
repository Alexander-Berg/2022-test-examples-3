package ru.yandex.market.mbo.db.params;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.ParameterOptionsPositions;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("checkstyle:magicNumber")
public class TestParameterProtoConverterConvertOption {

    @Test
    public void testSimpleOptionConversion() {
        Option option = new OptionImpl();
        option.setId(10L);
        option.addName(WordUtil.defaultWord("name1"));
        MboParameters.Option protoOption = ParameterProtoConverter.convert(option, null, false, false, false).build();

        MboParameters.Option expected = MboParameters.Option.newBuilder()
            .setId(10L)
            .addName(ParameterProtoConverter.convert(WordUtil.defaultWord("name1")))
            .addAlias(MboParameters.EnumAlias.newBuilder()
                .setAlias(ParameterProtoConverter.convert(WordUtil.defaultWord("name1")))
                .setType(MboParameters.EnumAlias.Type.GENERAL)
                .build())
            .setPublished(false)
            .setActive(true)
            .setTopValue(false)
            .setIgnoredInTitle(false)
            .build();
        Assert.assertEquals(expected, protoOption);
    }

    @Test
    public void testOverriddenOptionConversion() {
        Option parent = new OptionImpl();
        parent.setId(9L);
        parent.addName(WordUtil.defaultWord("name1"));

        OptionImpl overridenOption = new OptionImpl();
        overridenOption.setId(10L);
        parent.addName(WordUtil.defaultWord("name2"));
        overridenOption.setParent(parent);
        MboParameters.Option protoOption = ParameterProtoConverter
            .convert(overridenOption, null, false, false, false)
            .build();

        MboParameters.Option expected = MboParameters.Option.newBuilder()
            .setId(9L)
            .addName(ParameterProtoConverter.convert(WordUtil.defaultWord("name2")))
            .addAlias(MboParameters.EnumAlias.newBuilder()
                .setAlias(ParameterProtoConverter.convert(WordUtil.defaultWord("name2")))
                .setType(MboParameters.EnumAlias.Type.GENERAL)
                .build())
            .setPublished(false)
            .setActive(true)
            .setTopValue(false)
            .setIgnoredInTitle(false)
            .build();
        Assert.assertEquals(expected, protoOption);
    }

    @Test
    public void testVendorOptionConversion() {
        Option parent = new OptionImpl();
        parent.setId(9L);
        parent.addName(WordUtil.defaultWord("vendor1"));

        OptionImpl vendorOption = new OptionImpl(Option.OptionType.VENDOR);
        vendorOption.setId(10L);
        vendorOption.setParent(parent);
        MboParameters.Option protoOption = ParameterProtoConverter
            .convert(vendorOption, null, true, true, true)
            .build();

        MboParameters.Option expected = MboParameters.Option.newBuilder()
            .setId(9L)
            .addName(ParameterProtoConverter.convert(WordUtil.defaultWord("vendor1")))
            .addAlias(MboParameters.EnumAlias.newBuilder()
                .setAlias(ParameterProtoConverter.convert(WordUtil.defaultWord("vendor1")))
                .setType(MboParameters.EnumAlias.Type.GENERAL)
                .build())
            .setLocalVendorId(10L)
            .setPublished(false)
            .setActive(true)
            .setTopValue(false)
            .setIgnoredInTitle(false)
            .setIsGuruVendor(true)
            .setIsFakeVendor(true)
            .build();
        Assert.assertEquals(expected, protoOption);
    }

    @Test
    public void testPositions() {
        testOptionPositions(
                2L, 2L, null, null,
                0, 0, false
        );
        testOptionPositions(
                2L, 3L, null, null,
                0, 0, false
        );
        testOptionPositions(
                3L, 3L, 2L, null,
                2, 0, false
        );
        testOptionPositions(
                4L, 4L, null, 5L,
                0, 5, true
        );
        testOptionPositions(
                5L, 5L, 6L, 7L,
                6, 7, true
        );
    }

    protected void testOptionPositions(Long optionId,
                                       Long optionIdInPosition,
                                       Long defaultPosition,
                                       Long shortListPosition,
                                       int expectedPosition,
                                       int expectedShortEnumPosition,
                                       boolean expectedShortEnumInTop) {
        MboParameters.Option convertedOption = createMboParametersOptionWithPositions(
                optionId, optionIdInPosition, defaultPosition, shortListPosition
        );
        Assert.assertEquals(expectedPosition, convertedOption.getPosition());
        Assert.assertEquals(expectedShortEnumPosition, convertedOption.getShortEnumPosition());
        Assert.assertEquals(expectedShortEnumInTop, convertedOption.getShortEnumInTop());
    }

    protected MboParameters.Option createMboParametersOptionWithPositions(Long optionId,
                                                                          Long optionIdInPosition,
                                                                          Long defaultPosition,
                                                                          Long shortListPosition) {
        Option option = new OptionImpl();
        if (optionId != null) {
            option.setId(optionId);
        }

        ParameterOptionsPositions positions = new ParameterOptionsPositions();

        Map<Long, Long> defaultPositions = new HashMap<>();
        if (optionIdInPosition != null && defaultPosition != null) {
            defaultPositions.put(optionIdInPosition, defaultPosition);
            positions.addOptionPositions(ParameterOptionsPositions.ListType.DEFAULT, defaultPositions);
        }

        Map<Long, Long> shortListPositions = new HashMap<>();
        if (optionIdInPosition != null && shortListPosition != null) {
            shortListPositions.put(optionIdInPosition, shortListPosition);
            positions.addOptionPositions(ParameterOptionsPositions.ListType.SHORT_LIST, shortListPositions);
        }

        return ParameterProtoConverter.convert(option, positions, false, false, false).build();
    }
}
