package ru.yandex.market.mbi.data;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.partner.model.CompletePartnerInfo;
import ru.yandex.market.core.partner.placement.PartnerPlacementProgramType;
import ru.yandex.market.core.state.DataChangesEvent;
import ru.yandex.market.mbi.data.outer.DataOuterServiceUtil;
import ru.yandex.market.mbi.data.outer.PartnerDataOuterService;

import static org.mockito.internal.verification.VerificationModeFactory.times;

/**
 * Тесты для {@link PartnerDataService}.
 */
public class PartnerDataServiceTest extends FunctionalTest {

    private static final Set<PartnerPlacementProgramType> CPC_PROGRAM =
            Set.of(PartnerPlacementProgramType.CPC);
    private static final Set<PartnerPlacementProgramType> FBY_FBY_PLUS_PROGRAMS =
            Set.of(PartnerPlacementProgramType.FULFILLMENT, PartnerPlacementProgramType.CROSSDOCK);
    private static final Set<PartnerPlacementProgramType> DROPSHIP_PROGRAM =
            Set.of(PartnerPlacementProgramType.DROPSHIP);

    @Autowired
    private PartnerDataService partnerDataService;

    @Test
    @DbUnitDataSet(before = "PartnerDataServiceTest.before.csv")
    public void testProvideDataForYt() {
        Consumer<Pair<Long, CompletePartnerInfo>> mock = Mockito.mock(Consumer.class);
        partnerDataService.provideDataForYt(mock);
        ArgumentCaptor<Pair<Long, CompletePartnerInfo>> requestCaptor = ArgumentCaptor.forClass(Pair.class);
        Mockito.verify(mock, times(9)).accept(requestCaptor.capture());
        List<Pair<Long, CompletePartnerInfo>> values = requestCaptor.getAllValues();
        Assertions.assertEquals(9, values.size());
        Map<Long, CompletePartnerInfo> valuesMap = values.stream()
                .collect(Collectors.toMap(Pair::getKey, Pair::getValue));

        //проверяем информацию по партнерам
        compareCompletePartnerInfo(1L, null, 1L, CampaignType.TPL_OUTLET, Set.of(), "трипиэль", 1L, false, valuesMap.get(1L));
        compareCompletePartnerInfo(2L, null, 2L, CampaignType.SUPPLIER_1P, Set.of(), "одинпишник", 3L, false,
                valuesMap.get(2L));
        compareCompletePartnerInfo(3L, null, 3L, CampaignType.SHOP, CPC_PROGRAM, null, null, false, valuesMap.get(3L));
        compareCompletePartnerInfo(4L, 101L, 4L, CampaignType.SHOP, CPC_PROGRAM, "бизнесошоп", 4L, false, valuesMap.get(4L));
        compareCompletePartnerInfo(
                5L, 101L, 5L, CampaignType.SUPPLIER, FBY_FBY_PLUS_PROGRAMS, "бизнессаплаер", 4L, false, valuesMap.get(5L)
        );
        compareCompletePartnerInfo(
                6L, 101L, 6L, CampaignType.SUPPLIER, DROPSHIP_PROGRAM, "бизнессаплаерv2", 4L, false, valuesMap.get(6L)
        );
        compareCompletePartnerInfo(
                7L, null, null, CampaignType.EATS_AND_LAVKA, Set.of(), null, null, false, valuesMap.get(7L)
        );
        compareCompletePartnerInfo(8L, 102L, 8L, CampaignType.DIRECT, Set.of(), null, 5L, false, valuesMap.get(8L));
        compareCompletePartnerInfo(103L, null, null, CampaignType.SUPPLIER, Set.of(), "deleted", null, true, valuesMap.get(103L));
    }

    private void compareCompletePartnerInfo(
            long expectedPartnerId,
            Long expectedBusinessId,
            Long expectedCampaignId,
            CampaignType expectedCampaignType,
            Set<PartnerPlacementProgramType> expectedPlacementProgramTypes,
            String expectedInternalName,
            Long expectedOwnerUid,
            boolean expectedIsDeleted,
            CompletePartnerInfo actual
    ) {
        Assertions.assertEquals(expectedPartnerId, actual.getPartnerId());
        Assertions.assertEquals(expectedBusinessId, actual.getBusinessId());
        Assertions.assertEquals(expectedCampaignId, actual.getCampaignId());
        Assertions.assertEquals(expectedCampaignType, actual.getCampaignType());
        Assertions.assertEquals(expectedPlacementProgramTypes, actual.getPlacementProgramTypes());
        Assertions.assertEquals(expectedInternalName, actual.getInternalName());
        Assertions.assertEquals(expectedOwnerUid, actual.getOwnerUid());
        Assertions.assertEquals(expectedIsDeleted, actual.isDeleted());
    }

    @Test
    public void testGetPartnerDataForExport() {
        Instant eventTime = Instant.now();
        PartnerDataOuterClass.PartnerData expected = PartnerDataOuterClass.PartnerData.newBuilder()
                .setGeneralInfo(DataOuterServiceUtil.getGeneralDataInfo(eventTime,
                        DataChangesEvent.PartnerDataOperation.READ))
                .setPartnerId(1337L)
                .setBusinessId(42L)
                .setCampaignId(1337L)
                .setType(PartnerDataOuterClass.PartnerType.SHOP)
                .addPlacementPrograms(PartnerDataOuterClass.PlacementProgramType.CPC)
                .setInternalName("шопчик")
                .setOwnerUid(215L)
                .build();

        CompletePartnerInfo partnerInfo = new CompletePartnerInfo.Builder()
                .setPartnerId(1337L)
                .setBusinessId(42L)
                .setCampaignId(1337L)
                .setCampaignType(CampaignType.SHOP)
                .setPlacementProgramTypes(EnumSet.of(PartnerPlacementProgramType.CPC))
                .setInternalName("шопчик")
                .setOwnerUid(215L)
                .build();

        Assertions.assertEquals(
                expected,
                PartnerDataOuterService.getPartnerDataForExport(
                        1337L, partnerInfo, eventTime, DataChangesEvent.PartnerDataOperation.READ
                )
        );
    }
}
