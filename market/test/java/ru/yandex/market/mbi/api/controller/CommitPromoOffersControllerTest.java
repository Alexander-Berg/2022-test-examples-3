package ru.yandex.market.mbi.api.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampOffer;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;
import ru.yandex.market.common.mds.s3.client.service.factory.ResourceLocationFactory;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.logbroker.event.datacamp.DatacampMessageLogbrokerEvent;
import ru.yandex.market.core.promo.CommitPromoOffersResponse;
import ru.yandex.market.core.promo.CommitPromoOffersResult;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.api.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "CommitPromoOffersControllerTest.commit.before.csv")
public class CommitPromoOffersControllerTest extends FunctionalTest {
    @Autowired
    private ResourceLocationFactory resourceLocationFactory;
    @Autowired
    private AmazonS3 amazonS3;
    @Autowired
    @Qualifier("promoOfferLogbrokerService")
    private LogbrokerService promoOfferLogbrokerService;

    private final S3Object s3Object = mock(S3Object.class);

    @DisplayName("Успешное подтверждение участия партнера в акции.")
    @Test
    @DbUnitDataSet(before = "promoCommitSuccess.before.csv")
    void commitSuccessTest() throws MalformedURLException {
        mockMds();

        CommitPromoOffersResponse isCommitted = mbiApiClient.commitPromoOffers("validationId");
        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(promoOfferLogbrokerService, times(1)).publishEvent(eventCaptor.capture());
        DatacampMessageLogbrokerEvent event = eventCaptor.getValue();
        DatacampMessageOuterClass.DatacampMessage msg = event.getPayload();
        List<DataCampOffer.Offer> offers = msg.getOffersList().get(0).getOfferList();
        Assertions.assertThat(offers).hasSize(2);
        Assertions.assertThat(offers).map(offer -> offer.getPromos().getAnaplanPromos().getAllPromos()
                .getPromos(0).getId()).containsExactly("#1234", "#1234");
        Assertions.assertThat(isCommitted.getCommitPromoOffersResult()).isEqualTo(CommitPromoOffersResult.COMMITTED);
    }

    @DisplayName("Неуспешное подтверждение участия партнера в акции.")
    @Test
    @DbUnitDataSet(before = "promoCommitFailed.before.csv")
    void commitFailedTest() throws MalformedURLException {
        mockMds();

        CommitPromoOffersResponse isCommitted = mbiApiClient.commitPromoOffers("validationId");
        ArgumentCaptor<DatacampMessageLogbrokerEvent> eventCaptor =
                ArgumentCaptor.forClass(DatacampMessageLogbrokerEvent.class);
        verify(promoOfferLogbrokerService, times(0)).publishEvent(eventCaptor.capture());
        Assertions.assertThat(isCommitted.getCommitPromoOffersResult()).isEqualTo(CommitPromoOffersResult.NOT_COMPLETED);
    }

    void mockMds() throws MalformedURLException {
        String bucketName = "bucket";
        String keyWithPrefix = "eligible_s3_key";
        String validatedUploadUrl = "http://test.url/file-uploaded-with_results.xslm";

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
    }
}
