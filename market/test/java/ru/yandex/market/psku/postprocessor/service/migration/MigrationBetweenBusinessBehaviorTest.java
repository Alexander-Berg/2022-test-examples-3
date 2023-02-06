package ru.yandex.market.psku.postprocessor.service.migration;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import Market.DataCamp.DataCampUnitedOffer;
import Market.Ping;
import Market.PingServiceGrpc;
import com.google.protobuf.Empty;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import ru.yandex.market.business.migration.BusinessMigration;
import ru.yandex.market.business.migration.BusinessMigrationServiceGrpc;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

@Ignore
public class MigrationBetweenBusinessBehaviorTest {
    private static ManagedChannel managedChannel;
    private BusinessMigrationServiceGrpc.BusinessMigrationServiceBlockingStub businessMigrationServiceBlockingStub;

    @BeforeClass
    public static void init() {
                managedChannel = ManagedChannelBuilder.forAddress("aida", 4242)
//        managedChannel = ManagedChannelBuilder.forAddress("psku-post-processor.tst.vs.market.yandex.net", 8080)
            .usePlaintext()
            .build();
    }

    @Before
    public void setup() {
        businessMigrationServiceBlockingStub = BusinessMigrationServiceGrpc.newBlockingStub(managedChannel);
    }

    @Test
    public void ping() {
        PingServiceGrpc.PingServiceBlockingStub pingServiceBlocking = PingServiceGrpc.newBlockingStub(managedChannel);
        Ping.PingResponse pingResponse = pingServiceBlocking.ping(Empty.newBuilder().build());
        assertEquals("0;ok", pingResponse.getMessage());
    }

    @Test
    public void infinitePing() {
        PingServiceGrpc.PingServiceBlockingStub pingServiceBlocking = PingServiceGrpc.newBlockingStub(managedChannel);
        while (true) {
            Ping.PingResponse ping = pingServiceBlocking.ping(Empty.newBuilder().build());
            System.out.println(ping);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void migrationLock() {
        BusinessMigration.LockBusinessRequest lockBusinessRequest = BusinessMigration.LockBusinessRequest.newBuilder()
            .setSrcBusinessId(1)
            .setDstBusinessId(2)
            .setShopId(3)
            .build();

        BusinessMigration.LockBusinessResponse lockBusinessResponse =
            businessMigrationServiceBlockingStub.lock(lockBusinessRequest);

        assertEquals(BusinessMigration.Status.SUCCESS, lockBusinessResponse.getStatus());
    }

    @Test
    public void migrationUnlock() {
        BusinessMigration.UnlockBusinessRequest unlockBusinessRequest = BusinessMigration.UnlockBusinessRequest.newBuilder()
            .setSrcBusinessId(1)
            .setDstBusinessId(2)
            .setShopId(3)
            .build();

        BusinessMigration.UnlockBusinessResponse unlockBusinessResponse =
            businessMigrationServiceBlockingStub.unlock(unlockBusinessRequest);

        assertEquals(BusinessMigration.Status.SUCCESS, unlockBusinessResponse.getStatus());
    }

    @Test
    public void migrationMergeUseTarget() throws InvalidProtocolBufferException {
        BusinessMigration.MergeOffersRequest mergeRequest = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                    .setSource(unitedOffer(10462717, "olyaklu02111120", 30708003L))
                    .setTarget(unitedOffer(10462389, "olyaklu03101120", 100648298240L))
                    .setResult(unitedOffer(10462389, "olyaklu03101120", 100648298240L))
                    .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_TARGET)
                    .build()
            )
            .build();


        String print = JsonFormat.printer().print(mergeRequest);

        BusinessMigration.MergeOffersResponse mergeResponse = businessMigrationServiceBlockingStub.merge(mergeRequest);

        assertEquals(1, mergeResponse.getMergeResponseItemList().size());
    }

    @Test
    public void migrationMergeUseSourceWithoutTarget() {
        BusinessMigration.MergeOffersRequest mergeRequest = BusinessMigration.MergeOffersRequest.newBuilder()
            .addMergeRequestItem(
                BusinessMigration.MergeOffersRequestItem.newBuilder()
                    .setSource(unitedOffer(10462389, "olyaklu03101120", 100648298240L))
//                    .setTarget(null)
                    .setResult(unitedOffer(10462389, "olyaklu03101120", 100648298240L))
                    .setConflictResolutionStrategy(BusinessMigration.ConflictResolutionStrategy.ACCEPT_SOURCE)
                    .build()
            )
            .build();

        BusinessMigration.MergeOffersResponse mergeResponse = businessMigrationServiceBlockingStub.merge(mergeRequest);

        assertEquals(1, mergeResponse.getMergeResponseItemList().size());
    }

    @Test
    public void population() throws InvalidProtocolBufferException {
        BusinessMigration.PopulateOffersRequest populateRequest = BusinessMigration.PopulateOffersRequest.newBuilder()
            .addItem(BusinessMigration.PopulateOffersRequestItem.newBuilder()
                .setResult(unitedOffer(10462381, "1olyaklu03101120", 300648298240L))
                .setStored(unitedOffer(10462381, "1olyaklu03101120", 200648298240L))
                .build()
            ).build();


        String print = JsonFormat.printer().print(populateRequest);


        BusinessMigration.PopulateOffersResponse populateOffersResponse =
            businessMigrationServiceBlockingStub.populate(populateRequest);

        assertEquals(1, populateOffersResponse.getItemCount());
    }

    @Test
    public void migrationRemove() throws InvalidProtocolBufferException {
        BusinessMigration.RemoveOffersRequest removeRequest = BusinessMigration.RemoveOffersRequest.newBuilder()
            .addAllItem(Collections.singletonList(BusinessMigration.RemoveOffersRequestItem.newBuilder()
                .setId(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(10462717)
                    .setOfferId("olyaklu02111120")
                    .build())
                .build()
            ))
            .build();

        String print = JsonFormat.printer().print(removeRequest);
        BusinessMigration.RemoveOffersResponse removeResponse = businessMigrationServiceBlockingStub.remove(removeRequest);

        assertEquals(1, removeResponse.getItemCount());
    }

    private DataCampUnitedOffer.UnitedOffer unitedOffer(int businessId, String offerId, long skuId) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
            .setBasic(DataCampOffer.Offer.newBuilder()
                .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                    .setBusinessId(businessId)
                    .setOfferId(offerId)
                    .build()
                )
                .setContent(DataCampOfferContent.OfferContent.newBuilder()
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketSkuId(skuId)
                            .build()
                        )
                        .build()
                    )
                    .build()
                )
                .build()
            )
            .build();
    }
}