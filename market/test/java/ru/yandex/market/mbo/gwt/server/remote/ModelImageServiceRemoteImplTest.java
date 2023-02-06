package ru.yandex.market.mbo.gwt.server.remote;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.params.guru.GuruService;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.modelstorage.ModelPictureInfo;
import ru.yandex.market.mbo.gwt.models.modelstorage.Picture;
import ru.yandex.market.mbo.gwt.utils.XslNames;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getGuruBuilder;
import static ru.yandex.market.mbo.db.modelstorage.validation.SkuBuilderHelper.getSkuBuilder;
import static ru.yandex.market.mbo.image.ModelPictureInfoUtils.modelPictureInfo;
import static ru.yandex.market.mbo.image.ModelPictureInfoUtils.picture;

/**
 * @author danfertev
 * @since 04.07.2018
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class ModelImageServiceRemoteImplTest {
    private ModelImageServiceRemoteImpl modelImageService;

    private Picture xlPicture;
    private Picture xl2Picture;
    private Picture xl3Picture;
    private Picture xl4Picture;
    private Picture xl5Picture;
    private Picture xl6Picture;

    private CommonModel parent;
    private CommonModel modification1;
    private CommonModel modification2;

    private CommonModel sku11;
    private CommonModel sku12;
    private CommonModel sku21;
    private CommonModel sku22;

    @Before
    public void setUp() throws Exception {
        modelImageService = new ModelImageServiceRemoteImpl();
        GuruService guruService = mock(GuruService.class);
        ModelStorageService modelStorageService = mock(ModelStorageService.class);

        modelImageService.setModelStorageService(modelStorageService);
        modelImageService.setGuruService(guruService);

        xlPicture = picture(XslNames.XL_PICTURE, "xl_url", 101, 101, "xl_source", "xl_orig");
        xl2Picture = picture("XL-Picture_2", "xl2_url", 102, 102, "xl2_source", "xl2_orig");
        xl3Picture = picture("XL-Picture_3", "xl3_url", 103, 103, "xl3_source", "xl3_orig");
        xl4Picture = picture("XL-Picture_4", "xl4_url", 104, 104, "xl4_source", "xl4_orig");
        xl5Picture = picture("XL-Picture_5", "xl5_url", 105, 105, "xl5_source", "xl5_orig");
        xl6Picture = picture("XL-Picture_6", "xl6_url", 106, 106, "xl6_source", "xl6_orig");

        parent = getGuruBuilder().id(1).endModel();
        modification1 = getGuruBuilder().id(11).parentModel(parent).endModel();
        modification2 = getGuruBuilder().id(12).parentModel(parent).endModel();

        sku11 = getSkuBuilder(11).id(111).endModel();
        sku12 = getSkuBuilder(11).id(112).endModel();
        sku21 = getSkuBuilder(12).id(121).endModel();
        sku22 = getSkuBuilder(12).id(122).endModel();

        when(modelStorageService.getModel(anyLong(), anyLong()))
            .thenReturn(Optional.of(parent));
        when(modelStorageService.getModifications(anyLong()))
            .thenReturn(Arrays.asList(modification1, modification2));
        when(modelStorageService.getModels(anyLong(), anyList()))
            .thenReturn(Arrays.asList(sku11, sku12, sku21, sku22));
    }

    @Test
    public void testNoPicture() {
        List<ModelPictureInfo> infos = modelImageService.getAllChildrenModelPictureInfo(
            parent.getCategoryId(), parent.getId());

        assertThat(infos).isEmpty();
    }

    @Test
    public void testNoDuplicatePictures() {
        modification1.addPicture(xlPicture);
        modification2.addPicture(xl2Picture);
        sku11.addPicture(xl3Picture);
        sku12.addPicture(xl4Picture);
        sku21.addPicture(xl5Picture);
        sku22.addPicture(xl6Picture);

        List<ModelPictureInfo> infos = modelImageService.getAllChildrenModelPictureInfo(
            parent.getCategoryId(), parent.getId());

        assertThat(infos).containsExactlyInAnyOrder(
            modelPictureInfo(xlPicture, modification1),
            modelPictureInfo(xl2Picture, modification2),
            modelPictureInfo(xl3Picture, sku11),
            modelPictureInfo(xl4Picture, sku12),
            modelPictureInfo(xl5Picture, sku21),
            modelPictureInfo(xl6Picture, sku22)
        );
    }

    @Test
    public void testReturnOnlyUniquePictures() {
        parent.addPicture(xlPicture);
        parent.addPicture(xlPicture);
        modification1.addPicture(xlPicture);
        modification2.addPicture(xl2Picture);
        modification2.addPicture(xl3Picture);
        sku11.addPicture(xl3Picture);
        sku11.addPicture(xl6Picture);
        sku12.addPicture(xl4Picture);
        sku12.addPicture(xlPicture);
        sku21.addPicture(xl5Picture);
        sku21.addPicture(xlPicture);
        sku22.addPicture(xl6Picture);
        sku22.addPicture(xl4Picture);

        List<ModelPictureInfo> infos = modelImageService.getAllChildrenModelPictureInfo(
            parent.getCategoryId(), parent.getId());

        assertThat(infos).containsExactlyInAnyOrder(
            modelPictureInfo(xlPicture, parent, modification1, sku12, sku21),
            modelPictureInfo(xl2Picture, modification2),
            modelPictureInfo(xl3Picture, modification2, sku11),
            modelPictureInfo(xl4Picture, sku12, sku22),
            modelPictureInfo(xl5Picture, sku21),
            modelPictureInfo(xl6Picture, sku11, sku22)
        );
    }
}
