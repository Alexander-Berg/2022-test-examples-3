package ru.yandex.market.mbo.tms.photoDeduplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.mbo.db.modelstorage.DirectModelStorageOperations;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.ModelStoreInterface;
import ru.yandex.market.mbo.db.modelstorage.data.group.GroupOperationStatus;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.Duplicate;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.user.AutoUser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;

/**
 * @author Anastasiya Emelianova / orphie@ / 2/11/22
 */
@SuppressWarnings("checkstyle:all")
@Ignore
public class DuplicatesSetterServiceTest {
    ModelStorageService modelStorageService;
    DuplicatesSetterService duplicatesSetterService;
    DirectModelStorageOperations directModelStorageOperations;

    CommonModel skuWithoutDuplicates;
    CommonModel skuWithOneDuplicate;
    CommonModel skuWithTwoCyclicDuplicates;
    CommonModel skuWithMultipleLinksToOneDuplicate;
    CommonModel skuWithoutOrigHash;
    CommonModel skuWithNewPhoto;
    CommonModel skuWithDeletedPhoto;
    CommonModel skuWithFilledDuplicates;

    ResultProcessingService.Model ytModelWithoutDuplicates;
    ResultProcessingService.Model ytModelWithOneDuplicate;
    ResultProcessingService.Model ytModelWithTreeDuplicates;
    ResultProcessingService.Model ytModelWithMultipleLinksToOneDuplicate;
    ResultProcessingService.Model ytModelWithoutOrigHash;
    ResultProcessingService.Model ytModelWithNewPhoto;
    ResultProcessingService.Model ytModelWithDeletedPhoto;
    ResultProcessingService.Model ytModelWithFilledDuplicates;

    private static long UID = 1L;

    @Before
    public void before() {
        prepareModelsForTest();
        modelStorageService = Mockito.mock(ModelStorageService.class);
        directModelStorageOperations = Mockito.mock(DirectModelStorageOperations.class);
    }

    @Test
    public void fillSKUWithoutDuplicates() throws ModelStoreInterface.ModelStoreException {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithoutDuplicates);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(directModelStorageOperations.saveModel(anyList())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);

        duplicatesSetterService.processModelList(prepareYTModels());

