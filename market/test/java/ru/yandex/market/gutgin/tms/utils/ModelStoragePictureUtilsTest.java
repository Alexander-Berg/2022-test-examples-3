package ru.yandex.market.gutgin.tms.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.market.cleanweb.client.CWImageResult;
import ru.yandex.market.cleanweb.client.CWRawVerdict;
import ru.yandex.market.gutgin.tms.service.datacamp.savemodels.update.MboPictureService;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.csku.wrappers.pictures.PictureWrapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.gutgin.tms.utils.TestUtils.buildPictureWrapper;
import static ru.yandex.market.gutgin.tms.utils.TestUtils.generateSkuWithPictures;
import static ru.yandex.market.gutgin.tms.utils.TestUtils.generateSkuWithPicturesAndSupplier;

public class ModelStoragePictureUtilsTest {
    private static final Logger log = LoggerFactory.getLogger(ModelStoragePictureUtilsTest.class);

    @Test
    public void normalFlowWithDuplicatesTest() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        List<PictureWrapper> sku1List = new ArrayList<>();
        List<PictureWrapper> list2 = new ArrayList<>();
        sku1List.add(pictureWrapper1);
        sku1List.add(pictureWrapper2);
        list2.add(pictureWrapper1);
        list2.add(pictureWrapper2);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, sku1List).toBuilder();
        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, list2, service, 123);
        assertEquals(sku1.getPicturesList().size(), 2);
    }

    @Test
    public void whenSomeDuplicateIsInFirstPictureThenShouldNotBeDuplicatedTest() {
        MboPictureService service = Mockito.mock(MboPictureService.class);
        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        List<PictureWrapper> sku1List = new ArrayList<>();
        List<PictureWrapper> list2 = new ArrayList<>();
        sku1List.add(pictureWrapper1);
        sku1List.add(pictureWrapper2);
        list2.add(pictureWrapper2);
        list2.add(pictureWrapper1);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, sku1List).toBuilder();
        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, list2, service, 123);
        System.out.println(sku1);
        assertEquals(2, sku1.getPicturesList().size());
    }

    @Test
    public void whenDuplicatesInOfferThenShouldBeDuplicatedTest() {
        MboPictureService service = Mockito.mock(MboPictureService.class);
        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url2", "",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        List<PictureWrapper> sku1List = new ArrayList<>();
        List<PictureWrapper> list2 = new ArrayList<>();
        sku1List.add(pictureWrapper1);
        list2.add(pictureWrapper2);
        list2.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, sku1List).toBuilder();
        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, list2, service, 456);
        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).hasSize(2);
        assertThat(resultUrls).containsExactlyInAnyOrder("url1", "url2");
    }

    @Test
    public void whenTryingToAddFirstSKUDeletedPicturesFromOfferDeny() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper skuPW1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED);
        PictureWrapper skuPW2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.APPROVED);
        PictureWrapper offerPW1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        PictureWrapper offerPW2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        PictureWrapper offerPW3 = buildPictureWrapper("url3", "md3",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        List<PictureWrapper> skuList = new ArrayList<>();
        List<PictureWrapper> offerList = new ArrayList<>();
        skuList.add(skuPW1);
        skuList.add(skuPW2);
        offerList.add(offerPW1);
        offerList.add(offerPW2);
        offerList.add(offerPW3);
        ModelStorage.Model.Builder sku = generateSkuWithPictures(5, 5, skuList).toBuilder();
        log.debug("sku pics before: {}", sku.getPicturesList());
        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku, offerList, service, 123);
        log.debug("sku pics after: {}", sku.getPicturesList());
        assertEquals(3, sku.getPicturesList().size());
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.DELETED).count(), 1);
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.APPROVED).count(), 2);
    }

    @Test
    public void whenTryingToAddNotFirstSKUDeletedPicturesFromOfferDeny() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper skuPW1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.APPROVED);
        PictureWrapper skuPW2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED);
        PictureWrapper offerPW1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.OPERATOR_FILLED);
        PictureWrapper offerPW2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.OPERATOR_FILLED);
        PictureWrapper offerPW3 = buildPictureWrapper("url3", "md3",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        List<PictureWrapper> skuList = new ArrayList<>();
        List<PictureWrapper> offerList = new ArrayList<>();
        skuList.add(skuPW1);
        skuList.add(skuPW2);
        offerList.add(offerPW1);
        offerList.add(offerPW2);
        offerList.add(offerPW3);
        ModelStorage.Model.Builder sku = generateSkuWithPictures(5, 5, skuList).toBuilder();
        log.debug("sku pics before: {}", sku.getPicturesList());
        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku, offerList, service, 123);
        log.debug("sku pics after: {}", sku.getPicturesList());
        assertEquals(sku.getPicturesList().size(), 3);
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.DELETED).count(), 1);
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.APPROVED).count(), 2);
        assertSame(sku.getPicturesList().stream()
                .filter(p -> "url2".equals(p.getUrlOrig()))
                .findFirst()
                .get().getValueSource(), ModelStorage.ModificationSource.VENDOR_OFFICE);
    }

    @Test
    public void whenTryingToAddNotFirstSKUDeletedPicturesFromOfferOtherSupplierDeny() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper skuPW1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.APPROVED);
        PictureWrapper skuPW2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED);
        PictureWrapper offerPW1 = buildPictureWrapper("url1", "md1",
                ModelStorage.ModificationSource.OPERATOR_FILLED);
        PictureWrapper offerPW2 = buildPictureWrapper("url2", "md2",
                ModelStorage.ModificationSource.OPERATOR_FILLED);
        PictureWrapper offerPW3 = buildPictureWrapper("url3", "md3",
                ModelStorage.ModificationSource.VENDOR_OFFICE);
        List<PictureWrapper> skuList = new ArrayList<>();
        List<PictureWrapper> offerList = new ArrayList<>();
        skuList.add(skuPW1);
        skuList.add(skuPW2);
        offerList.add(offerPW1);
        offerList.add(offerPW2);
        offerList.add(offerPW3);
        ModelStorage.Model.Builder sku = generateSkuWithPictures(5, 5, skuList).toBuilder();
        log.debug("sku pics before: {}", sku.getPicturesList());
        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku, offerList, service, 124);
        log.debug("sku pics after: {}", sku.getPicturesList());
        assertEquals(sku.getPicturesList().size(), 3);
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.DELETED).count(), 1);
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.APPROVED).count(), 1);
        assertEquals(sku.getPicturesList().stream()
                .filter(p -> p.getPictureStatus() == ModelStorage.PictureStatus.NEW).count(), 1);
    }

    @Test
    public void whenBestPictureThenBecomeFirst() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url2", "url1");
    }

    @Test
    public void whenWorstPictureThenBecomeLast() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 3, 3, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1", "url2");
    }

    @Test
    public void whenSkuOperatorPictureThenOperatorPictureRestFirst() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.OPERATOR_FILLED, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1", "url2");
    }

    @Test
    public void whenAddLessPicturesThenRemove() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper3);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url3", "url1");
    }

    @Test
    public void whenAddPicturesInOtherOrderThenReorder() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        skuList.add(pictureWrapper4);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(4, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url4", "url2", "url1", "url3");
    }

    @Test
    public void whenAddWorstPicturesThenReorderOfferPicturesAfterGood() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        // картинки из ску с размерами 0 считаются лучше картинок с размерами из оффера
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 0, 0, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 789L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url5", "md5", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper6 = buildPictureWrapper("url6", "md6", 789L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        skuList.add(pictureWrapper4);
        skuList.add(pictureWrapper5);
        skuList.add(pictureWrapper6);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper1);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(6, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url2", "url5", "url4", "url1", "url3", "url6");
    }

    @Test
    public void whenFirstSkuOperatorPictureThenOperatorPictureRestFirstAndOtherReorder() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.OPERATOR_FILLED, 2, 2, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", 789L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        // заполненные оператором, незвисимо от ownerId, должны оказаться рядом
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url5", "md5", 777L,
                ModelStorage.ModificationSource.OPERATOR_FILLED, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper6 = buildPictureWrapper("url6", "md6", 789L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        skuList.add(pictureWrapper4);
        skuList.add(pictureWrapper5);
        skuList.add(pictureWrapper6);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper3);
        listToAdd.add(pictureWrapper4);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(6, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1", "url5", "url3", "url4", "url2", "url6");
    }

    @Test
    public void whenNoPicturesInSkuThenAddFromOffer() {
        //в ску нет ничего => просто добавляем из оффера
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper1);
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1", "url2");
    }

    @Test
    public void whenAllSkuPicturesFromOfferOwnerThenReorder() {
        // в ску только поставщика оффера => все заменяются на картинк из оффера
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url3", "md3", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper3);
        listToAdd.add(pictureWrapper1);
        listToAdd.add(pictureWrapper4);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url3", "url1");
    }

    @Test
    public void whenNoNewToOtherSupplierPicturesThenNoChanges() {
        // в оффере нет ничего после фильтрации => нет изменений
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper3);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(2, sku1.getPicturesList().size());
        assertThat(sku1.getPicturesList()).containsExactly(pictureWrapper1.getPicture(), pictureWrapper2.getPicture());
    }

    @Test
    public void whenFirstPictureInOfferIsDupThenOfferPicturesBecomeSecond() {
        // Первая в офере дублирующая => первая каринка остается из ску
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", 789L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper6 = buildPictureWrapper("url5", "md5", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper5);
        listToAdd.add(pictureWrapper6);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(5, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1", "url3", "url4", "url5", "url2");
    }

    @Test
    public void whenSkuPicturesHasNoOwnerThenOneGroup() {
        // если не известно чья картинка (нет даже суплаера в ску) - все в одну группу
        // все картинки становятся этого владельца + APPROVED статус
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url5", "md5", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();
        sku1.clearSupplierId();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper5);
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(3, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url4", "url5", "url2");
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getOwnerId)).containsOnly(123L);
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getPictureStatus))
                .containsOnly(ModelStorage.PictureStatus.APPROVED);
    }

    @Test
    public void whenSkuPicturesHasNoOwnerAndSkuHasSupplierThenOneGroup() {
        // если не известно чья картинка но есть суплаер на ску = поставщику оффера - все в одну группу
        // все картинки становятся этого владельца + APPROVED статус
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url5", "md5", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 =
                generateSkuWithPicturesAndSupplier(5, 5, 123L, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper5);
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(3, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url4", "url5", "url2");
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getOwnerId)).containsOnly(123L);
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getPictureStatus))
                .containsOnly(ModelStorage.PictureStatus.APPROVED);
    }

    @Test
    public void whenSkuPicturesHasNoOwnerAndHasOtherSuppliersThenTheyMakeGroup() {
        // если не известно чья картинка (нет даже суплаера в ску) и есть картинки другого поставщика
        // картинки неизвестного поставщика заменяются на картинки оффера, статус старых картинок не меняется
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED,
                5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED,
                5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url5", "md5", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict(), 1);

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 = generateSkuWithPictures(5, 5, skuList).toBuilder();
        sku1.clearSupplierId();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper5);
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(4, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url4", "url5", "url2", "url3");
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getOwnerId))
                .containsExactly(123L, 123L, 123L, 456L);
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getPictureStatus))
                .containsExactly(
                        ModelStorage.PictureStatus.NEW, ModelStorage.PictureStatus.NEW,
                        ModelStorage.PictureStatus.DELETED, ModelStorage.PictureStatus.DELETED
                );
    }

    @Test
    public void whenSkuPicturesHasNoOwnerAndSkuHasSupplierAndHasOtherSuppliersThenTheyMakeGroup() {
        // если не известно чья картинка но есть суплаер на ску = поставщику оффера и есть картинки другого поставщика
        // картинки неизвестного поставщика заменяются на картинки оффера, статус старых картинок не меняется
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper pictureWrapper1 = buildPictureWrapper("url1", "md1", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper2 = buildPictureWrapper("url2", "md2", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED,
                5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper3 = buildPictureWrapper("url3", "md3", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, ModelStorage.PictureStatus.DELETED,
                5, 5, true, buildVerdict());
        PictureWrapper pictureWrapper4 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());
        PictureWrapper pictureWrapper5 = buildPictureWrapper("url5", "md5", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict());

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(pictureWrapper1);
        skuList.add(pictureWrapper2);
        skuList.add(pictureWrapper3);
        ModelStorage.Model.Builder sku1 =
                generateSkuWithPicturesAndSupplier(5, 5, 123L, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(pictureWrapper4);
        listToAdd.add(pictureWrapper5);
        listToAdd.add(pictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku1, listToAdd, service, 123);

        assertEquals(4, sku1.getPicturesList().size());
        List<String> resultUrls =
                sku1.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url4", "url5", "url2", "url3");
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getOwnerId))
                .containsExactly(123L, 123L, 123L, 456L);
        assertThat(sku1.getPicturesList().stream().map(ModelStorage.Picture::getPictureStatus))
                .containsExactly(
                        ModelStorage.PictureStatus.NEW, ModelStorage.PictureStatus.NEW,
                        ModelStorage.PictureStatus.DELETED, ModelStorage.PictureStatus.DELETED
                );
    }

    @Test
    public void whenInvalidFirstPictureSingleSupplierThenFirstPictureInSkuNotModified() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper skuPictureWrapper1 = buildPictureWrapper("url1", "md1", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper skuPictureWrapper2= buildPictureWrapper("url2", "md2", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper offerPictureWrapper1 = buildPictureWrapper("url3", "md3", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict(), 1);
        PictureWrapper offerPictureWrapper2 = buildPictureWrapper("url4", "md4", null,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict(), 2);

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(skuPictureWrapper1);
        skuList.add(skuPictureWrapper2);
        var sku = generateSkuWithPicturesAndSupplier(5, 5, 123L, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(offerPictureWrapper1);
        listToAdd.add(offerPictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku, listToAdd, service, 123);

        assertEquals(3, sku.getPicturesList().size());
        List<String> resultUrls =
                sku.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1", "url3", "url4");
        assertThat(sku.getPicturesList().stream().map(ModelStorage.Picture::getOwnerId))
                .containsExactly(123L, 123L, 123L);
    }

    @Test
    public void whenInvalidFirstPictureMultipleSuppliersThenFirstPictureInSkuNotModified() {
        MboPictureService service = Mockito.mock(MboPictureService.class);

        doReturn(buildVerdict()).when(service).getCWImageResult(any());
        PictureWrapper skuPictureWrapper1 = buildPictureWrapper("url1_1", "md1_1", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper skuPictureWrapper2 = buildPictureWrapper("url1_2", "md1_2", 123L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper skuPictureWrapper3 = buildPictureWrapper("url2_1", "md2_2", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 5, 5, true, buildVerdict());
        PictureWrapper offerPictureWrapper1 = buildPictureWrapper("url3", "md3", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict(), 1);
        PictureWrapper offerPictureWrapper2 = buildPictureWrapper("url4", "md4", 456L,
                ModelStorage.ModificationSource.VENDOR_OFFICE, 10, 10, true, buildVerdict(), 2);

        List<PictureWrapper> skuList = new ArrayList<>();
        skuList.add(skuPictureWrapper1);
        skuList.add(skuPictureWrapper2);
        skuList.add(skuPictureWrapper3);
        var sku = generateSkuWithPicturesAndSupplier(5, 5, 123L, skuList).toBuilder();

        List<PictureWrapper> listToAdd = new ArrayList<>();
        listToAdd.add(offerPictureWrapper1);
        listToAdd.add(offerPictureWrapper2);

        ModelStoragePictureUtils.updateWithTheBestFirstSkuPictures(sku, listToAdd, service, 456);

        assertEquals(4, sku.getPicturesList().size());
        List<String> resultUrls =
                sku.getPicturesList().stream().map(ModelStorage.Picture::getUrlOrig).collect(Collectors.toList());
        assertThat(resultUrls).containsExactly("url1_1", "url1_2", "url3", "url4");
        assertThat(sku.getPicturesList().stream().map(ModelStorage.Picture::getOwnerId))
                .containsExactly(123L, 123L, 456L, 456L);
    }


    private CWImageResult buildVerdict() {
        return new CWImageResult(Collections.singletonList(
                new CWRawVerdict(
                        "some_key",
                        "watermark_clean",
                        "true",
                        "some_source",
                        "watermark_image_toloka",
                        "image"
                )
        ));
    }
}
