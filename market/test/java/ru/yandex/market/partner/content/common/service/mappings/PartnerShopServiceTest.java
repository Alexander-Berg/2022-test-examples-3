package ru.yandex.market.partner.content.common.service.mappings;

import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcTicketProcessDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileDataProcessRequest;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Source;

import java.util.Optional;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

@Issue("MARKETIR-8665")
public class PartnerShopServiceTest {
    private static final Random RANDOM = new Random(13294345);

    @Mock
    private SourceDao sourceDao;

    private PartnerShopService partnerShopService;
    @Mock
    private FileDataProcessRequestDao fileDataProcessRequestDao;
    @Mock
    private GcTicketProcessDao gcTicketProcessDao;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        partnerShopService = new PartnerShopService(sourceDao, fileDataProcessRequestDao, gcTicketProcessDao);
    }

    @Test(expected = IllegalStateException.class)
    public void getWhenNoSourceForDataBucket() {
        long dataBucketId = generateId();

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.empty());

        //------

        partnerShopService.getShopIdByDataBucketId(dataBucketId);
    }

    @Test(expected = IllegalStateException.class)
    public void getOrFailWhenNoSourceForDataBucket() {
        long dataBucketId = generateId();

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.empty());

        //------

        partnerShopService.getShopIdByDataBucketIdOrFail(dataBucketId);
    }

    @Test(expected = IllegalStateException.class)
    public void getOrFailWhenNoSourceForRequest() {
        long requestId = generateId();

        Mockito.when(sourceDao.getSourceByRequestId(requestId))
            .thenReturn(Optional.empty());

        //------

        partnerShopService.getShopIdByRequestIdOrFail(requestId);
    }

    @Test(expected = IllegalStateException.class)
    public void getWhenNullPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        Source source = createSource(null);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        partnerShopService.getShopIdByDataBucketId(dataBucketId);
    }

    @Test
    public void getWhenSubstituteAndNullPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        Source source = createSourceWithSubstitute(null);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        Optional<Integer> actualShopId = partnerShopService.getShopIdByDataBucketId(dataBucketId);

        //------

        assertThat(actualShopId.isPresent()).isFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void getOrFailWhenNullPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        Source source = createSource(null);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        partnerShopService.getShopIdByDataBucketIdOrFail(dataBucketId);
    }

    @Test(expected = IllegalStateException.class)
    public void getOrFailWhenNullPartnerShopIdForRequest() {
        long requestId = generateId();
        Source source = createSource(0);

        Mockito.when(sourceDao.getSourceByRequestId(requestId))
            .thenReturn(Optional.of(source));

        //------

        partnerShopService.getShopIdByRequestIdOrFail(requestId);
    }

    @Test(expected = IllegalStateException.class)
    public void getWhenZeroPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        Source source = createSource(0);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        partnerShopService.getShopIdByDataBucketId(dataBucketId);
    }

    @Test
    public void getWhenSubstituteAndZeroPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        Source source = createSourceWithSubstitute(0);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        Optional<Integer> actualShopId = partnerShopService.getShopIdByDataBucketId(dataBucketId);

        //------

        assertThat(actualShopId.isPresent()).isFalse();
    }

    @Test(expected = IllegalStateException.class)
    public void getOrFailWhenZeroPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        Source source = createSource(0);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        partnerShopService.getShopIdByDataBucketIdOrFail(dataBucketId);
    }

    @Test(expected = IllegalStateException.class)
    public void getOrFailWhenZeroPartnerShopIdForRequest() {
        long requestId = generateId();
        Source source = createSource(0);

        Mockito.when(sourceDao.getSourceByRequestId(requestId))
            .thenReturn(Optional.of(source));

        //------

        partnerShopService.getShopIdByRequestIdOrFail(requestId);
    }

    @Test
    public void getOkPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        int shopId = generateId();
        Source source = createSource(shopId);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        Optional<Integer> actualShopId = partnerShopService.getShopIdByDataBucketId(dataBucketId);

        //------

        assertThat(actualShopId.isPresent()).isTrue();
        assertThat(actualShopId).contains(shopId);
    }

    @Test
    public void getOrFailWhenOkPartnerShopIdForDataBucket() {
        long dataBucketId = generateId();
        int shopId = generateId();
        Source source = createSource(shopId);

        Mockito.when(sourceDao.getSourceByDataBucketId(dataBucketId))
            .thenReturn(Optional.of(source));

        //------

        int actualShopId = partnerShopService.getShopIdByDataBucketIdOrFail(dataBucketId);

        //------

        assertThat(actualShopId).isEqualTo(shopId);
    }

    @Test
    public void getOrFailWhenOkPartnerShopIdForRequest() {
        long requestId = generateId();
        int shopId = generateId();
        Source source = createSource(shopId);

        Mockito.when(sourceDao.getSourceByRequestId(requestId))
            .thenReturn(Optional.of(source));

        //------

        int actualShopId = partnerShopService.getShopIdByRequestIdOrFail(requestId);

        //------

        assertThat(actualShopId).isEqualTo(shopId);
    }

    @Test
    public void getWhenOkRequestForDatBucketIdFileProcess() {
        long dataBucketId = generateId();
        long requestId = generateId();

        FileDataProcessRequest fileDataProcessRequest = new FileDataProcessRequest();
        fileDataProcessRequest.setId(requestId);
        Mockito.when(fileDataProcessRequestDao.fetchOneByDataBucketId(dataBucketId))
                .thenReturn(fileDataProcessRequest);

        //------

        Long actualRequestId = partnerShopService.getRequest(dataBucketId);

        //------

        assertThat(actualRequestId).isEqualTo(requestId);
    }

    @Test
    public void getWhenOkRequestForDatBucketIdTicketProcess() {
        long dataBucketId = generateId();
        long requestId = generateId();

        Mockito.when(gcTicketProcessDao.getRequestIdByDataBucketId(dataBucketId))
                .thenReturn(requestId);

        //------

        Long actualRequestId = partnerShopService.getRequest(dataBucketId);

        //------

        assertThat(actualRequestId).isEqualTo(requestId);
    }

    /**
     * Generates random positive integer.
     */
    private int generateId() {
        return RANDOM.nextInt(Integer.MAX_VALUE) + 1;
    }

    private Source createSource(Integer shopId) {
        return createSource(shopId, false);
    }

    private Source createSourceWithSubstitute(Integer shopId) {
        return createSource(shopId, true);
    }

    private Source createSource(Integer shopId, boolean canSubstituteShopId) {
        return new Source(generateId(), "source", "url", shopId, canSubstituteShopId, false);
    }
}