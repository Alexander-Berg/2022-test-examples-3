package ru.yandex.market.abo.core.outlet.license;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.market.abo.core.outlet.repo.OutletLicenseInfoRepo;
import ru.yandex.market.core.outlet.OutletLicenseType;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author komarovns
 * @date 04.06.19
 */
class AlcoLicenseDownloadManagerTest extends EmptyTest {
    private static final String XML_PATH = "/outlet/license/fsrar_test.xml";
    @Autowired
    private AlcoLicenseDownloadManager alcoLicenseDownloadManager;
    @Autowired
    private OutletLicenseInfoRepo outletLicenseInfoRepo;

    @Test
    @Disabled
    void downloadReal() throws Exception {
        alcoLicenseDownloadManager.syncLicenses();
    }

    @Test
    void testSyncLicenses() {
        var zip = prepareZip();
        alcoLicenseDownloadManager.parseLicensesArchive(zip);

        var licenses = outletLicenseInfoRepo.findAll();
        assertEquals(1, licenses.size());

        var licenseInfo = licenses.get(0);
        assertEquals(OutletLicenseType.ALCOHOL, licenseInfo.getType());
        assertEquals("some number", licenseInfo.getNumber());
        assertEquals(LocalDate.of(2011, 11, 29), licenseInfo.getIssueDate());
        assertEquals(LocalDate.of(2015, 6, 26), licenseInfo.getExpiryDate());
        assertEquals("0123456789", licenseInfo.getInn());
        assertEquals("Общество с ограниченной ответственностью \"Рога и копыта\" Сокращенно: ООО \"РОГА И КОПЫТА\"", licenseInfo.getOrganizationName());
        assertEquals("juridical address", licenseInfo.getJuridicalAddress());
        assertEquals("fact address", licenseInfo.getFactAddress());
    }

    private File prepareZip() {
        var zip = new File("licenses.zip");
        try (var is = EmptyTest.class.getResourceAsStream(XML_PATH);
             var fos = new FileOutputStream(zip);
             var zos = new ZipOutputStream(fos)) {
            zos.putNextEntry(new ZipEntry("licenses.xml"));
            IOUtils.copy(is, zos);
            zos.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return zip;
    }
}
