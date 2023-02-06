package ru.yandex.market.rg.asyncreport.unitedcatalog.migration;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import Market.DataCamp.DataCampUnitedOffer;
import Market.DataCamp.PartnerCategoryOuterClass;
import Market.DataCamp.SyncAPI.SyncCategory;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.grpc.stub.StreamObserver;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;
import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportsService;
import ru.yandex.market.core.asyncreport.model.ReportsType;
import ru.yandex.market.core.feed.supplier.SupplierXlsHelper;
import ru.yandex.market.core.feed.validation.result.FeedXlsService;
import ru.yandex.market.core.fulfillment.mds.ReportsMdsStorage;
import ru.yandex.market.core.offer.mapping.MboMappingService;
import ru.yandex.market.core.offer.mapping.OfferConversionService;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.environment.EnvironmentService;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.rg.asyncreport.assortment.AssortmentReportWriteService;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.market.rg.asyncreport.unitedcatalog.migration.UnitedOfferBuilder.offerBuilder;

@ExtendWith(MockitoExtension.class)
@DbUnitDataSet(before = "AbstractMigrationTaskProcessorTest.before.csv")
public abstract class AbstractMigrationTaskProcessorTest extends FunctionalTest {
    @Autowired
    protected MdsS3Client mdsS3Client;
    @Autowired
    @Qualifier("dataCampMigrationClient")
    protected DataCampClient dataCampMigrationClient;
    @Autowired
    @Qualifier("mbocGrpcClient")
    protected BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub mbocGrcpClient;
    @Autowired
    protected BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mbocGrpcService;
    @Autowired
    @Qualifier("mdmGrpcClient")
    protected BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub mdmGrpcClient;
    @Autowired
    protected BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase mdmGrpcService;
    @Autowired
    @Qualifier("pppGrpcClient")
    protected BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub pppGrpcClient;
    @Autowired
    @Qualifier("pppGrpcService")
    protected BusinessMigrationServiceGrpc.BusinessMigrationServiceImplBase pppGrpcService;
    @Autowired
    protected ReportsMdsStorage<ReportsType> reportsMdsStorage;
    @Spy
    @Autowired
    protected ReportsService<ReportsType> reportsService;
    @Autowired
    protected List<DistributedMigrator> distributedMigrators;
    @Autowired
    protected EnvironmentService environmentService;
    @Autowired
    protected SupplierXlsHelper supplierXlsHelper;
    @Autowired
    protected FeedXlsService<OfferInfo> feedXlsService;
    @Autowired
    protected MigrationTaskStateService migrationTaskStateService;
    @Autowired
    protected MboMappingService mboMappingService;
    @Autowired
    protected OfferConversionService offerConversionService;
    @Autowired
    protected ru.yandex.market.mboc.http.MboMappingsService mboMappingsService;

    @Autowired
    protected AssortmentReportWriteService assortmentReportWriteService;

    ObjectMapper objectMapper = new ObjectMapper();

