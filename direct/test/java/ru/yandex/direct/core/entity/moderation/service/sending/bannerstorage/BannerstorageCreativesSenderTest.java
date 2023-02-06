package ru.yandex.direct.core.entity.moderation.service.sending.bannerstorage;

import org.junit.Test;

import ru.yandex.direct.bannerstorage.client.model.Creative;
import ru.yandex.direct.bannerstorage.client.model.CreativeLayoutCode;
import ru.yandex.direct.core.entity.moderation.model.bannerstorage.SmartCreativeType;

import static org.junit.Assert.assertEquals;
import static ru.yandex.direct.bannerstorage.client.BannerStorageClient.SMART_TGO_LAYOUT_ID;
import static ru.yandex.direct.core.entity.moderation.service.sending.bannerstorage.BannerstorageCreativesSender.getDisplayHeight;
import static ru.yandex.direct.core.entity.moderation.service.sending.bannerstorage.BannerstorageCreativesSender.getDisplayWidth;
import static ru.yandex.direct.core.entity.moderation.service.sending.bannerstorage.BannerstorageCreativesSender.getSmartCreativeType;

public class BannerstorageCreativesSenderTest {
    @Test
    public void getSmartCreativeTypeTest() {
        assertEquals(SmartCreativeType.TGO, getSmartCreativeType(
                new Creative()
                        .withLayoutCode(new CreativeLayoutCode()
                                .withLayoutId(SMART_TGO_LAYOUT_ID))));

        assertEquals(SmartCreativeType.ADAPTIVE, getSmartCreativeType(
                new Creative()
                        .withWidth(0)
                        .withHeight(0)
                        .withLayoutCode(new CreativeLayoutCode()
                                .withLayoutId(SMART_TGO_LAYOUT_ID + 1))));

        assertEquals(SmartCreativeType.STANDARD, getSmartCreativeType(
                new Creative()
                        .withWidth(100)
                        .withHeight(200)
                        .withLayoutCode(new CreativeLayoutCode()
                                .withLayoutId(SMART_TGO_LAYOUT_ID + 1))));
    }

    @Test
    public void getSmartCreativeWidthHeightTest_SmartTile() {
        Creative creative = new Creative()
                .withLayoutCode(new CreativeLayoutCode().withLayoutId(47))
                .withWidth(0)
                .withHeight(0);
        assertEquals(160, getDisplayWidth(creative));
        assertEquals(600, getDisplayHeight(creative));
    }

    @Test
    public void getSmartCreativeWidthHeightTest_UnknownAdaptive() {
        Creative creative = new Creative()
                .withLayoutCode(new CreativeLayoutCode().withLayoutId(127))
                .withWidth(0)
                .withHeight(0);
        assertEquals(0, getDisplayWidth(creative));
        assertEquals(0, getDisplayHeight(creative));
    }
}
