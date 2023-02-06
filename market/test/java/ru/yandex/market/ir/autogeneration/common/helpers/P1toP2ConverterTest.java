package ru.yandex.market.ir.autogeneration.common.helpers;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import ru.yandex.market.mbo.http.ModelStorage;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class P1toP2ConverterTest {
    private final ModelStorageHelper mockModelStorageHelper = mock(ModelStorageHelper.class);
    private final P1toP2Converter converter = new P1toP2Converter(mockModelStorageHelper);

    //Pictures
    private static final ModelStorage.Picture PIC1 = ModelStorage.Picture.newBuilder()
        .setOrigMd5("pic1").setModificationDate(1L).build();
    private static final ModelStorage.Picture PIC2 = ModelStorage.Picture.newBuilder()
        .setOrigMd5("pic2").setModificationDate(1L).build();
    private static final ModelStorage.Picture PIC3 = ModelStorage.Picture.newBuilder()
        .setOrigMd5("pic1").setModificationDate(2L).build();
    private static final ModelStorage.Picture PIC4 = ModelStorage.Picture.newBuilder()
        .setOrigMd5("pic4").setModificationDate(1L).build();
    private static final ModelStorage.Picture PIC5 = ModelStorage.Picture.newBuilder()
        .setOrigMd5("pic4").setModificationDate(0L).build();
    private static final ModelStorage.Picture PIC6 = ModelStorage.Picture.newBuilder()
        .setOrigMd5("pic6").setModificationDate(1L).build();

    @Test
    public void testWhenDuplicatePicturesUniqueAndOrderedAreReturned() {
        ModelStorage.Model pSku = ModelStorage.Model.newBuilder()
            .addAllPictures(ImmutableList.of(PIC6, PIC2, PIC3, PIC4, PIC5, PIC1)).build();
        assertThat(converter.getPicturesWithoutDuplicates(pSku)).containsExactly(PIC6, PIC2, PIC3, PIC4);
    }
}