        assertTrue(modelsToSave.get().get(0).getDuplicates().isEmpty());
        modelsToSave.get().get(0).getPictures()
                .forEach(picture -> assertEquals(Picture.PictureStatus.APPROVED, picture.getPictureStatus()));
    }

    @Test
    public void fillSKUWithOneSimpleDuplicate() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithOneDuplicate);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);
        List<Picture> picturesAfterSave = modelAfterSave.getPictures();

        assertEquals(skuWithOneDuplicate.getPictures().size(), picturesAfterSave.size());
        assertFalse(modelAfterSave.getDuplicates().isEmpty());
        assertEquals(skuWithOneDuplicate.getPictures().get(0).getUrl(), modelAfterSave.getDuplicates().get(0).getOrigUrl());
        assertEquals(skuWithOneDuplicate.getPictures().get(0).getOrigMd5(), modelAfterSave.getDuplicates().get(0).getOrigMd5());
        assertEquals(skuWithOneDuplicate.getPictures().get(1).getUrl(), modelAfterSave.getDuplicates().get(0).getDupUrl());
        assertEquals(skuWithOneDuplicate.getPictures().get(1).getOrigMd5(), modelAfterSave.getDuplicates().get(0).getDupMd5());
        assertEquals(Picture.PictureStatus.APPROVED, picturesAfterSave.get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(1).getPictureStatus());
    }

    @Test
    public void fillSKUWithCyclicDuplicates() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithTwoCyclicDuplicates);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);
        List<Picture> picturesAfterSave = modelAfterSave.getPictures();

        assertEquals(skuWithTwoCyclicDuplicates.getPictures().size(), picturesAfterSave.size());
        assertFalse(modelAfterSave.getDuplicates().isEmpty());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(1).getUrl(), modelAfterSave.getDuplicates().get(0).getOrigUrl());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(1).getOrigMd5(), modelAfterSave.getDuplicates().get(0).getOrigMd5());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(2).getUrl(), modelAfterSave.getDuplicates().get(0).getDupUrl());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(2).getOrigMd5(), modelAfterSave.getDuplicates().get(0).getDupMd5());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(3).getUrl(), modelAfterSave.getDuplicates().get(1).getOrigUrl());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(3).getOrigMd5(), modelAfterSave.getDuplicates().get(1).getOrigMd5());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(4).getUrl(), modelAfterSave.getDuplicates().get(1).getDupUrl());
        assertEquals(skuWithTwoCyclicDuplicates.getPictures().get(4).getOrigMd5(), modelAfterSave.getDuplicates().get(1).getDupMd5());
        assertEquals(Picture.PictureStatus.APPROVED, picturesAfterSave.get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, picturesAfterSave.get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(2).getPictureStatus());
        assertEquals(Picture.PictureStatus.APPROVED, picturesAfterSave.get(3).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(4).getPictureStatus());
    }

    @Test
    public void fillSKUWithLinkedDuplicates() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithMultipleLinksToOneDuplicate);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);
        List<Picture> picturesAfterSave = modelAfterSave.getPictures();

        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().size(), picturesAfterSave.size());
        assertFalse(modelAfterSave.getDuplicates().isEmpty());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(0).getUrl(), modelAfterSave.getDuplicates().get(0).getOrigUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(0).getOrigMd5(), modelAfterSave.getDuplicates().get(0).getOrigMd5());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getUrl(), modelAfterSave.getDuplicates().get(0).getDupUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getOrigMd5(), modelAfterSave.getDuplicates().get(0).getDupMd5());

        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getUrl(), modelAfterSave.getDuplicates().get(1).getOrigUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getOrigMd5(), modelAfterSave.getDuplicates().get(1).getOrigMd5());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(2).getUrl(), modelAfterSave.getDuplicates().get(1).getDupUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(2).getOrigMd5(), modelAfterSave.getDuplicates().get(1).getDupMd5());

        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getUrl(), modelAfterSave.getDuplicates().get(2).getOrigUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getOrigMd5(), modelAfterSave.getDuplicates().get(2).getOrigMd5());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(3).getUrl(), modelAfterSave.getDuplicates().get(2).getDupUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(3).getOrigMd5(), modelAfterSave.getDuplicates().get(2).getDupMd5());

        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getUrl(), modelAfterSave.getDuplicates().get(3).getOrigUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(1).getOrigMd5(), modelAfterSave.getDuplicates().get(3).getOrigMd5());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(4).getUrl(), modelAfterSave.getDuplicates().get(3).getDupUrl());
        assertEquals(skuWithMultipleLinksToOneDuplicate.getPictures().get(4).getOrigMd5(), modelAfterSave.getDuplicates().get(3).getDupMd5());

        assertEquals(Picture.PictureStatus.APPROVED, picturesAfterSave.get(0).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(1).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(2).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(3).getPictureStatus());
        assertEquals(Picture.PictureStatus.DUPLICATE, picturesAfterSave.get(4).getPictureStatus());
    }

    @Test
    public void fillSKUWithoutOrigHash() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithoutOrigHash);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);

        assertFalse(modelsToSave.get().get(0).getDuplicates().isEmpty());
        assertTrue(modelAfterSave.getDuplicates().get(0).getOrigMd5().isEmpty());
    }

    @Test
    public void fillSKUWithNewPhoto() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithNewPhoto);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);
        CommonModel skuBeforeSave = new CommonModel(skuWithNewPhoto);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);

        assertFalse(modelsToSave.get().get(0).getDuplicates().isEmpty());

        List<Picture> picturesAfterSave = modelAfterSave.getPictures();
        assertEquals(skuBeforeSave.getPictures().size(), picturesAfterSave.size());
        assertEquals(Picture.PictureStatus.NEW, modelAfterSave.getPictures().get(2).getPictureStatus());
    }

    @Test
    public void fillSKUWithDeletedPhoto() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithDeletedPhoto);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);
        CommonModel skuBeforeSave = new CommonModel(skuWithDeletedPhoto);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);

        assertTrue(modelsToSave.get().get(0).getDuplicates().isEmpty());

        List<Picture> picturesAfterSave = modelAfterSave.getPictures();
        assertEquals(skuBeforeSave.getPictures().size(), picturesAfterSave.size());
    }

    @Test
    public void fillSKUWithExistingDuplicatesField() {
        List<CommonModel> testBatch = new ArrayList<>();
        testBatch.add(skuWithFilledDuplicates);
        Mockito.when(modelStorageService.searchByIds(anyCollection())).thenReturn(testBatch);

        AtomicReference<List<CommonModel>> modelsToSave = new AtomicReference<>(new ArrayList<>());
        Mockito.when(modelStorageService.saveModels(anyCollection(), any())).thenAnswer(I -> {
            List<CommonModel> a = I.getArgument(0);
            a.forEach(p -> modelsToSave.get().add(p));
            GroupOperationStatus status = mock(GroupOperationStatus.class);
            Mockito.when(status.isOk()).thenReturn(true);
            return status;
        });

        duplicatesSetterService = new DuplicatesSetterService(directModelStorageOperations, new AutoUser(UID), modelStorageService);

        CommonModel skuBeforeSave = new CommonModel(skuWithFilledDuplicates);

        duplicatesSetterService.processModelList(prepareYTModels());

        CommonModel modelAfterSave = modelsToSave.get().get(0);

        assertFalse(modelsToSave.get().get(0).getDuplicates().isEmpty());

        List<Picture> picturesAfterSave = modelAfterSave.getPictures();
        assertEquals(skuWithFilledDuplicates.getPictures().size(), picturesAfterSave.size());
        assertEquals(skuBeforeSave.getDuplicates().size() + 1, modelAfterSave.getDuplicates().size());
    }


    private CommonModel createModelFromModelStorage(Long id, CommonModel.Source type) {
        CommonModel model = new CommonModel();
        model.setCurrentType(type);
        model.setSource(type);
        model.setId(id);
        model.setPublished(true);
        return model;
    }

    private CommonModel createSKUFromModelStorage(Long id) {
        return createModelFromModelStorage(id, CommonModel.Source.SKU);
    }

    private Picture modelPicture(String url, String xslName, CommonModel.Source type, String urlOrig, String md5Hash) {
        Picture picture = new Picture();
        picture.setUrl(url);
        picture.setUrlOrig(urlOrig);
        picture.setOrigMd5(md5Hash);
        if (type.equals(CommonModel.Source.GURU)) {
            picture.setXslName(xslName);
        }
        picture.setLastModificationDate(new Date());
        return picture;
    }

    private void prepareModelsForTest() {
        skuWithoutDuplicates = createSKUFromModelStorage(1L);
        Picture pic1 = modelPicture("url1", "", CommonModel.Source.SKU, "url1Orig", "hash1");
        pic1.setPictureStatus(Picture.PictureStatus.APPROVED);
        Picture pic2 = modelPicture("url2", "", CommonModel.Source.SKU, "url2Orig", "hash2");
        List<Picture> pictures1 = new ArrayList<>();
        pictures1.add(pic1);
        pictures1.add(pic2);
        skuWithoutDuplicates.setPictures(pictures1);

        skuWithOneDuplicate = createSKUFromModelStorage(2L);
        Picture pic3 = modelPicture("url3", "", CommonModel.Source.SKU, "url3Orig", "hash3");
        Picture pic4 = modelPicture("url4", "", CommonModel.Source.SKU, "url4Orig", "hash4");
        List<Picture> pictures2 = new ArrayList<>();
        pictures2.add(pic3);
        pictures2.add(pic4);
        skuWithOneDuplicate.setPictures(pictures2);

        skuWithTwoCyclicDuplicates = createSKUFromModelStorage(3L);
        Picture pic5 = modelPicture("url5", "", CommonModel.Source.SKU, "url5Orig", "hash5");
        Picture pic6 = modelPicture("url6", "", CommonModel.Source.SKU, "url6Orig", "hash6");
        Picture pic7 = modelPicture("url7", "", CommonModel.Source.SKU, "url7Orig", "hash7");
        Picture pic8 = modelPicture("url8", "", CommonModel.Source.SKU, "url8Orig", "hash8");
        Picture pic9 = modelPicture("url9", "", CommonModel.Source.SKU, "url9Orig", "hash9");
        List<Picture> pictures3 = new ArrayList<>();
        pictures3.add(pic5);
        pictures3.add(pic6);
        pictures3.add(pic7);
        pictures3.add(pic8);
        pictures3.add(pic9);
        skuWithTwoCyclicDuplicates.setPictures(pictures3);

        skuWithMultipleLinksToOneDuplicate = createSKUFromModelStorage(4L);
        Picture pic10 = modelPicture("url10", "", CommonModel.Source.SKU, "url10Orig", "hash10");
        Picture pic11 = modelPicture("url11", "", CommonModel.Source.SKU, "url11Orig", "hash11");
        Picture pic12 = modelPicture("url12", "", CommonModel.Source.SKU, "url12Orig", "hash12");
        Picture pic13 = modelPicture("url13", "", CommonModel.Source.SKU, "url13Orig", "hash13");
        Picture pic14 = modelPicture("url14", "", CommonModel.Source.SKU, "url14Orig", "hash14");
        List<Picture> pictures4 = new ArrayList<>();
        pictures4.add(pic10);
        pictures4.add(pic11);
        pictures4.add(pic12);
        pictures4.add(pic13);
        pictures4.add(pic14);
        skuWithMultipleLinksToOneDuplicate.setPictures(pictures4);

        skuWithoutOrigHash = createSKUFromModelStorage(5L);
        Picture pic15 = modelPicture("url15", "", CommonModel.Source.SKU, "url15Orig", null);
        Picture pic16 = modelPicture("url16", "", CommonModel.Source.SKU, "url16Orig", "hash16");
        List<Picture> pictures5 = new ArrayList<>();
        pictures5.add(pic15);
        pictures5.add(pic16);
        skuWithoutOrigHash.setPictures(pictures5);

        skuWithNewPhoto = createSKUFromModelStorage(6L);
        Picture pic17 = modelPicture("url17", "", CommonModel.Source.SKU, "url17Orig", "hash17");
        Picture pic18 = modelPicture("url18", "", CommonModel.Source.SKU, "url18Orig", "hash18");
        Picture pic19 = modelPicture("url19", "", CommonModel.Source.SKU, "url19Orig", "hash19");
        pic19.setPictureStatus(Picture.PictureStatus.NEW);
        List<Picture> pictures6 = new ArrayList<>();
        pictures6.add(pic17);
        pictures6.add(pic18);
        pictures6.add(pic19);
        skuWithNewPhoto.setPictures(pictures6);

        skuWithDeletedPhoto = createSKUFromModelStorage(7L);
        Picture pic20 = modelPicture("url20", "", CommonModel.Source.SKU, "url20Orig", "hash20");
        List<Picture> pictures7 = new ArrayList<>();
        pictures7.add(pic20);
        skuWithDeletedPhoto.setPictures(pictures7);

        skuWithFilledDuplicates = createSKUFromModelStorage(8L);
        Picture pic21 = modelPicture("url21", "", CommonModel.Source.SKU, "url21Orig", "hash21");
        Picture pic22 = modelPicture("url22", "", CommonModel.Source.SKU, "url22Orig", "hash22");
        Picture pic23 = modelPicture("url23", "", CommonModel.Source.SKU, "url23Orig", "hash23");
        Picture pic24 = modelPicture("url24", "", CommonModel.Source.SKU, "url24Orig", "hash24");
        List<Picture> pictures8 = new ArrayList<>();
        pictures8.add(pic21);
        pictures8.add(pic22);
        pictures8.add(pic23);
        pictures8.add(pic24);
        skuWithFilledDuplicates.setPictures(pictures8);
        Duplicate duplicate = new Duplicate();
        duplicate.setOrigUrl(pic21.getUrlOrig());
        duplicate.setOrigMd5(pic21.getOrigMd5());
        duplicate.setDupMd5(pic22.getOrigMd5());
        duplicate.setDupUrl(pic22.getUrlOrig());
        skuWithFilledDuplicates.setDuplicates(Collections.singletonList(duplicate));
    }

    private ResultProcessingService.Model createYTModel(long id) {
        return new ResultProcessingService.Model(id);
    }

    private List<ResultProcessingService.Model> prepareYTModels() {
        List<ResultProcessingService.Model> list = new ArrayList<>();

        setYtModelWithoutDuplicates();
        setYtModelWithOneDuplicate();
        setYtModelWithTreeDuplicates();
        setYtModelWithMultipleLinksToOneDuplicate();
        setYtModelWithoutOrigHash();
        setYtModelWithNewPhoto();
        setYtModelWithDeletedPhoto();
        setYtModelWithFilledDuplicates();


        list.add(ytModelWithoutDuplicates);
        list.add(ytModelWithOneDuplicate);
        list.add(ytModelWithTreeDuplicates);
        list.add(ytModelWithMultipleLinksToOneDuplicate);
        list.add(ytModelWithoutOrigHash);
        list.add(ytModelWithNewPhoto);
        list.add(ytModelWithDeletedPhoto);
        list.add(ytModelWithFilledDuplicates);

        return list;
    }

    private void setYtModelWithoutDuplicates() {
        ytModelWithoutDuplicates = createYTModel(1L);
        ResultProcessingService.Picture picture1 = new ResultProcessingService.Picture("1_url1");
        picture1.setDuplicate(false);
        picture1.setParentId(picture1.getUrl());
        ResultProcessingService.Picture picture2 = new ResultProcessingService.Picture("1_url2");
        picture1.setDuplicate(false);
        picture1.setParentId(picture2.getUrl());
        List<ResultProcessingService.Picture> pictures1 = new ArrayList<>();
        pictures1.add(picture1);
        pictures1.add(picture2);

        ytModelWithoutDuplicates.setPictures(pictures1
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithOneDuplicate() {
        ytModelWithOneDuplicate = createYTModel(2L);
        ResultProcessingService.Picture picture3 = new ResultProcessingService.Picture("2_url3");
        picture3.setDuplicate(false);
        picture3.setParentId(picture3.getUrl());
        ResultProcessingService.Picture picture4 = new ResultProcessingService.Picture("2_url4");
        picture4.setDuplicate(true);
        picture4.setParentId(picture3.getUrl());
        List<ResultProcessingService.Picture> pictures2 = new ArrayList<>();
        pictures2.add(picture4);
        pictures2.add(picture3);
        ytModelWithOneDuplicate.setPictures(pictures2
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithTreeDuplicates() {
        ytModelWithTreeDuplicates = createYTModel(3L);
        ResultProcessingService.Picture picture5 = new ResultProcessingService.Picture("3_url5");
        picture5.setDuplicate(false);
        picture5.setParentId(picture5.getUrl());
        ResultProcessingService.Picture picture6 = new ResultProcessingService.Picture("3_url6");
        picture6.setDuplicate(false);
        picture6.setParentId(picture6.getUrl());
        ResultProcessingService.Picture picture7 = new ResultProcessingService.Picture("3_url7");
        picture7.setDuplicate(true);
        picture7.setParentId(picture6.getUrl());
        ResultProcessingService.Picture picture8 = new ResultProcessingService.Picture("3_url8");
        picture8.setDuplicate(true);
        ResultProcessingService.Picture picture9 = new ResultProcessingService.Picture("3_url9");
        picture9.setDuplicate(true);
        picture8.setParentId(picture9.getUrl());
        picture9.setParentId(picture8.getUrl());
        List<ResultProcessingService.Picture> pictures3 = new ArrayList<>();
        pictures3.add(picture5);
        pictures3.add(picture6);
        pictures3.add(picture7);
        pictures3.add(picture8);
        pictures3.add(picture9);
        ytModelWithTreeDuplicates.setPictures(pictures3
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithMultipleLinksToOneDuplicate() {
        ytModelWithMultipleLinksToOneDuplicate = createYTModel(4L);
        ResultProcessingService.Picture picture10 = new ResultProcessingService.Picture("4_url10");
        picture10.setDuplicate(false);
        picture10.setParentId(picture10.getUrl());
        ResultProcessingService.Picture picture11 = new ResultProcessingService.Picture("4_url11");
        picture11.setDuplicate(true);
        picture11.setParentId(picture10.getUrl());
        ResultProcessingService.Picture picture12 = new ResultProcessingService.Picture("4_url12");
        picture12.setDuplicate(true);
        picture12.setParentId(picture11.getUrl());
        ResultProcessingService.Picture picture13 = new ResultProcessingService.Picture("4_url13");
        picture13.setDuplicate(true);
        picture13.setParentId(picture11.getUrl());
        ResultProcessingService.Picture picture14 = new ResultProcessingService.Picture("4_url14");
        picture14.setDuplicate(true);
        picture14.setParentId(picture11.getUrl());
        List<ResultProcessingService.Picture> pictures = new ArrayList<>();
        pictures.add(picture10);
        pictures.add(picture11);
        pictures.add(picture12);
        pictures.add(picture13);
        pictures.add(picture14);
        ytModelWithMultipleLinksToOneDuplicate.setPictures(pictures
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithoutOrigHash() {
        ytModelWithoutOrigHash = createYTModel(5L);
        ResultProcessingService.Picture picture15 = new ResultProcessingService.Picture("5_url15");
        picture15.setDuplicate(false);
        picture15.setParentId(picture15.getUrl());
        ResultProcessingService.Picture picture16 = new ResultProcessingService.Picture("5_url16");
        picture16.setDuplicate(true);
        picture16.setParentId(picture15.getUrl());
        List<ResultProcessingService.Picture> pictures2 = new ArrayList<>();
        pictures2.add(picture16);
        pictures2.add(picture15);
        ytModelWithoutOrigHash.setPictures(pictures2
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithNewPhoto() {
        ytModelWithNewPhoto = createYTModel(6L);
        ResultProcessingService.Picture picture17 = new ResultProcessingService.Picture("6_url17");
        picture17.setDuplicate(false);
        picture17.setParentId(picture17.getUrl());
        ResultProcessingService.Picture picture18 = new ResultProcessingService.Picture("6_url18");
        picture18.setDuplicate(true);
        picture18.setParentId(picture17.getUrl());
        List<ResultProcessingService.Picture> pictures2 = new ArrayList<>();
        pictures2.add(picture18);
        pictures2.add(picture17);
        ytModelWithNewPhoto.setPictures(pictures2
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithDeletedPhoto() {
        ytModelWithDeletedPhoto = createYTModel(7L);
        ResultProcessingService.Picture picture17 = new ResultProcessingService.Picture("7_url20");
        picture17.setDuplicate(false);
        picture17.setParentId(picture17.getUrl());
        ResultProcessingService.Picture picture18 = new ResultProcessingService.Picture("7_DELETED");
        picture18.setDuplicate(true);
        picture18.setParentId(picture17.getUrl());
        List<ResultProcessingService.Picture> pictures2 = new ArrayList<>();
        pictures2.add(picture18);
        pictures2.add(picture17);
        ytModelWithDeletedPhoto.setPictures(pictures2
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));
    }

    private void setYtModelWithFilledDuplicates() {
        ytModelWithFilledDuplicates = createYTModel(8L);

        ResultProcessingService.Picture picture17 = new ResultProcessingService.Picture("8_url21");
        picture17.setDuplicate(false);
        picture17.setParentId(picture17.getUrl());
        ResultProcessingService.Picture picture18 = new ResultProcessingService.Picture("8_url22");
        picture18.setDuplicate(true);
        picture18.setParentId(picture17.getUrl());
        ResultProcessingService.Picture picture19 = new ResultProcessingService.Picture("8_url23");
        picture19.setDuplicate(false);
        picture19.setParentId(picture19.getUrl());
        ResultProcessingService.Picture picture20 = new ResultProcessingService.Picture("8_url24");
        picture20.setDuplicate(true);
        picture20.setParentId(picture19.getUrl());
        List<ResultProcessingService.Picture> pictures2 = new ArrayList<>();
        pictures2.add(picture18);
        pictures2.add(picture17);
        pictures2.add(picture19);
        pictures2.add(picture20);
        ytModelWithFilledDuplicates.setPictures(pictures2
                .stream()
                .collect(Collectors.toMap(ResultProcessingService.Picture::getUrl, Function.identity())));

    }
}
