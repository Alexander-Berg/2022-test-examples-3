package ru.yandex.market.mbo.db.modelstorage.preprocessing.preprocessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.ModelSaveContext;
import ru.yandex.market.mbo.db.modelstorage.StatsModelQueryService;
import ru.yandex.market.mbo.gwt.models.ModificationSource;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Duplicate;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Anastasiya Emelianova / orphie@ / 2/14/22
 */
@SuppressWarnings("checkstyle:all")
public class PicturesDuplicatesPreprocessorTest extends BasePreprocessorTest {
    private PicturesDuplicatesPreprocessor picturesDuplicatesPreprocessor;
    private StatsModelQueryService modelQueryService = Mockito.mock(StatsModelQueryService.class);
    private Date modPictureDate = new Date();

    @Before
    public void before() {
        super.before();
        picturesDuplicatesPreprocessor = new PicturesDuplicatesPreprocessor(modelQueryService);
    }

    @Test
    public void testDeleteDuplicateWithDefaultReplaceFlag() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        List<Duplicate> duplicates = new ArrayList<>();
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);


        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.setDuplicates(duplicates);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertFalse(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testDeleteDuplicateWithTrueReplaceFlag() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.NEW, ModificationSource.OPERATOR_FILLED));
        List<Duplicate> duplicates = new ArrayList<>();
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);


        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.setDuplicates(duplicates);
        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        modelSaveContext.setReplaceAllPictures(true);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(1, modelForSave.getPictures().size());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testAddNewPictureByNotOperator() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", null, ModificationSource.VENDOR_OFFICE));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertEquals("url2", modelForSave.getPictures().get(1).getUrl());
        assertEquals(Picture.PictureStatus.NEW, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testAddNewPictureByOperator() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", null, ModificationSource.OPERATOR_FILLED));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertEquals("url2", modelForSave.getPictures().get(1).getUrl());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testAddNewPictureWithApprovedStatus() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.APPROVED, ModificationSource.VENDOR_OFFICE));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertEquals("url2", modelForSave.getPictures().get(1).getUrl());
        assertEquals(Picture.PictureStatus.NEW, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromNewToApproved() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.NEW, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.APPROVED, ModificationSource.VENDOR_OFFICE));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(1).getPictureStatus());
    }

    @Test
    public void testChangeStatusFromNewToDuplicateWithoutInfoInDuplicates() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.NEW, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.NEW, modelForSave.getPictures().get(1).getPictureStatus());
    }

    @Test
    public void testChangeStatusFromNewToDuplicateWithInfoInDuplicates() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.NEW, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        modelForSave.setDuplicates(duplicates);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(2, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.DUPLICATE, modelForSave.getPictures().get(1).getPictureStatus());
        assertFalse(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromApprovedToNew() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url", "", CommonModel.Source.SKU, "urlOrig", "hash", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url", "", CommonModel.Source.SKU, "urlOrig", "hash", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.NEW, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(4, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.NEW, modelForSave.getPictures().get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(2).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromApprovedToDuplicateWithoutDuplicatesInfo() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, modelForSave.getPictures().get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(2).getPictureStatus());
        assertFalse(modelForSave.getDuplicates().isEmpty());
        assertEquals(1, modelForSave.getDuplicates().size());
    }

    @Test
    public void testChangeStatusFromApprovedToDuplicateWithDuplicatesInfo() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url", "", CommonModel.Source.SKU, "urlOrig", "hash", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url", "", CommonModel.Source.SKU, "urlOrig", "hash", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup2 = new Duplicate();
        dup2.setOrigUrl("url2");
        dup2.setOrigMd5("hash2");
        dup2.setDupUrl("url1");
        dup2.setDupMd5("hash1");
        List<Duplicate> duplicates1 = new ArrayList<>();
        duplicates1.add(dup2);
        modelForSave.setDuplicates(duplicates1);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(4, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.DUPLICATE, modelForSave.getPictures().get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(2).getPictureStatus());
        assertFalse(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromDuplicateToNew() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.NEW, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup2 = new Duplicate();
        dup2.setOrigUrl("url1");
        dup2.setOrigMd5("hash1");
        dup2.setDupUrl("url2");
        dup2.setDupMd5("hash2");
        List<Duplicate> duplicates1 = new ArrayList<>();
        duplicates1.add(dup2);
        modelForSave.setDuplicates(duplicates1);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.NEW, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromDuplicateToApproved() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.APPROVED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup2 = new Duplicate();
        dup2.setOrigUrl("url1");
        dup2.setOrigMd5("hash1");
        dup2.setDupUrl("url2");
        dup2.setDupMd5("hash2");
        List<Duplicate> duplicates1 = new ArrayList<>();
        duplicates1.add(dup2);
        modelForSave.setDuplicates(duplicates1);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangePictureOrder() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.VENDOR_OFFICE));
        Duplicate dup2 = new Duplicate();
        dup2.setOrigUrl("url1");
        dup2.setOrigMd5("hash1");
        dup2.setDupUrl("url2");
        dup2.setDupMd5("hash2");
        List<Duplicate> duplicates1 = new ArrayList<>();
        duplicates1.add(dup2);
        modelForSave.setDuplicates(duplicates1);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, modelForSave.getPictures().get(2).getPictureStatus());
        assertFalse(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromDuplicateToDeleted() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DUPLICATE, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DELETED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup2 = new Duplicate();
        dup2.setOrigUrl("url1");
        dup2.setOrigMd5("hash1");
        dup2.setDupUrl("url2");
        dup2.setDupMd5("hash2");
        List<Duplicate> duplicates1 = new ArrayList<>();
        duplicates1.add(dup2);
        modelForSave.setDuplicates(duplicates1);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.DELETED, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromApprovedToDeletedNotFirst() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DELETED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.DELETED, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromApprovedToDeletedFirst() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.DELETED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DELETED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals("url3", modelForSave.getPictures().get(0).getUrl());
        assertEquals(Picture.PictureStatus.DELETED, modelForSave.getPictures().get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.DELETED, modelForSave.getPictures().get(2).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromDeletedToApproved() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DELETED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup1 = new Duplicate();
        dup1.setOrigUrl("url1");
        dup1.setOrigMd5("hash1");
        dup1.setDupUrl("url2");
        dup1.setDupMd5("hash2");
        List<Duplicate> duplicates = new ArrayList<>();
        duplicates.add(dup1);
        oldModel.setDuplicates(duplicates);

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.APPROVED, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        Duplicate dup2 = new Duplicate();
        dup2.setOrigUrl("url1");
        dup2.setOrigMd5("hash1");
        dup2.setDupUrl("url2");
        dup2.setDupMd5("hash2");
        List<Duplicate> duplicates1 = new ArrayList<>();
        duplicates1.add(dup2);
        modelForSave.setDuplicates(duplicates1);

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().stream().filter(p -> p.getPictureStatus() == Picture.PictureStatus.APPROVED).count());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }

    @Test
    public void testChangeStatusFromDeletedToNew() {
        CommonModel oldModel = model(1L);
        oldModel.setPublished(true);
        oldModel.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.DELETED, ModificationSource.OPERATOR_FILLED));
        oldModel.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        CommonModel modelForSave = model(1L);
        modelForSave.setPublished(true);
        modelForSave.addPicture(modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));
        modelForSave.addPicture(modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2", Picture.PictureStatus.NEW, ModificationSource.VENDOR_OFFICE));
        modelForSave.addPicture(modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3", Picture.PictureStatus.APPROVED, ModificationSource.OPERATOR_FILLED));

        ModelSaveContext modelSaveContext = new ModelSaveContext(USER_ID);
        PicturesDuplicatesPreprocessor.PicturesDuplicates pd = new PicturesDuplicatesPreprocessor.PicturesDuplicates(modelSaveContext, modelForSave, oldModel);
        pd.process();

        assertEquals(3, modelForSave.getPictures().size());
        assertEquals(Picture.PictureStatus.APPROVED, modelForSave.getPictures().get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.NEW, modelForSave.getPictures().get(1).getPictureStatus());
        assertTrue(modelForSave.getDuplicates().isEmpty());
    }


    private Picture modelPicture(String url,
                                 String xslName,
                                 CommonModel.Source type,
                                 String urlOrig,
                                 String md5Hash,
                                 Picture.PictureStatus status,
                                 ModificationSource modificationSource) {
        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setUrlOrig(urlOrig);
        picture.setOrigMd5(md5Hash);
        if (type.equals(CommonModel.Source.GURU)) {
            picture.setXslName(xslName);
        }
        picture.setLastModificationDate(modPictureDate);
        picture.setPictureStatus(status);
        picture.setModificationSource(modificationSource);
        return picture;
    }
}
