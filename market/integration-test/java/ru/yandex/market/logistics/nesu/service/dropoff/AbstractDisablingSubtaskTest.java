package ru.yandex.market.logistics.nesu.service.dropoff;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.abo.api.client.AboAPI;
import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.point.filter.LogisticsPointFilter;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.logistic.segment.BaseLogisticSegmentDto;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.configuration.properties.PartnerBannersProperties;
import ru.yandex.market.logistics.nesu.jobs.model.DisableDropoffSubtaskPayload;
import ru.yandex.market.logistics.nesu.jobs.processor.DisableDropoffSubtaskProcessor;
import ru.yandex.market.logistics.nesu.logging.enums.LoggingTag;
import ru.yandex.market.logistics.nesu.service.lms.LogisticSegmentSequenceFilterFactory;
import ru.yandex.market.logistics.test.integration.logging.BackLogCaptor;
import ru.yandex.market.pvz.client.logistics.PvzLogisticsClient;

import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.nesu.utils.CommonsConstants.MSK_TIME_ZONE;

@DatabaseSetup("/service/dropoff/before/disabling_dropoff_request.xml")
public abstract class AbstractDisablingSubtaskTest extends AbstractContextualTest {

    protected static final Long DROPSHIP_PARTNER_ID_1 = 1L;
    protected static final Long DROPSHIP_PARTNER_ID_2 = 2L;
    protected static final Long DROPSHIP_PARTNER_ID_3 = 3L;
    protected static final Long DROPSHIP_LOGISTIC_POINT_ID_11 = 11L;
    protected static final Long DROPSHIP_LOGISTIC_POINT_ID_22 = 22L;
    protected static final Long DROPSHIP_LOGISTIC_POINT_ID_33 = 33L;

    protected static final Long SHOP_ID_11 = 11L;

    protected static final Long DROPOFF_PARTNER_ID_123 = 123L;
    protected static final Long DROPOFF_LOGISTIC_POINT_ID_321 = 321L;

    protected static final Long SC_PARTNER_ID_45 = 45L;
    protected static final Long SC_LOGISTIC_POINT_ID_54 = 54L;

    @Autowired
    protected DisableDropoffSubtaskProcessor disableDropoffSubtaskProcessor;

    @RegisterExtension
    protected final BackLogCaptor backLogCaptor = new BackLogCaptor();

    @Autowired
    protected LMSClient lmsClient;

    @Autowired
    protected AboAPI aboClient;

    @Autowired
    protected PvzLogisticsClient pvzLogisticsClient;

    @Autowired
    protected LogisticSegmentSequenceFilterFactory logisticSegmentSequenceFilterFactory;

    @Autowired
    protected PartnerBannersProperties partnerBannersProperties;

    @BeforeEach
    void setup() {
        clock.setFixed(Instant.parse("2021-11-26T17:00:00Z"), MSK_TIME_ZONE);
    }

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(lmsClient, aboClient);
    }

    @Nonnull
    protected DisableDropoffSubtaskPayload getDisableDropoffPayload(long disableRequestId, long subtaskId) {
        return new DisableDropoffSubtaskPayload(REQUEST_ID, disableRequestId, subtaskId);
    }

    protected void verifyBacklog(LoggingTag tag, String exceptionText, long disableRequestId, long disableSubtaskId) {
        softly.assertThat(backLogCaptor.getResults().toString())
            .contains(
                "level=WARN\t"
                    + "format=plain\t"
                    + "code=DISABLE_DROPOFF_SUBTASK\t"
                    + "payload=" + exceptionText + "\t"
                    + "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t"
                    + "tags=" + tag.name() + "\t"
                    + "extra_keys=disablingSubtaskId,disablingRequestId\t"
                    + "extra_values=" + disableSubtaskId + "," + disableRequestId
            );
    }

    protected void mockDropships() {
        when(lmsClient.searchLogisticSegmentsSequence(
            logisticSegmentSequenceFilterFactory.createFromWarehousesToWarehouseWithId(DROPOFF_LOGISTIC_POINT_ID_321)
        )).thenReturn(dropshipToDropoff());

    }

    protected void mockSc() {
        when(lmsClient.searchLogisticSegmentsSequence(
            logisticSegmentSequenceFilterFactory.createFromWarehouseWithIdToWarehouses(DROPOFF_LOGISTIC_POINT_ID_321)
        )).thenReturn(dropoffToSc());
    }

    protected void mockScDropoffAddress(@Nullable String scAddress, @Nullable String dropoffAddress) {
        when(lmsClient.getLogisticsPoints(
            LogisticsPointFilter.newBuilder()
                .ids(Set.of(SC_LOGISTIC_POINT_ID_54, DROPOFF_LOGISTIC_POINT_ID_321))
                .build()
        )).thenReturn(
            List.of(
                LogisticsPointResponse.newBuilder()
                    .id(SC_LOGISTIC_POINT_ID_54)
                    .address(Address.newBuilder().addressString(scAddress).build())
                    .build(),
                LogisticsPointResponse.newBuilder()
                    .id(DROPOFF_LOGISTIC_POINT_ID_321)
                    .address(Address.newBuilder().addressString(dropoffAddress).build())
                    .build()
            )
        );
    }

    @Nonnull
    protected String getBannerId(Long requestId) {
        return partnerBannersProperties.getDropoffBannerId(
            requestId
        );
    }

    @Nonnull
    private List<BaseLogisticSegmentDto> dropshipToDropoff() {
        return List.of(
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPSHIP_PARTNER_ID_1)
                .setLogisticsPointId(DROPSHIP_LOGISTIC_POINT_ID_11),
            new BaseLogisticSegmentDto().setPartnerId(DROPSHIP_PARTNER_ID_1),
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPOFF_PARTNER_ID_123)
                .setLogisticsPointId(DROPOFF_LOGISTIC_POINT_ID_321),
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPSHIP_PARTNER_ID_2)
                .setLogisticsPointId(DROPSHIP_LOGISTIC_POINT_ID_22),
            new BaseLogisticSegmentDto().setPartnerId(DROPSHIP_PARTNER_ID_2),
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPOFF_PARTNER_ID_123)
                .setLogisticsPointId(DROPOFF_LOGISTIC_POINT_ID_321),
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPSHIP_PARTNER_ID_3)
                .setLogisticsPointId(DROPSHIP_LOGISTIC_POINT_ID_33),
            new BaseLogisticSegmentDto().setPartnerId(DROPSHIP_PARTNER_ID_3),
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPOFF_PARTNER_ID_123)
                .setLogisticsPointId(DROPOFF_LOGISTIC_POINT_ID_321)
        );
    }

    @Nonnull
    private List<BaseLogisticSegmentDto> dropoffToSc() {
        return List.of(
            new BaseLogisticSegmentDto()
                .setPartnerId(DROPOFF_PARTNER_ID_123)
                .setLogisticsPointId(DROPOFF_LOGISTIC_POINT_ID_321),
            new BaseLogisticSegmentDto().setPartnerId(DROPOFF_PARTNER_ID_123),
            new BaseLogisticSegmentDto()
                .setPartnerId(SC_PARTNER_ID_45)
                .setLogisticsPointId(SC_LOGISTIC_POINT_ID_54)
        );
    }
}
