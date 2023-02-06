package ru.yandex.market.mbo.mdm.common.service.bmdm.converter.util;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mdm.http.MdmBase;

public class BmdmPathKeeperTest {
    @Test
    public void testGetPathAfterMultipleAdditionsAndRemoves() {
        BmdmPathKeeper bmdmPathKeeper = new BmdmPathKeeper();
        bmdmPathKeeper.addBmdmEntity(12); // E12
        bmdmPathKeeper.addBmdmAttribute(13); // E12 -> A13
        bmdmPathKeeper.removeLastSegment(); // E12
        bmdmPathKeeper.addBmdmAttribute(14); // E12 -> A14
        bmdmPathKeeper.addBmdmEntity(15); // E12 -> A14 -> E15
        bmdmPathKeeper.addBmdmAttribute(16); // E12 -> A14 -> E15 -> A16
        bmdmPathKeeper.removeLastSegment(); // E12 -> A14 -> E15
        bmdmPathKeeper.addBmdmAttribute(17); // E12 -> A14 -> E15 -> A17
        bmdmPathKeeper.addPathSegment(1, MdmBase.MdmMetaType.MDM_BOOL); // E12 -> A14 -> E15 -> A17 -> B1

        Assertions.assertThat(bmdmPathKeeper.getPath())
            .isEqualTo(MdmBase.MdmPath.newBuilder()
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setMdmId(12)
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setMdmId(14)
                    .setType(MdmBase.MdmMetaType.MDM_ATTR))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setMdmId(15)
                    .setType(MdmBase.MdmMetaType.MDM_ENTITY_TYPE))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setMdmId(17)
                    .setType(MdmBase.MdmMetaType.MDM_ATTR))
                .addSegments(MdmBase.MdmPath.MdmPathSegment.newBuilder()
                    .setMdmId(1)
                    .setType(MdmBase.MdmMetaType.MDM_BOOL))
                .build());
    }

    @Test
    public void whenRemoveFromEmptyPathShouldThrowException() {
        BmdmPathKeeper pathKeeper = new BmdmPathKeeper();
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(pathKeeper::removeLastSegment)
            .withMessage("Trying to remove last segment from empty path.");
    }

    @Test
    public void whenDepthLimitIsReachedShouldThrowException() {
        BmdmPathKeeper pathKeeper = new BmdmPathKeeper(10);

        //длина пути 10 разрешена
        for (int i = 1; i <= 10; i++) {
            pathKeeper.addPathSegment(i);
        }

        //А вот 11 уже нет
        Assertions.assertThatExceptionOfType(IllegalStateException.class)
            .isThrownBy(() -> pathKeeper.addPathSegment(11))
            .withMessageStartingWith("Out of depth limit. Depth limit: 10. Path length: 11.");
    }
}
