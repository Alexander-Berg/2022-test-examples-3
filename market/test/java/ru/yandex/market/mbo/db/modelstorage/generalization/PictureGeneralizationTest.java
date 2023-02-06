package ru.yandex.market.mbo.db.modelstorage.generalization;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.stubs.ModelStorageServiceStub;
import ru.yandex.market.mbo.db.modelstorage.validation.ModelValidationError;
import ru.yandex.market.mbo.db.modelstorage.validation.PicturesModelValidator;
import ru.yandex.market.mbo.export.client.CategoryParametersServiceClientStub;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.GeneralizationStrategy;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelChanges;
import ru.yandex.market.mbo.gwt.models.modelstorage.ParameterValue;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.models.modelstorage.PictureBuilder;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.rules.ParameterBuilder;
import ru.yandex.market.mbo.gwt.utils.WordUtil;
import ru.yandex.market.mbo.user.AutoUser;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author dmserebr
 * @date 23.04.18
 */
@SuppressWarnings("checkstyle:magicNumber")
public class PictureGeneralizationTest extends BaseGeneralizationTest {
    private PicturesModelValidator validator;
    private static final String NAMESPACE = "mpic-fake";
    private static final String AVATARS_HOST1 = "avatars.mdst-fake.yandex.net";

    private static final Long PICTURE_PARAM_ID_1 = 501L;
    private static final Long PICTURE_PARAM_ID_2 = 502L;
    private static final Long PICTURE_URL_PARAM_ID_1 = 511L;
    private static final Long PICTURE_URL_PARAM_ID_2 = 512L;
    private static final Long PICTURE_WIDTH_PARAM_ID_1 = 521L;
    private static final Long PICTURE_WIDTH_PARAM_ID_2 = 522L;
    private static final Long PICTURE_HEIGHT_PARAM_ID_1 = 531L;
    private static final Long PICTURE_HEIGHT_PARAM_ID_2 = 532L;

    private static final Map<Integer, Long> PICTURE_NUMBER_TO_PICTURE_PARAM_ID = ImmutableMap.of(
        1, PICTURE_PARAM_ID_1,
        2, PICTURE_PARAM_ID_2);
    private static final Map<Integer, Long> PICTURE_NUMBER_TO_PICTURE_URL_PARAM_ID = ImmutableMap.of(
        1, PICTURE_URL_PARAM_ID_1,
        2, PICTURE_URL_PARAM_ID_2);
    private static final Map<Integer, Long> PICTURE_NUMBER_TO_PICTURE_WIDTH_PARAM_ID = ImmutableMap.of(
        1, PICTURE_WIDTH_PARAM_ID_1,
        2, PICTURE_WIDTH_PARAM_ID_2);
    private static final Map<Integer, Long> PICTURE_NUMBER_TO_PICTURE_HEIGHT_PARAM_ID = ImmutableMap.of(
        1, PICTURE_HEIGHT_PARAM_ID_1,
        2, PICTURE_HEIGHT_PARAM_ID_2);

