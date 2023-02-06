package ru.yandex.market.mbo.synchronizer.export.licensor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.db.ParameterLoaderServiceStub;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.EnumAlias;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.licensor.MboLicensors;
import ru.yandex.market.mbo.licensor2.LicensorServiceImpl;
import ru.yandex.market.mbo.licensor2.proto.LicensorProtoBuildService;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCaseDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorExtraDAOMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorSchemeService;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraint.Source;
import ru.yandex.market.mbo.licensor2.scheme.LicensorVendorConstraintDAOMock;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfDeletedUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfExtrasUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorRuleOfRestoreUpdater;
import ru.yandex.market.mbo.licensor2.updater.LicensorVendorLinkUpdater;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExtractorBaseTestClass;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author ayratgdl
 * @date 11.05.18
 */
public class LicensorsExtractorTest extends ExtractorBaseTestClass {
    private static final int PROTO_MAGIC_PREFIX_LENGTH = 4;
    private static final long NONE_ID = 0;
    private static final long UID1 = 1;
    private static final IdAndName LICENSOR_1 = new IdAndName(101, "Licensor 1");
    private static final IdAndName LICENSOR_2 = new IdAndName(102, "Licensor 2");
    private static final IdAndName FRANCHISE_1 = new IdAndName(201, "Franchise 1");
    private static final IdAndName FRANCHISE_2 = new IdAndName(202, "Franchise 2");
    private static final IdAndName PERSONAGE_1 = new IdAndName(301, "Personage 1");
    private static final IdAndName PERSONAGE_2 = new IdAndName(302, "Personage 2");
    private static final IdAndName VENDOR_1 = new IdAndName(401, "Vendor 1");
    private static final IdAndName VENDOR_2 = new IdAndName(402, "Vendor 2");
    private static final IdAndName CATEGORY_1 = new IdAndName(501, "Category 1");
    private static final IdAndName CATEGORY_2 = new IdAndName(502, "Category 2");

    private LicensorServiceImpl licensorService;
    private ParameterLoaderServiceStub parameterLoaderService;

    private CategoryParam licensorParameter;
    private CategoryParam franchiseParameter;
    private CategoryParam personageParameter;

    @Override
    @Before
    public void setUp() throws Exception {
        LicensorSchemeService schemeService = new LicensorSchemeService();
        schemeService.setLicensorCaseDAO(new LicensorCaseDAOMock());
        schemeService.setExtraLfpDAO(new LicensorExtraDAOMock());
        schemeService.setLVConstraintDAO(new LicensorVendorConstraintDAOMock());

        licensorService = new LicensorServiceImpl();
        licensorService.setSchemeService(schemeService);
        licensorService.setRuleOfRestoreUpdater(Mockito.mock(LicensorRuleOfRestoreUpdater.class));
        licensorService.setRuleOfDeletedUpdater(Mockito.mock(LicensorRuleOfDeletedUpdater.class));
        licensorService.setRuleOfExtrasUpdater(Mockito.mock(LicensorRuleOfExtrasUpdater.class));
        licensorService.setVendorLinkUpdater(Mockito.mock(LicensorVendorLinkUpdater.class));

        parameterLoaderService = new ParameterLoaderServiceStub();
        parameterLoaderService.addCategoryParam(licensorParameter = buildParameter(KnownIds.LICENSOR_PARAM_ID));
        parameterLoaderService.addCategoryParam(franchiseParameter = buildParameter(KnownIds.FRANCHISE_PARAM_ID));
        parameterLoaderService.addCategoryParam(personageParameter = buildParameter(KnownIds.PERSONAGE_PARAM_ID));

        super.setUp();
    }

    @Override
    protected BaseExtractor createExtractor() {
        LicensorProtoBuildService licensorProtoService = new LicensorProtoBuildService();
        licensorProtoService.setLicensorService(licensorService);
        licensorProtoService.setParameterLoaderService(parameterLoaderService);

        LicensorsExtractor extractor = new LicensorsExtractor();
        extractor.setLicensorService(licensorProtoService);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        extractor.setExtractorWriterService(extractorWriterService);
        return extractor;
    }