    Clock clock = Clock.fixed(DateTimes.toInstantAtDefaultTz(2019, 10, 11), ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        UnitedOfferBuilder.clock = clock;

        try {
            doReturn(new URL("http://path/to"))
                    .when(mdsS3Client)
                    .getUrl(any());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    void mockDataCamp(long businessId, long partnerId, int total, DataCampMockRequest request,
                      DataCampMockResponse response) {
        willReturn(getBusinessOffersResult(businessId, partnerId, total, response))
                .given(dataCampMigrationClient).searchBusinessOffers(getBusinessOffersRequest(request));
        willReturn(getPartnerCategoriesResponse(request.offerIds))
                .given(dataCampMigrationClient).getPartnerCategories(eq(businessId));
    }

    protected SyncCategory.PartnerCategoriesResponse getPartnerCategoriesResponse(List<String> offerIds) {
        AtomicInteger i = new AtomicInteger(1);
        List<PartnerCategoryOuterClass.PartnerCategory> cats = offerIds.stream()
                .map(offerId -> PartnerCategoryOuterClass.PartnerCategory.newBuilder()
                        .setId(i.getAndIncrement())
                        .setName("category_" + offerId)
                        .build())
                .collect(Collectors.toList());

        return SyncCategory.PartnerCategoriesResponse.newBuilder()
                .setCategories(PartnerCategoryOuterClass.PartnerCategoriesBatch.newBuilder()
                        .addAllCategories(cats)
                        .build())
                .build();
    }

    protected SearchBusinessOffersResult getBusinessOffersResult(long businessId, long serviceId, int total,
                                                                 DataCampMockResponse response) {
        return SearchBusinessOffersResult.builder()
                .setTotalCount(total)
                .setOffers(
                        response.offerIds != null
                                ? response.offerIds.stream()
                                .map(offerId -> offerBuilder((int) businessId, (int) serviceId, offerId).build())
                                .collect(Collectors.toList())
                                : response.offers)
                .setNextPageToken(response.nextPage)
                .build();
    }

    protected SearchBusinessOffersRequest getBusinessOffersRequest(DataCampMockRequest request) {
        return ArgumentMatchers.argThat(
                argument -> CollectionUtils.isEqualCollection(argument.getOfferIds(), request.offerIds)
                        && Objects.equals(argument.getPageRequest().seekKey().orElse(null), request.page)
        );
    }


    protected static class DataCampMockRequest {
        String page;
        List<String> offerIds;

        DataCampMockRequest(String page, List<String> offerIds) {
            this.page = page;
            this.offerIds = offerIds;
        }
    }

    protected static class DataCampMockResponse {
        String nextPage;
        List<String> offerIds;
        List<DataCampUnitedOffer.UnitedOffer> offers;

        public static DataCampMockResponse ofOffers(String nextPage, List<DataCampUnitedOffer.UnitedOffer> offers) {
            var response = new DataCampMockResponse();
            response.nextPage = nextPage;
            response.offers = offers;
            return response;
        }

        public static DataCampMockResponse ofOfferIds(String nextPage, List<String> offerIds) {
            var response = new DataCampMockResponse();
            response.nextPage = nextPage;
            response.offerIds = offerIds;
            return response;
        }
    }


    protected static class MboMockResponse {
        String nextPage;
        List<String> offerIds;
        List<SupplierOffer.Offer> offers;

        public static MboMockResponse ofOffers(String nextPage, List<SupplierOffer.Offer> offers) {
            var response = new MboMockResponse();
            response.nextPage = nextPage;
            response.offers = offers;
            return response;
        }

        public static MboMockResponse ofOfferIds(String nextPage, List<String> offerIds) {
            var response = new MboMockResponse();
            response.nextPage = nextPage;
            response.offerIds = offerIds;
            return response;
        }
    }

    void mockMbo(long businessId, long partnerId, int total, DataCampMockRequest request,
                 MboMockResponse response) {
        willReturn(getMboOffersResult(businessId, partnerId, total, response))
                .given(mboMappingsService).searchMappingsByShopId(getMboOffersRequest(request));
    }

    protected MboMappings.SearchMappingsResponse getMboOffersResult(
            long businessId, long serviceId, int total,
            MboMockResponse response
    ) {
        MboMappings.SearchMappingsResponse.Builder builder = MboMappings.SearchMappingsResponse.newBuilder()
                .setTotalCount(total)
                .addAllOffers(response.offerIds != null
                        ? response.offerIds.stream()
                        .map(offerId -> MboOfferBuilder.offerBuilder((int) businessId, (int) serviceId, offerId).build())
                        .collect(Collectors.toList())
                        : response.offers);

        if (response.nextPage != null) {
            builder.setNextOffsetKey(response.nextPage);
        }

        return builder.build();
    }

    protected MboMappings.SearchMappingsBySupplierIdRequest getMboOffersRequest(DataCampMockRequest request) {
        return ArgumentMatchers.argThat(
                argument -> !argument.hasOffsetKey() && request.page == null || Objects.equals(argument.getOffsetKey(), request.page)
        );
    }

    protected Object successMergeAnswer(InvocationOnMock invocation) {
        StreamObserver<BusinessMigration.MergeOffersResponse> mergeOffersResponseObserver =
                invocation.getArgument(1);
        mergeOffersResponseObserver.onNext(BusinessMigration.MergeOffersResponse.newBuilder().setSuccess(true).build());
        mergeOffersResponseObserver.onCompleted();
        return null;
    }
}
