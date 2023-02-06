package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;

import static org.junit.Assert.assertEquals;

public class DjfsResourcePathTest {

    private static final DjfsUid UID = DjfsUid.cons(1L);

    @Test
    public void testUsualFileName() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/Документы/Табеля/file.xlsx");
        assertEquals("xlsx", path.getExtensionByMpfsWay());
    }

    @Test
    public void testDotStartingFileName() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/Документы/Табеля/.xlsx");
        assertEquals("", path.getExtensionByMpfsWay());
    }

    @Test
    public void testDotStartingFileNameWithExtension() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/Документы/Табеля/.odt.odt");
        assertEquals("odt", path.getExtensionByMpfsWay());
    }

    @Test
    public void testManyDotsStartingFileName() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/Документы/Табеля/..xlsx");
        assertEquals("", path.getExtensionByMpfsWay());
        DjfsResourcePath threeDotsPath = DjfsResourcePath.cons(UID, "/disk/Документы/Табеля/...xlsx");
        assertEquals("", threeDotsPath.getExtensionByMpfsWay());
    }

    @Test
    public void testManyDotsExtension() {
        DjfsResourcePath path = DjfsResourcePath.cons(UID, "/disk/Документы/Табеля/file...xlsx");
        assertEquals("xlsx", path.getExtensionByMpfsWay());
    }
}