    @Before
    public void before() {
        validator = new PicturesModelValidator(singletonList(AVATARS_HOST1), NAMESPACE);
        validator.setUrlPatternValidation(false);

        List<CategoryParam> params = new ArrayList<>();
        params.add(ParameterBuilder.builder()
            .id(PICTURE_PARAM_ID_1).xsl("XL-Picture_1").type(Param.Type.STRING).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_PARAM_ID_2).xsl("XL-Picture_2").type(Param.Type.STRING).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_WIDTH_PARAM_ID_1).xsl("XLPictureSizeX_1").type(Param.Type.NUMERIC).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_WIDTH_PARAM_ID_2).xsl("XLPictureSizeX_2").type(Param.Type.NUMERIC).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_HEIGHT_PARAM_ID_1).xsl("XLPictureSizeY_1").type(Param.Type.NUMERIC).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_HEIGHT_PARAM_ID_2).xsl("XLPictureSizeY_2").type(Param.Type.NUMERIC).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_URL_PARAM_ID_1).xsl("XLPictureUrl_1").type(Param.Type.NUMERIC).endParameter());
        params.add(ParameterBuilder.builder()
            .id(PICTURE_URL_PARAM_ID_2).xsl("XLPictureUrl_2").type(Param.Type.NUMERIC).endParameter());

        generalizationService = new ModelGeneralizationServiceImpl(
            guruService, new AutoUser(AUTO_USER_ID),
            CategoryParametersServiceClientStub.ofCategoryParams(GROUP_CATEGORY_HID, params));
    }

    @Test
    public void testSaveModelPictureGeneralizedParameterValues() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", null);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertNoParamValue(modifications.get(0), PICTURE_PARAM_ID_1);
        assertNoParamValue(modifications.get(1), PICTURE_PARAM_ID_1);
        assertNoPicture(modifications.get(0), 1, "http://url1", null);
        assertNoPicture(modifications.get(1), 1, "http://url1", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModelPictureIfModifsHaveThisPictureGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = modifications.get(1);
        assertNoModel(updatedModels, modif2.getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertPicture(parentModel, 1, "http://url1", null);

        assertNoParamValue(modif1, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif2, PICTURE_PARAM_ID_1);
        assertNoPicture(modif1, 1, "http://url1", null);
        assertNoPicture(modif2, 1, "http://url1", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModelPictureIfModifsHaveThisPictureThenAddUrlGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(0), 1, "http://url1", "http://srcurl1");
        group.setStrategyType(GeneralizationStrategy.MODEL);

        generalizationService.generalizeGroup(group, true, false);
        addPictureSourceUrl(group.getParentModel(), 1, "http://srcurl1");
        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = modifications.get(1);
        assertNoModel(updatedModels, modif2.getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(parentModel, PICTURE_URL_PARAM_ID_1);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertStringParameterValueEquals(
            parentModel.getSingleParameterValue(PICTURE_URL_PARAM_ID_1), "http://srcurl1");
        assertPicture(parentModel, 1, "http://url1", "http://srcurl1");

        assertNoParamValue(modif1, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif2, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif1, PICTURE_URL_PARAM_ID_1);
        assertNoParamValue(modif2, PICTURE_URL_PARAM_ID_1);
        assertNoPicture(modif1, 1, "http://url1", "http://srcurl1");
        assertNoPicture(modif2, 1, "http://url1", "http://srcurl1");

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModelPictureIfModifsHaveDifferentPictureNotGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(0), 1, "http://url2", null);
        group.setStrategyType(GeneralizationStrategy.MODEL);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertStringParameterValueEquals(
            modifications.get(0).getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url2");
        assertNoParamValue(modifications.get(1), PICTURE_PARAM_ID_1);

        assertPicture(parentModel, 1, "http://url1", null);
        assertPicture(modifications.get(0), 1, "http://url2", null);
        assertNoPicture(modifications.get(1), 1, "http://url1", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModifPicturesAllDifferentNotGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(1), 1, "http://url2", null);
        addPictureParameterValue(modifications.get(2), 1, "http://url3", null);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());

        assertNoParamValue(parentModel, PICTURE_PARAM_ID_1);
        assertNoPicture(parentModel, 1, "http://url1", null);
        assertNoPicture(parentModel, 1, "http://url2", null);
        assertNoPicture(parentModel, 1, "http://url3", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModifSomePicturesSameNotGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(1), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(2), 1, "http://url2", null);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());
        assertNoModel(updatedModels, modifications.get(2).getId());
        assertStringParameterValueEquals(
            modifications.get(0).getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertStringParameterValueEquals(
            modifications.get(1).getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertStringParameterValueEquals(
            modifications.get(2).getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url2");

        assertNoParamValue(parentModel, PICTURE_PARAM_ID_1);
        assertNoPicture(parentModel, 1, "http://url1", null);
        assertNoPicture(parentModel, 1, "http://url2", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModifAllPicturesSameGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 3);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(1), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(2), 1, "http://url1", null);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);
        group.addBeforeModels(modifications); //not to preserve modification source

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());
        CommonModel modif3 = getAndAssertModel(updatedModels, modifications.get(2).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertNoParamValue(modif1, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif2, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif3, PICTURE_PARAM_ID_1);

        assertPicture(parentModel, 1, "http://url1", null);
        assertNoPicture(modif1, 1, "http://url1", null);
        assertNoPicture(modif2, 1, "http://url1", null);
        assertNoPicture(modif3, 1, "http://url1", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModifPictureSameInModelGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);
        group.addBeforeModels(modifications); //not to preserve modification source

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel updatedModif = getAndAssertModel(updatedModels, modifications.get(0).getId());
        assertNoModel(updatedModels, modifications.get(1).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertNoParamValue(updatedModif, PICTURE_PARAM_ID_1);
        assertNoParamValue(modifications.get(1), PICTURE_PARAM_ID_1);

        assertPicture(parentModel, 1, "http://url1", null);
        assertNoPicture(updatedModif, 1, "http://url1", null);
        assertNoPicture(modifications.get(1), 1, "http://url1", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveModifPictureDifferentInModelGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", null, true);
        addPictureParameterValue(modifications.get(0), 1, "http://url2", null, true);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        //CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());
        CommonModel modif1 = modifications.get(0);
        assertNoModel(updatedModels, modif1.getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());

        assertNoParamValue(parentModel, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(modif2, PICTURE_PARAM_ID_1);
        assertStringParameterValueEquals(modif1.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url2");
        assertStringParameterValueEquals(modif2.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");

        assertNoPicture(parentModel, 1, "http://url1", null);
        assertNoPicture(parentModel, 1, "http://url2", null);
        assertPicture(modif1, 1, "http://url2", null);
        assertPicture(modif2, 1, "http://url1", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveOneModifTwoPicturesGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 1);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        addPictureParameterValue(modifications.get(0), 2, "http://url2", null);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);
        group.addBeforeModels(modifications); //not to preserve modification source

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_2);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_2), "http://url2");
        assertNoParamValue(modif1, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif1, PICTURE_PARAM_ID_2);

        assertPicture(parentModel, 1, "http://url1", null);
        assertPicture(parentModel, 2, "http://url2", null);
        assertNoPicture(modif1, 1, "http://url1", null);
        assertNoPicture(modif1, 2, "http://url2", null);

        validateResult(updatedModels);
    }

    @Test
    public void testSaveOneModifPictureWithSourceUrlGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 1);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(modifications.get(0), 1, "http://url1", "http://srcurl1");
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);
        group.addBeforeModels(modifications); //not to preserve modification source

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(parentModel, PICTURE_URL_PARAM_ID_1);
        assertStringParameterValueEquals(parentModel.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");
        assertStringParameterValueEquals(
            parentModel.getSingleParameterValue(PICTURE_URL_PARAM_ID_1), "http://srcurl1");
        assertNoParamValue(modif1, PICTURE_PARAM_ID_1);
        assertNoParamValue(modif1, PICTURE_URL_PARAM_ID_1);

        assertPicture(parentModel, 1, "http://url1", "http://srcurl1");
        assertNoPicture(modif1, 1, "http://url1", "http://srcurl1");

        validateResult(updatedModels);
    }

    @Test
    public void testSaveTwoModifDifferentSourceUrlGeneralizedThenDifferentPictureGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 2);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(group.getParentModel(), 1, "http://url1", "http://srcurl1");
        addPictureParameterValue(modifications.get(0), 1, "http://url1", "http://srcurl2");
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = modifications.get(0);
        assertNoModel(updatedModels, modif1.getId());
        CommonModel modif2 = getAndAssertModel(updatedModels, modifications.get(1).getId());

        assertNoParamValue(parentModel, PICTURE_PARAM_ID_1);
        assertNoParamValue(parentModel, PICTURE_URL_PARAM_ID_1);
        assertParamValueGeneralized(modif1, PICTURE_URL_PARAM_ID_1);
        assertStringParameterValueEquals(modif1.getSingleParameterValue(PICTURE_URL_PARAM_ID_1), "http://srcurl2");
        assertStringParameterValueEquals(modif2.getSingleParameterValue(PICTURE_URL_PARAM_ID_1), "http://srcurl1");

        assertNoPicture(parentModel, 1, "http://url1", "http://srcurl1");
        assertPicture(modif1, 1, "http://url1", "http://srcurl2");
        assertPicture(modif2, 1, "http://url1", "http://srcurl1");

        modif1.clearPictures();
        addPictureParameterValue(modif1, 1, "http://url2", "http://srcurl2");

        updatedModels = generalizationService.generalizeGroup(group, true, false);

        parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        assertNoModel(updatedModels, modif1.getId());
        assertNoModel(updatedModels, modif2.getId());

        assertNoParamValue(parentModel, PICTURE_PARAM_ID_1);
        assertNoParamValue(parentModel, PICTURE_URL_PARAM_ID_1);
        assertParamValueGeneralized(modif1, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(modif1, PICTURE_URL_PARAM_ID_1);
        assertParamValueGeneralized(modif2, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(modif2, PICTURE_URL_PARAM_ID_1);
        assertStringParameterValueEquals(modif1.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url2");
        assertStringParameterValueEquals(modif1.getSingleParameterValue(PICTURE_URL_PARAM_ID_1), "http://srcurl2");
        assertStringParameterValueEquals(modif2.getSingleParameterValue(PICTURE_URL_PARAM_ID_1), "http://srcurl1");
        assertStringParameterValueEquals(modif2.getSingleParameterValue(PICTURE_PARAM_ID_1), "http://url1");

        assertNoPicture(parentModel, 1, "http://url1", "http://srcurl1");
        assertNoPicture(parentModel, 1, "http://url2", "http://srcurl2");
        assertPicture(modif1, 1, "http://url2", "http://srcurl2");
        assertPicture(modif2, 1, "http://url1", "http://srcurl1");

        validateResult(updatedModels);
    }

    @Test
    public void testSaveOneModifPictureGeneralizedThenWithSourceUrlGeneralized() {
        GeneralizationGroup group = createGroup(GROUP_CATEGORY_HID, 1);
        List<CommonModel> modifications = new ArrayList<>(group.getModifications());
        addPictureParameterValue(modifications.get(0), 1, "http://url1", null);
        group.setStrategyType(GeneralizationStrategy.MODIFICATION);
        group.addBeforeModels(modifications); //not to preserve modification source

        generalizationService.generalizeGroup(group, true, false);

        addPictureParameterValue(modifications.get(0), 1, "http://url1", "http://srcurl1");

        List<CommonModel> updatedModels = generalizationService.generalizeGroup(group, true, false);

        CommonModel parentModel = getAndAssertModel(updatedModels, group.getParentModelId());
        CommonModel modif1 = getAndAssertModel(updatedModels, modifications.get(0).getId());

        assertParamValueGeneralized(parentModel, PICTURE_PARAM_ID_1);
        assertParamValueGeneralized(parentModel, PICTURE_URL_PARAM_ID_1);
        assertNoParamValue(modif1, PICTURE_URL_PARAM_ID_1);

        assertPicture(parentModel, 1, "http://url1", "http://srcurl1");
        assertNoPicture(modif1, 1, "http://url1", "http://srcurl1");

        validateResult(updatedModels);
    }

    private void addPictureParameterValue(CommonModel model, Integer picNumber, String url, String sourceUrl) {
        addPictureParameterValue(model, picNumber, url, sourceUrl, true);
    }

    /**
     * @param uniqueSize if true, different pics (with different urls) have different sizes;
     *                   if false, only pics with different xsl names have different sizes
     */
    private void addPictureParameterValue(CommonModel model, Integer picNumber, String url, String sourceUrl,
                                          boolean uniqueSize) {
        ParameterValue paramValue = new ParameterValue();
        paramValue.setParamId(PICTURE_NUMBER_TO_PICTURE_PARAM_ID.get(picNumber));
        paramValue.setXslName("XL-Picture_" + picNumber);
        paramValue.setType(Param.Type.STRING);
        paramValue.setStringValue(WordUtil.defaultWords(url));
        paramValue.setModificationSource(ModificationSource.OPERATOR_FILLED);
        paramValue.setLastModificationDate(ModelStorageServiceStub.LAST_MODIFIED_START_DATE);
        paramValue.setLastModificationUid(USER_ID);
        model.addParameterValue(paramValue);

        ParameterValue widthParamValue = new ParameterValue();
        widthParamValue.setParamId(PICTURE_NUMBER_TO_PICTURE_WIDTH_PARAM_ID.get(picNumber));
        widthParamValue.setXslName("XLPictureSizeX_" + picNumber);
        widthParamValue.setType(Param.Type.NUMERIC);
        if (uniqueSize) {
            widthParamValue.setNumericValue(new BigDecimal(url.hashCode() % 117));
        } else {
            widthParamValue.setNumericValue(new BigDecimal(picNumber));
        }
        widthParamValue.setModificationSource(ModificationSource.OPERATOR_FILLED);
        widthParamValue.setLastModificationDate(ModelStorageServiceStub.LAST_MODIFIED_START_DATE);
        widthParamValue.setLastModificationUid(USER_ID);
        model.addParameterValue(widthParamValue);

        ParameterValue heightParamValue = new ParameterValue();
        heightParamValue.setParamId(PICTURE_NUMBER_TO_PICTURE_HEIGHT_PARAM_ID.get(picNumber));
        heightParamValue.setXslName("XLPictureSizeY_" + picNumber);
        heightParamValue.setType(Param.Type.NUMERIC);
        if (uniqueSize) {
            heightParamValue.setNumericValue(new BigDecimal(url.hashCode() % 130));
        } else {
            heightParamValue.setNumericValue(new BigDecimal(picNumber));
        }

        heightParamValue.setModificationSource(ModificationSource.OPERATOR_FILLED);
        heightParamValue.setLastModificationDate(ModelStorageServiceStub.LAST_MODIFIED_START_DATE);
        heightParamValue.setLastModificationUid(USER_ID);
        model.addParameterValue(heightParamValue);

        if (sourceUrl != null) {
            addSourceUrlParameterValue(model, picNumber, sourceUrl);
        }

        Picture picture = PictureBuilder.newBuilder()
            .setXslName("XL-Picture_" + picNumber)
            .setUrl(url)
            .setWidth(uniqueSize ? 400 + url.hashCode() % 117 : picNumber * 100)
            .setHeight(uniqueSize ? 300 + url.hashCode() % 130 : picNumber * 120)
            .setUrlSource(sourceUrl)
            .build();
        model.addPicture(picture);
    }

    private void addSourceUrlParameterValue(CommonModel model, Integer picNumber, String sourceUrl) {
        ParameterValue urlParamValue = new ParameterValue();
        urlParamValue.setParamId(PICTURE_NUMBER_TO_PICTURE_URL_PARAM_ID.get(picNumber));
        urlParamValue.setXslName("XLPictureUrl_" + picNumber);
        urlParamValue.setType(Param.Type.STRING);
        urlParamValue.setStringValue(WordUtil.defaultWords(sourceUrl));
        urlParamValue.setModificationSource(ModificationSource.OPERATOR_FILLED);
        urlParamValue.setLastModificationDate(ModelStorageServiceStub.LAST_MODIFIED_START_DATE);
        urlParamValue.setLastModificationUid(USER_ID);
        model.addParameterValue(urlParamValue);
    }

    private void addPictureSourceUrl(CommonModel model, Integer picNumber, String sourceUrl) {
        String xslName = "XL-Picture_" + picNumber;
        model.getPicture(xslName).setUrlSource(sourceUrl);
    }

    private void assertStringParameterValueEquals(ParameterValue value, String str) {
        assertEquals(str, WordUtil.getDefaultWord(value.getStringValue()));
    }

    private void assertPicture(CommonModel model, Integer picNumber, String url, String sourceUrl) {
        Picture picture = model.getPicture("XL-Picture_" + picNumber);
        assertNotNull(picture);
        assertEquals(url, picture.getUrl());
        assertEquals(sourceUrl, picture.getUrlSource());
    }

    /**
     * Makes sure than the model doesn't contain any picture with the given url, urlSource
     * or XSL name corresponding to picNumber.
     */
    private void assertNoPicture(CommonModel model, Integer picNumber, String url, @Nullable String sourceUrl) {
        Picture picture = model.getPicture("XL-Picture_" + picNumber);
        if (picture == null) {
            picture = model.getPictures().stream()
                .filter(pic -> pic.getUrl().equals(url) ||
                    (sourceUrl != null && pic.getUrlSource().equals(sourceUrl)))
                .findFirst().orElse(null);
        }
        assertNull(picture);
    }

    private void validateResult(List<CommonModel> updatedModels) {
        List<ModelValidationError> errors = new ArrayList<>();
        for (CommonModel model : updatedModels) {
            errors.addAll(validator.validate(null, new ModelChanges(null, model), updatedModels));
        }
        Assertions.assertThat(errors).isEmpty();
    }
}
