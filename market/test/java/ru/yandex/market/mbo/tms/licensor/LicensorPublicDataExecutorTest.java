package ru.yandex.market.mbo.tms.licensor;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.IdAndName;
import ru.yandex.market.mbo.licensor2.LicensorServiceImpl;
import ru.yandex.market.mbo.licensor2.LicensorServiceMockBuilder;
import ru.yandex.market.mbo.licensor2.name.NameLicensorServiceMock;
import ru.yandex.market.mbo.licensor2.scheme.LicensorCase;
import ru.yandex.market.mbo.s3.AmazonS3Mock;
import ru.yandex.market.mbo.utils.XlsxDate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * @author ayratgdl
 * @since 01.10.18
 */
public class LicensorPublicDataExecutorTest {
    private static final IdAndName LICENSOR_1 = new IdAndName(101, "Лицензиар 101");
    private static final IdAndName LICENSOR_2 = new IdAndName(102, "Лицензиар 102");
    private static final IdAndName FRANCHISE_1 = new IdAndName(201, "Франшиза 201");
    private static final IdAndName FRANCHISE_2 = new IdAndName(202, "Франшиза 202");
    private static final IdAndName FRANCHISE_3 = new IdAndName(203, "Франшиза 203");
    private static final IdAndName PERSONAGE_1 = new IdAndName(301, "Персонаж 301");
    private static final IdAndName PERSONAGE_2 = new IdAndName(302, "Персонаж 302");
    private static final IdAndName PERSONAGE_3 = new IdAndName(303, "Персонаж 303");
    private static final IdAndName PERSONAGE_4 = new IdAndName(304, "Персонаж 304");

    private LicensorPublicDataExecutor executor;
    private LicensorServiceImpl licensorService;
    private NameLicensorServiceMock nameLicensorService;
    private AmazonS3Mock s3Client;
    private Clock clock;

    @Before
    public void setUp() {
        executor = new LicensorPublicDataExecutor();

        licensorService = new LicensorServiceMockBuilder().build().getLicensorService();
        nameLicensorService = new NameLicensorServiceMock();
        LicensorLoader licensorLoader = new LicensorLoader();
        licensorLoader.setLicensorService(licensorService);
        licensorLoader.setNameLicensorService(nameLicensorService);
        executor.setLicensorLoader(licensorLoader);

        s3Client = new AmazonS3Mock();
        executor.setS3Client(s3Client);

        clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        executor.setClock(clock);
    }

    @Test
    public void exportLicensors() throws Exception {
        nameLicensorService.addLicensors(LICENSOR_1);
        nameLicensorService.addLicensors(LICENSOR_2);
        nameLicensorService.addFranchises(FRANCHISE_1);
        nameLicensorService.addFranchises(FRANCHISE_2);
        nameLicensorService.addFranchises(FRANCHISE_3);
        nameLicensorService.addPersonages(PERSONAGE_1);
        nameLicensorService.addPersonages(PERSONAGE_2);
        nameLicensorService.addPersonages(PERSONAGE_3);
        nameLicensorService.addPersonages(PERSONAGE_4);

        licensorService.createLicensorCase(new LicensorCase(null, FRANCHISE_3.getId(), PERSONAGE_4.getId()));
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_2.getId(), PERSONAGE_2.getId())
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_2.getId(), PERSONAGE_1.getId())
        );
        licensorService.createLicensorCase(
            new LicensorCase(LICENSOR_1.getId(), FRANCHISE_1.getId(), PERSONAGE_1.getId())
        );

        executor.doRealJob(null);

        XlsxDate expected = new XlsxDate(
            "Дата: " + getDate() + "\n" +
            "Лицензиар, Франшиза, Персонаж\n" +
            LICENSOR_1.getName() + "," + FRANCHISE_1.getName() + "," + PERSONAGE_1.getName() + "\n" +
                LICENSOR_1.getName() + "," + FRANCHISE_2.getName() + "," + PERSONAGE_1.getName() + "\n" +
                LICENSOR_1.getName() + "," + FRANCHISE_2.getName() + "," + PERSONAGE_2.getName() + "\n" +
                LICENSOR_2.getName() + ",,\n" +
                "," + FRANCHISE_3.getName() + "," + PERSONAGE_4.getName() + "\n" +
                ",," + PERSONAGE_3.getName()
        );
        XlsxDate actual = new XlsxDate(readActualXlsx());
        Assert.assertEquals(expected, actual);
    }

    private Workbook readActualXlsx() {
        try {
            S3Object s3Object = s3Client.getObject(LicensorPublicDataExecutor.BUCKET, LicensorPublicDataExecutor.KEY);
            S3ObjectInputStream input = s3Object.getObjectContent();
            return WorkbookFactory.create(input);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getDate() {
        return LocalDateTime.now(clock).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }
}
