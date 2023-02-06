package ru.yandex.market.gutgin.tms.pipeline.dcp.taskaction.databucket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferPictures;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.gutgin.tms.config.TestConfig;
import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations.DcpImageValidation;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.mbo.http.ModelStorageServiceStub;
import ru.yandex.market.partner.content.common.db.dao.dcp.DcpPartnerPictureDao;
import ru.yandex.market.partner.content.common.db.dao.dcp.FakeDatacampOfferDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;

@Ignore
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TestConfig.class)
public class UploadImagesTaskActionRunner {
    private static final String MODEL_STORAGE_URL = "http://ag-mbo-card-api.tst.vs.market.yandex.net/modelStorage/";

    @Autowired
    private DcpPartnerPictureDao dcpPartnerPictureDao;

    @Test
    public void testBasic() {
        ModelStorageServiceStub modelStorageServiceStub = new ModelStorageServiceStub();
        modelStorageServiceStub.setHost(MODEL_STORAGE_URL);

        DcpImageValidation.CategoryAccess categoryAccess = new DcpImageValidation.CategoryAccess() {
            @Override
            public boolean isWhiteBackgroundMandatory(long categoryId) {
                return true;
            }
        };

        AvatarImageDownloader avatarImageDownloader = new AvatarImageDownloader(1, 0);
        ModelStorageHelper modelStorageHelper = new ModelStorageHelper(modelStorageServiceStub, modelStorageServiceStub);
        DcpPicturesFromModelCollector dcpPicturesFromModelCollector = new DcpPicturesFromModelCollector(modelStorageHelper);
        Executor immediateThreadExecutor = Runnable::run;
        ImageUploader uploadImagesTaskAction = new ImageUploader(2, avatarImageDownloader,
                modelStorageServiceStub, immediateThreadExecutor, dcpPartnerPictureDao, dcpPicturesFromModelCollector);

        BiFunction<Long, List<String>, GcSkuTicket> ticketCreator = (id, urls) -> {
            GcSkuTicket gcSkuTicket = new GcSkuTicket();
            gcSkuTicket.setId(17L);

            DataCampOffer.Offer.Builder offerBuilder = DataCampOffer.Offer.newBuilder();
            DataCampOfferPictures.MarketPictures.Builder marketBuilder = offerBuilder.getPicturesBuilder().getMarketBuilder();
            urls.forEach(url -> marketBuilder.addProductPicturesBuilder().getOriginalBuilder().setUrl(url));

            gcSkuTicket.setDatacampOffer(offerBuilder.build());
            return gcSkuTicket;
        };

        List<String> urls = Arrays.asList(
                "https://i.ytimg.com/vi/ESBo_y01QLA/maxresdefault.jpg",
                "https://avatars.mds.yandex.net/get-banana/64225/x25SP05kOxAjUgfyn3olkxz8_banana_20161021_scale_tiz_1_120x90.png/orig",
                //"https://peter.rybin.spb.ru/90.jpg",
                "//avatars.mds.yandex.net/get-mpic/1605421/img_id1032322867845600247.jpeg/orig");

        GcSkuTicket ticket1 = ticketCreator.apply(19L, urls.subList(0, 2));
        GcSkuTicket ticket2 = ticketCreator.apply(17L, urls);

        ArrayList<String> problems1 = new ArrayList<>();
        uploadImagesTaskAction.uploadForAllTickets(Arrays.asList(ticket1), problems1);
        ArrayList<String> problems2 = new ArrayList<>();
        uploadImagesTaskAction.uploadForAllTickets(Arrays.asList(ticket1, ticket2), problems2);

        FakeDatacampOfferDao fakeDatacampOfferDaoMock = Mockito.mock(FakeDatacampOfferDao.class);
        Mockito.when(fakeDatacampOfferDaoMock.getNonFake(Mockito.anyList()))
               .thenAnswer(invocation -> invocation.getArgument(0));

        DcpImageValidation.Checker checker = new DcpImageValidation.Checker(
                dcpPartnerPictureDao, categoryAccess, fakeDatacampOfferDaoMock
        );

        ProcessTaskResult<List<TicketValidationResult>> result1 = checker.validate(Arrays.asList(ticket1));
        ProcessTaskResult<List<TicketValidationResult>> result2 = checker.validate(Arrays.asList(ticket1, ticket2));

        problems1.toString();
    }
}