    @Test
    public void exportEmptyData() throws Exception {
        extractor.perform("");
        List<MboLicensors.Licensor> expectedProtoLicensors = Collections.emptyList();
        Assert.assertEquals(expectedProtoLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportOneLicensorWithoutScheme() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportOneLicensorWithScheme() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), null, null));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportOneFranchiseWithoutScheme() throws Exception {
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportOneFranchiseWithScheme() throws Exception {
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE_1.getId(), null));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportOnePersonageWithoutScheme() throws Exception {
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(NONE_ID)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportOnePersonageWithScheme() throws Exception {
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(null, null, PERSONAGE_1.getId()));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(NONE_ID)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFWhereLRecNoAndLFRecLF() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), null));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.NO_RECOGNITION_LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.LICENSOR_FRANCHISE)
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFWhereLRecLAndLFRecF() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), null, null));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), null).setRestoreByF(true));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFPWhereLRecLAndLFRecNoAndLFPRecP() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), null, null));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
                .setRestoreByP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.NO_RECOGNITION_FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFPWhereLRecNoAndLFRecFAndLFPRecLPOrFP() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), null)
                .setRestoreByF(true)
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
                .setRestoreByLP(true)
                .setRestoreByFP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.NO_RECOGNITION_LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE_FRANCHISE_OR_LICENSOR)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFPWhereLRecLAndLFRecLFAndLFPRecFP() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), null, null));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), null));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
                .setRestoreByFP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.LICENSOR_FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE_FRANCHISE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFPWhereLRecLAndLFRecLFAndLFPRecLP() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), null, null));
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), null));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
                .setRestoreByLP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.LICENSOR_FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE_LICENSOR)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportLFPWhereLRecLAndLFRecFAndLFPRecLFP() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(LICENSOR_1.getId(), null, null));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), null)
                .setRestoreByF(true)
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE_FRANCHISE_LICENSOR)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportFPWhereFRecNoAndFPRecFP() throws Exception {
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE_1.getId(), PERSONAGE_1.getId()));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.NO_RECOGNITION_FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE_FRANCHISE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportFPWhereFRecFAndFPRecP() throws Exception {
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE_1.getId(), null));
        licensorService.createLicensorCase(
            new LicensorCase(null, FRANCHISE_1.getId(), PERSONAGE_1.getId())
                .setRestoreByP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportFreeLFreeFFreePAndLFPWhereLRecNoLFRecNoLFPRecP() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_2.getId())
                                        .addName(LICENSOR_2.getName())
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_2.getId())
                                         .addName(FRANCHISE_2.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_2.getId())
                                         .addName(PERSONAGE_2.getName())
                                         .build()
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_2.getId(), FRANCHISE_2.getId(), PERSONAGE_2.getId())
                .setRestoreByP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .build(),
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_2.getId())
                .addName(toProtoWord(LICENSOR_2.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.NO_RECOGNITION_LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_2.getId())
                        .addName(toProtoWord(FRANCHISE_2.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.NO_RECOGNITION_FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_2.getId())
                                .addName(toProtoWord(PERSONAGE_2.getName()))
                                .setFormalizerRecognition(
                                    MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                        )
                )
                .build(),
            MboLicensors.Licensor.newBuilder()
                .setId(NONE_ID)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.FRANCHISE)
                )
                .addFranchise(MboLicensors.Franchise.newBuilder()
                                  .setId(NONE_ID)
                                  .addCharacter(
                                      MboLicensors.Character.newBuilder()
                                          .setId(PERSONAGE_1.getId())
                                          .addName(toProtoWord(PERSONAGE_1.getName()))
                                          .setFormalizerRecognition(
                                              MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                                  )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportWithAliases() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .addAlias(buildAlias("Licensor Alias 1"))
                                        .addAlias(buildAlias("Licensor Alias 2"))
                                        .build()
        );
        franchiseParameter.addOption(new OptionBuilder()
                                         .setId(FRANCHISE_1.getId())
                                         .addName(FRANCHISE_1.getName())
                                         .addName(new Word(Language.UKRANIAN.getId(), "Franchise 1 (2)"))
                                         .addAlias(buildAlias("Franchise Alias 1"))
                                         .build()
        );
        personageParameter.addOption(new OptionBuilder()
                                         .setId(PERSONAGE_1.getId())
                                         .addName(PERSONAGE_1.getName())
                                         .addAlias(buildAlias("Personage Alias 1"))
                                         .build()
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
                .setRestoreByP(true)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .addAlias(toProtoWord("Licensor Alias 1"))
                .addAlias(toProtoWord("Licensor Alias 2"))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.NO_RECOGNITION_LICENSOR)
                .addFranchise(
                    MboLicensors.Franchise.newBuilder()
                        .setId(FRANCHISE_1.getId())
                        .addName(toProtoWord(FRANCHISE_1.getName()))
                        .addName(toProtoWord(Language.UKRANIAN, "Franchise 1 (2)"))
                        .addAlias(toProtoWord("Franchise Alias 1"))
                        .setFormalizerRecognition(MboLicensors.FranchiseFormalizerRecognition.NO_RECOGNITION_FRANCHISE)
                        .addCharacter(
                            MboLicensors.Character.newBuilder()
                                .setId(PERSONAGE_1.getId())
                                .addName(toProtoWord(PERSONAGE_1.getName()))
                                .addAlias(toProtoWord("Personage Alias 1"))
                                .setFormalizerRecognition(MboLicensors.CharacterFormalizerRecognition.PERSONAGE)
                        )
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportGlobalVendorConstraint() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1.getId(), VENDOR_1.getId(), KnownIds.GLOBAL_CATEGORY_ID,
                                         UID1, Source.MBO_UI));

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_1.getId())
                        .setCategoryId(KnownIds.GLOBAL_CATEGORY_ID)
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    @Test
    public void exportVariousVendorConstraints() throws Exception {
        licensorParameter.addOption(new OptionBuilder()
                                        .setId(LICENSOR_1.getId())
                                        .addName(LICENSOR_1.getName())
                                        .build()
        );
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1.getId(), VENDOR_1.getId(), CATEGORY_1.getId(), UID1, Source.MBO_UI)
        );
        licensorService.createVendorConstraint(
            new LicensorVendorConstraint(LICENSOR_1.getId(), VENDOR_2.getId(), CATEGORY_2.getId(), UID1, Source.MBO_UI)
        );

        extractor.perform("");

        List<MboLicensors.Licensor> expectedLicensors = Arrays.asList(
            MboLicensors.Licensor.newBuilder()
                .setId(LICENSOR_1.getId())
                .addName(toProtoWord(LICENSOR_1.getName()))
                .setFormalizerRecognition(MboLicensors.LicensorFormalizerRecognition.LICENSOR)
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_1.getId())
                        .setCategoryId(CATEGORY_1.getId())
                )
                .addVendorConstraint(
                    MboLicensors.VendorConstraint.newBuilder()
                        .setVendorId(VENDOR_2.getId())
                        .setCategoryId(CATEGORY_2.getId())
                )
                .build()
        );
        Assert.assertEquals(expectedLicensors, getLicensorsFromExport());
    }

    private Parameter buildParameter(long parameterId) {
        Parameter parameter = new Parameter();
        parameter.setCategoryHid(KnownIds.GLOBAL_CATEGORY_ID);
        parameter.setId(parameterId);
        return parameter;
    }

    private static EnumAlias buildAlias(String alias) {
        return new EnumAlias(0, Language.RUSSIAN.getId(), alias);
    }

    private static MboLicensors.Word toProtoWord(Language language, String name) {
        return MboLicensors.Word.newBuilder()
            .setLangId(language.getId())
            .setName(name)
            .build();
    }

    private static MboLicensors.Word toProtoWord(String name) {
        return toProtoWord(Language.RUSSIAN, name);
    }

    private List<MboLicensors.Licensor> getLicensorsFromExport() throws Exception {
        List<MboLicensors.Licensor> result = new ArrayList<>();
        ByteArrayInputStream input = new ByteArrayInputStream(getExtractContent());
        input.skip(PROTO_MAGIC_PREFIX_LENGTH);
        MboLicensors.Licensor licensor;
        while ((licensor = MboLicensors.Licensor.parseDelimitedFrom(input)) != null) {
            result.add(licensor);
        }
        return result;
    }
}
