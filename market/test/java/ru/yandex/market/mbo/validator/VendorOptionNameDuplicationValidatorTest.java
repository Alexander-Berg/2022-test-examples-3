package ru.yandex.market.mbo.validator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceMock;
import ru.yandex.market.mbo.gwt.exceptions.dto.OptionParametersDuplicationDto;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.linkedvalues.InitializedValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.CategoryParamBuilder;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.utils.OptionUtils;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dmserebr
 * @date 18.10.18
 *
 * Тестируется валидация на уникальность связанных опций для вендорских опций.
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class VendorOptionNameDuplicationValidatorTest {

    private static final long VENDOR_LINE_1 = 101L;
    private static final long VENDOR_LINE_2 = 102L;
    private static final long VENDOR_LINE_3 = 103L;
    private static final long VENDOR_1 = 201L;
    private static final long VENDOR_2 = 202L;

    private static final long VENDOR_PARAM_ID = 7893318L;

    private OptionNameDuplicationValidator validator;

    private ValueLinkServiceMock valueLinkService;

    private ParameterLoaderServiceStub parameterLoaderService;
    private Parameter paramVendorLine;
    private Parameter paramVendor;
    private Multimap<Long, Long> optionIdsByParamId = ArrayListMultimap.create();

    @Before
    public void before() {
        parameterLoaderService = new ParameterLoaderServiceStub();
        validator = new OptionNameDuplicationValidator();
        valueLinkService = new ValueLinkServiceMock();
        valueLinkService.setOptionIdsByParamId(optionIdsByParamId);
        validator.setValueLinkService(valueLinkService);
        validator.setParameterLoaderService(parameterLoaderService);

        paramVendorLine = CategoryParamBuilder.newBuilder(1, XslNames.VENDOR_LINE, Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(VENDOR_LINE_1).addName("vendor-line-1"))
            .addOption(OptionBuilder.newBuilder(VENDOR_LINE_2).addName("vendor-line-2"))
            .build();
        paramVendor = CategoryParamBuilder.newBuilder(VENDOR_PARAM_ID, XslNames.VENDOR, Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(VENDOR_1).addName("vendor-1"))
            .addOption(OptionBuilder.newBuilder(VENDOR_2).addName("vendor-2"))
            .build();

        parameterLoaderService.addAllCategoryParams(Arrays.asList(paramVendorLine, paramVendor));
    }

    @Test
    public void testVendorLineNotDuplicated() {
        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("vendor-line-3").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
            addOption,
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testVendorLineDuplicatedInGroup() {
        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("vendor-line-2").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
            OptionBuilder.newBuilder().addName("vendor-line-2").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
                ImmutableMap.of("vendor-line-2",
                    new HashSet<>(Arrays.asList(
                        OptionBuilder.newBuilder(VENDOR_LINE_2).addName("vendor-line-2").build(),
                        OptionBuilder.newBuilder().addName("vendor-line-2").build()))));
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testVendorLineDuplicatedNotInGroup() {
        // duplicated outside of group - no error (2 "param-1-second" options are allowed in different groups)

        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("vendor-line-2").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionUtils.createFakeVendorOption(VENDOR_2, VENDOR_PARAM_ID),
            OptionBuilder.newBuilder().addName("vendor-line-2").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isFalse();
        assertThat(duplications.getGroupedDuplications()).isEmpty();
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testVendorLineHasBothGroupedAndNonGroupedDuplications() {
        paramVendorLine = CategoryParamBuilder.newBuilder(1, XslNames.VENDOR_LINE, Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(VENDOR_LINE_1).addName("vendor-line-1"))
            .addOption(OptionBuilder.newBuilder(VENDOR_LINE_2).addName("vendor-line-2"))
            .addOption(OptionBuilder.newBuilder(VENDOR_LINE_3).addName("vendor-line-3"))
            .build();

        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2, VENDOR_LINE_3));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("vendor-line-2").build();
        Option addOption2 = OptionBuilder.newBuilder().addName("vendor-line-3").build();
        changes.getAdded().add(addOption);
        changes.getAdded().add(addOption2);

        changes.getAddedLinks().add(new InitializedValueLink(
            OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
            OptionBuilder.newBuilder().addName("vendor-line-2").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
                ImmutableMap.of("vendor-line-2",
                    new HashSet<>(Arrays.asList(
                        OptionBuilder.newBuilder(VENDOR_LINE_2).addName("vendor-line-2").build(),
                        OptionBuilder.newBuilder().addName("vendor-line-2").build()))));
        assertThat(duplications.getNonGroupedDuplications()).hasSize(1)
            .containsEntry("vendor-line-3", new HashSet<>(
                Arrays.asList(paramVendorLine.getOption(VENDOR_LINE_3), addOption2)));
    }

    @Test
    public void testVendorLineDuplicatedWhenChangedInGroup() {
        // Vendor lines #1 & #2 linked to vendor #1
        // We rename vendor line #2 as vendor line #1 - we get error, as both lines are in the same group

        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(VENDOR_LINE_2).addName("vendor-line-1").build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
                ImmutableMap.of("vendor-line-1",
                    new HashSet<>(Arrays.asList(
                        OptionBuilder.newBuilder(VENDOR_LINE_1).addName("vendor-line-1").build(),
                        OptionBuilder.newBuilder(VENDOR_LINE_2).addName("vendor-line-1").build()))));
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }

    @Test
    public void testVendorLineNotDuplicatedWhenChangedNotInGroup() {
        // Vendor lines #1 & #2 linked to vendor #1. Option #3 is not linked to any vendor.
        // We rename vendor line #3 as vendor line #1 - no error, as duplication are checked only in group

        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2, VENDOR_LINE_3));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option updatedOption = OptionBuilder.newBuilder(VENDOR_LINE_3).addName("vendor-line-1").build();
        changes.getUpdated().add(updatedOption);

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isFalse();
    }

    @Test
    public void testVendorLineNotDuplicatedWhenDeleted() {
        // Vendor lines #1 & #2 linked to vendor #1
        // Line #2 is deleted - no error

        optionIdsByParamId.putAll(1L, Arrays.asList(VENDOR_LINE_1, VENDOR_LINE_2));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_1, LinkDirection.DIRECT));
        valueLinkService.saveValueLink(
            new ValueLink(VENDOR_PARAM_ID, VENDOR_1, 1L, VENDOR_LINE_2, LinkDirection.DIRECT));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        changes.getDeletedLinks().add(new InitializedValueLink(
            OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
            OptionBuilder.newBuilder().addName("vendor-line-2").setParamId(1L).build(),
            LinkDirection.DIRECT, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramVendorLine, changes);

        assertThat(duplications.haveDuplications()).isFalse();
    }

    @Test
    public void testReverseLinkDuplicatedWhenAdded() {
        // Test for reverse direction (licensor param to vendor)
        // Licensor options are duplicated, duplications grouped by vendor

        CategoryParam paramLicensor = CategoryParamBuilder.newBuilder(KnownIds.LICENSOR_PARAM_ID,
            XslNames.LICENSOR, Param.Type.ENUM)
            .addOption(OptionBuilder.newBuilder(300L).addName("licensor-1"))
            .addOption(OptionBuilder.newBuilder(301L).addName("licensor-2"))
            .build();
        parameterLoaderService.addCategoryParam(paramLicensor);

        optionIdsByParamId.putAll(KnownIds.LICENSOR_PARAM_ID, Arrays.asList(200L, 201L));
        optionIdsByParamId.putAll(VENDOR_PARAM_ID, Arrays.asList(VENDOR_1, VENDOR_2));

        valueLinkService.saveValueLink(
            new ValueLink(KnownIds.LICENSOR_PARAM_ID, 300L, VENDOR_PARAM_ID, VENDOR_1, LinkDirection.REVERSE));
        valueLinkService.saveValueLink(
            new ValueLink(KnownIds.LICENSOR_PARAM_ID, 301L, VENDOR_PARAM_ID, VENDOR_1, LinkDirection.REVERSE));

        ParameterValuesChanges changes = new ParameterValuesChanges();
        Option addOption = OptionBuilder.newBuilder().addName("licensor-2").build();
        changes.getAdded().add(addOption);
        changes.getAddedLinks().add(new InitializedValueLink(
            OptionBuilder.newBuilder().addName("licensor-2").build(),
            OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
            LinkDirection.REVERSE, ValueLinkType.GENERAL));

        OptionParametersDuplicationDto duplications = validator.findDuplications(paramLicensor, changes);

        assertThat(duplications.haveDuplications()).isTrue();
        assertThat(duplications.getGroupedDuplications()).hasSize(1)
            .containsEntry(OptionUtils.createFakeVendorOption(VENDOR_1, VENDOR_PARAM_ID),
                ImmutableMap.of("licensor-2",
                    new HashSet<>(Arrays.asList(
                        OptionBuilder.newBuilder().addName("licensor-2").build(),
                        OptionBuilder.newBuilder(301L).addName("licensor-2").build()))));
        assertThat(duplications.getNonGroupedDuplications()).isEmpty();
    }
}
