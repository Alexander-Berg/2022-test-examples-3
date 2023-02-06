package ru.yandex.market.partner.promo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.logbroker.event.datacamp.DatacampMessageLogbrokerEvent;
import ru.yandex.market.core.supplier.promo.model.validation.PromoOfferValidationStats;
import ru.yandex.market.core.supplier.promo.service.DatacampCommitPromoOffersService;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.partner.test.context.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class DatacampCommitPromoOffersServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private DatacampCommitPromoOffersService datacampCommitPromoOffersService;
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private AmazonS3 amazonS3;
    @Autowired
    @Qualifier("promoOfferLogbrokerService")
    private LogbrokerService promoOfferLogbrokerService;

    private final S3Object s3Object = mock(S3Object.class);

    @Test
    @DbUnitDataSet(
            before = "promo-offer-commit.before.csv",
            after = "promo-offer-commit.after.csv")
    public void testCommit() throws IOException {
        String bucketName = "bucket";
        String keyWithPrefix = "eligible_s3_key";
        String validationId = "id";
        String validatedUploadUrl = "http://test.url/file-uploaded-with_results.xslm";
        String promoId = "#1";

        // скачивание файла
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucketName, keyWithPrefix));
        when(amazonS3.getObject(bucketName, keyWithPrefix))
                .thenReturn(s3Object);
        File file = new File(getClass()
                .getResource("fileForParted.pbsn").getPath());
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        when(s3Object.getObjectContent()).thenReturn(new S3ObjectInputStream(inputStream, null));

        // загрузка файла
        when(resourceLocationFactory.createLocation(anyString()))
                .thenReturn(ResourceLocation.create(bucketName, keyWithPrefix));
        when(amazonS3.getUrl(bucketName, keyWithPrefix)).thenReturn(new URL(validatedUploadUrl));

        final PromoOfferValidationStats stats = new PromoOfferValidationStats.Builder()
                .withPromoId(promoId)
                .withValidationId(validationId)
                .withEligibleS3Key(keyWithPrefix)
                .build();

        datacampCommitPromoOffersService.commitOffersToDC(stats);

        verify(promoOfferLogbrokerService, times(1))
                .publishEvent(any(DatacampMessageLogbrokerEvent.class));

        inputStream.close();
    }
}
