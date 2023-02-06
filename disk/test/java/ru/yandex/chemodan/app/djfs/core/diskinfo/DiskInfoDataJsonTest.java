package ru.yandex.chemodan.app.djfs.core.diskinfo;

import org.junit.Test;

import ru.yandex.bolts.collection.Option;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DiskInfoDataJsonTest {

    @Test
    public void testJsonParsing() {
        DiskInfoData diskInfoData = PgDiskInfoDao.getDiskInfoDataFromDbString("{" +
                "   \"geo_albums_generation_in_progress\":true" +
                "}");
        assertNotNull(diskInfoData);
        assertTrue(diskInfoData.isJsonData());
        assertEquals(true, diskInfoData.getJsonData().getGeoAlbumsGenerationInProgress().get());
    }

    @Test
    public void testLongParsing() {
        DiskInfoData diskInfoData = PgDiskInfoDao.getDiskInfoDataFromDbString("123");
        assertNotNull(diskInfoData);
        assertTrue(diskInfoData.isLongData());
        assertEquals(123L, diskInfoData.getLongValue().longValue());
    }

    @Test
    public void testRawStringParsing() {
        DiskInfoData diskInfoData = PgDiskInfoDao.getDiskInfoDataFromDbString("[[\"market\", \"RU\"]]");
        assertNotNull(diskInfoData);
        assertTrue(diskInfoData.isRawData());
        assertEquals("[[\"market\", \"RU\"]]", diskInfoData.getRawData());
    }

    @Test
    public void testJsonSerialization() {
        String result = PgDiskInfoDao.serializeDiskInfoDataToDbString(DiskInfoData.jsonData(new DiskInfoData.JsonData(Option.of(true))));
        assertEquals("{\"geo_albums_generation_in_progress\":true}", result);
    }

    @Test
    public void testLongSerialization() {
        String result = PgDiskInfoDao.serializeDiskInfoDataToDbString(DiskInfoData.longData(123L));
        assertEquals("123", result);
    }

    @Test
    public void testRawStringSerialization() {
        String result = PgDiskInfoDao.serializeDiskInfoDataToDbString(DiskInfoData.rawData("[[\"market\", \"RU\"]]"));
        assertEquals("[[\"market\", \"RU\"]]", result);
    }
}
