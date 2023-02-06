package ru.yandex.market.pvz.core.domain.survey;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.client.model.pickup_point.PickupPointBrandingType;
import ru.yandex.market.pvz.client.model.survey.PartnerSurveyStatus;
import ru.yandex.market.pvz.core.domain.banner_information.BannerCampaignFeatures;
import ru.yandex.market.pvz.core.domain.legal_partner.LegalPartner;
import ru.yandex.market.pvz.core.domain.operator_window.OwClient;
import ru.yandex.market.pvz.core.domain.operator_window.dto.OwTicketDto;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPoint;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointQueryService;
import ru.yandex.market.pvz.core.domain.pickup_point.PickupPointSimpleParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;
import ru.yandex.market.pvz.core.test.factory.TestBrandRegionFactory;
import ru.yandex.market.pvz.core.test.factory.TestLegalPartnerFactory;
import ru.yandex.market.pvz.core.test.factory.TestPickupPointFactory;
import ru.yandex.market.pvz.core.test.factory.TestSurveyFactory;
import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.common.util.exception.TplIllegalStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.pvz.core.domain.dbqueue.PvzQueueType.CREATE_SURVEY_PARTNER_BATCH;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.COMMENT_META_CLASS;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PARTNER_BODY_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PARTNER_TITLE_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PICKUP_POINT_BODY_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PICKUP_POINT_CABINET_URL_TEST;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PICKUP_POINT_TITLE_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.SERVICE;
import static ru.yandex.market.pvz.core.domain.survey.SurveyPartnerCommandService.OW_ID_PREFIX;

@Slf4j
@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SurveyPartnerCommandServiceTest {

    private static final Long OW_ID = 12345L;

    private final TestLegalPartnerFactory legalPartnerFactory;
    private final SurveyPartnerCommandService surveyPartnerCommandService;
    private final TestSurveyFactory surveyFactory;
    private final TestableClock clock;
    private final TestPickupPointFactory pickupPointFactory;
    private final TestBrandRegionFactory brandRegionFactory;
    private final DbQueueTestUtil dbQueueTestUtil;
    private final OwClient owClient;
    private final SurveyPartnerQueryService surveyPartnerQueryService;
    private final PickupPointQueryService pickupPointQueryService;

    @Test
    void whenCreateThenSuccess() {
        SurveyPartnerParams surveyPartnerParams = buildSurveyPartner();
        SurveyPartnerParams result = surveyPartnerCommandService.create(surveyPartnerParams);
        assertThat(result).isEqualTo(surveyPartnerParams);
    }

    @Test
    void whenSaveSurveyPartnerTwiceThenError() {
        SurveyPartnerParams surveyPartnerParams = buildSurveyPartner();
        surveyPartnerCommandService.create(surveyPartnerParams);
        assertThatThrownBy(() -> surveyPartnerCommandService.create(surveyPartnerParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    private SurveyPartnerParams buildSurveyPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        SurveyParams survey = surveyFactory.create();
        return SurveyPartnerParams.builder()
                .surveyId(survey.getId())
                .legalPartnerId(legalPartner.getId())
                .showDateTime(clock.instant())
                .owId(1234L)
                .owStatus(PartnerSurveyStatus.REGISTERED)
                .build();
    }

    @Test
    void whenCreateWithPickupPointThenSuccess() {
        SurveyPartnerParams surveyPartnerParams = buildSurveyPartnerWithPickupPoint();
        SurveyPartnerParams result = surveyPartnerCommandService.create(surveyPartnerParams);
        assertThat(result).isEqualTo(surveyPartnerParams);
    }

    @Test
    void whenSaveSurveyPartnerWithPickupPointTwiceThenError() {
        SurveyPartnerParams surveyPartnerParams = buildSurveyPartnerWithPickupPoint();
        surveyPartnerCommandService.create(surveyPartnerParams);
        assertThatThrownBy(() -> surveyPartnerCommandService.create(surveyPartnerParams))
                .isExactlyInstanceOf(TplIllegalStateException.class);
    }

    private SurveyPartnerParams buildSurveyPartnerWithPickupPoint() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .build()
        );
        SurveyParams survey = surveyFactory.create();
        return SurveyPartnerParams.builder()
                .surveyId(survey.getId())
                .legalPartnerId(legalPartner.getId())
                .pvzMarketId(pickupPoint.getPvzMarketId())
                .showDateTime(clock.instant())
                .owId(1234L)
                .owStatus(PartnerSurveyStatus.REGISTERED)
                .build();
    }

    @Test
    void whenCreateSurveySuccess() throws ParseException {
        clock.setFixed(Instant.parse("2021-01-14T12:00:00Z"), ZoneOffset.UTC);
        LegalPartner legalPartner = createTestLegalPartner();
        SurveyParams surveyParams = createSurvey();

        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 0);

        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneOffset.UTC);
        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 1);

        when(owClient.createSurveyPartnerTicket(any())).thenReturn(OW_ID_PREFIX + OW_ID);
        dbQueueTestUtil.executeAllQueueItems(CREATE_SURVEY_PARTNER_BATCH);

        ArgumentCaptor<OwTicketDto> owTicketDtoArgumentCaptor  = ArgumentCaptor.forClass(OwTicketDto.class);
        verify(owClient, times(3)).createSurveyPartnerTicket(owTicketDtoArgumentCaptor.capture());
        OwTicketDto owTicketDto = owTicketDtoArgumentCaptor.getValue();

        OwTicketDto expectedTicket = OwTicketDto.builder()
                .title(String.format(PARTNER_TITLE_PATTERN, surveyParams.getTitle(), legalPartner.getPartnerId()))
                .service(SERVICE)
                .partnerId(legalPartner.getPartnerId())
                .stTicket(surveyParams.getTitle())
                .clientEmail(legalPartner.getDelegate().getDelegateEmail())
                .comment(
                        OwTicketDto.Comment.builder()
                                .metaClass(COMMENT_META_CLASS)
                                .body(String.format(
                                        PARTNER_BODY_PATTERN, surveyParams.getBody(), surveyParams.getUrl()
                                ))
                                .build()
                )
                .build();

        assertThat(owTicketDto).isEqualTo(expectedTicket);

        SurveyPartnerParams expected = SurveyPartnerParams.builder()
                .surveyId(surveyParams.getId())
                .legalPartnerId(legalPartner.getId())
                .owId(OW_ID)
                .owStatus(PartnerSurveyStatus.REGISTERED)
                .showDateTime(Instant.parse("2021-01-15T00:00:00Z"))
                .build();

        SurveyPartnerParams result = surveyPartnerQueryService.findByLegalPartnerIdAndOwId(legalPartner.getId(), OW_ID);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void whenCreateSurveyWithoutSuitableCompanies() throws ParseException {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneOffset.UTC);
        LegalPartner legalPartner = createTestLegalPartner();
        SurveyParams surveyParams = surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .startDate(LocalDate.of(2021, 1, 15))
                        .endDate(LocalDate.of(2021, 10, 1))
                        .frequency("0 0 0 15 1/6 ? *")
                        .campaignIds(List.of(legalPartner.getId() + 1))
                        .surveyByPickupPoint(false)
                        .build()
        );;

        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 0);
    }

    @Test
    void whenCreateSurveyForPickupPointsSuccess() throws ParseException {
        clock.setFixed(Instant.parse("2021-01-14T12:00:00Z"), ZoneOffset.UTC);
        LegalPartner legalPartner = createTestLegalPartner();
        SurveyParams surveyParams = createSurveyWithFlag(true);

        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 0);

        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneOffset.UTC);
        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 1);

        when(owClient.createSurveyPartnerTicket(any())).thenReturn(OW_ID_PREFIX + OW_ID);
        dbQueueTestUtil.executeAllQueueItems(CREATE_SURVEY_PARTNER_BATCH);

        List<PickupPointSimpleParams> pickupPoints = pickupPointQueryService.getAllByLegalPartnerId(
                legalPartner.getId()
        );
        PickupPointSimpleParams pickupPointSimpleParams = pickupPoints.get(0);

        ArgumentCaptor<OwTicketDto> owTicketDtoArgumentCaptor  = ArgumentCaptor.forClass(OwTicketDto.class);
        verify(owClient, times(4)).createSurveyPartnerTicket(owTicketDtoArgumentCaptor.capture());
        OwTicketDto owTicketDto = owTicketDtoArgumentCaptor.getValue();

        String pickupPointCabinetUrl = String.format(
                PICKUP_POINT_CABINET_URL_TEST, pickupPointSimpleParams.getPvzMarketId()
        );
        OwTicketDto expectedTicket = OwTicketDto.builder()
                .title(String.format(
                        PICKUP_POINT_TITLE_PATTERN, surveyParams.getTitle(), legalPartner.getPartnerId(),
                        pickupPointSimpleParams.getName()))
                .service(SERVICE)
                .partnerId(legalPartner.getPartnerId())
                .pupMarketId(pickupPointSimpleParams.getPvzMarketId())
                .stTicket(surveyParams.getTitle())
                .clientEmail(legalPartner.getDelegate().getDelegateEmail())
                .comment(
                        OwTicketDto.Comment.builder()
                                .metaClass(COMMENT_META_CLASS)
                                .body(
                                        String.format(
                                                PICKUP_POINT_BODY_PATTERN, pickupPointCabinetUrl,
                                                pickupPointSimpleParams.getName(), surveyParams.getBody(),
                                                surveyParams.getUrl()
                                        )
                                )
                                .build()
                )
                .build();

        assertThat(owTicketDto).isEqualTo(expectedTicket);

        SurveyPartnerParams expected = SurveyPartnerParams.builder()
                .surveyId(surveyParams.getId())
                .legalPartnerId(legalPartner.getId())
                .pvzMarketId(pickupPointSimpleParams.getPvzMarketId())
                .owId(OW_ID)
                .owStatus(PartnerSurveyStatus.REGISTERED)
                .showDateTime(Instant.parse("2021-01-15T00:00:00Z"))
                .build();

        SurveyPartnerParams result = surveyPartnerQueryService.findByLegalPartnerIdAndOwId(legalPartner.getId(), OW_ID);
        assertThat(result).isEqualTo(expected);

        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneOffset.UTC);
        PickupPoint pickupPoint = addPickupPoint(legalPartner);

        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 1);

        long newId = OW_ID + 1;
        when(owClient.createSurveyPartnerTicket(any())).thenReturn(OW_ID_PREFIX + newId);
        dbQueueTestUtil.executeAllQueueItems(CREATE_SURVEY_PARTNER_BATCH);

        expected = SurveyPartnerParams.builder()
                .surveyId(surveyParams.getId())
                .legalPartnerId(legalPartner.getId())
                .pvzMarketId(pickupPoint.getPvzMarketId())
                .owId(newId)
                .owStatus(PartnerSurveyStatus.REGISTERED)
                .showDateTime(Instant.parse("2021-01-15T00:00:00Z"))
                .build();
        result = surveyPartnerQueryService.findByLegalPartnerIdAndOwId(legalPartner.getId(), newId);
        assertThat(result).isEqualTo(expected);
    }

    private LegalPartner createTestLegalPartner() {
        LegalPartner legalPartner = legalPartnerFactory.createLegalPartner();
        legalPartner = legalPartnerFactory.forceApprove(legalPartner.getId(), LocalDate.of(2021, 1, 1));

        addPickupPoint(legalPartner);

        return legalPartner;
    }

    private PickupPoint addPickupPoint(LegalPartner legalPartner) {
        PickupPoint pickupPoint = pickupPointFactory.createPickupPointFromCrm(
                TestPickupPointFactory.CreatePickupPointBuilder.builder()
                        .legalPartner(legalPartner)
                        .params(TestPickupPointFactory.PickupPointTestParams.builder()
                                .name(UUID.randomUUID().toString())
                                .build())
                        .build()
        );
        brandRegionFactory.createDefaults();
        pickupPointFactory.updatePickupPoint(
                pickupPoint.getId(), TestPickupPointFactory.PickupPointTestParams.builder()
                        .brandingType(PickupPointBrandingType.FULL)
                        .build());
        return pickupPoint;
    }

    private SurveyParams createSurvey() {
        return createSurveyWithFlag(false);
    }

    private SurveyParams createSurveyWithFlag(boolean isSurveyByPickupPoint) {
        return surveyFactory.create(
                TestSurveyFactory.SurveyTestParams.builder()
                        .startDate(LocalDate.of(2021, 1, 15))
                        .endDate(LocalDate.of(2021, 10, 1))
                        .frequency("0 0 0 15 1/6 ? *")
                        .campaignFeatures(List.of(BannerCampaignFeatures.BRANDED.name()))
                        .surveyByPickupPoint(isSurveyByPickupPoint)
                        .build()
        );
    }

    @Test
    void checkDifferentDatesWhenCreateSurvey() throws ParseException {
        when(owClient.createSurveyPartnerTicket(any())).thenReturn(OW_ID_PREFIX + OW_ID);
        ArgumentCaptor<OwTicketDto> owTicketDtoArgumentCaptor  = ArgumentCaptor.forClass(OwTicketDto.class);

        clock.setFixed(Instant.parse("2021-01-14T12:00:00Z"), ZoneOffset.UTC);
        createTestLegalPartner();
        SurveyParams surveyParams = createSurvey();

        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 0);

        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneOffset.UTC);
        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 1);
        dbQueueTestUtil.executeAllQueueItems(CREATE_SURVEY_PARTNER_BATCH);

        clock.setFixed(Instant.parse("2021-07-14T12:00:00Z"), ZoneOffset.UTC);
        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 0);

        clock.setFixed(Instant.parse("2021-07-15T12:00:00Z"), ZoneOffset.UTC);
        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 1);
        dbQueueTestUtil.executeAllQueueItems(CREATE_SURVEY_PARTNER_BATCH);

        clock.setFixed(Instant.parse("2022-02-01T12:00:00Z"), ZoneOffset.UTC);
        surveyPartnerCommandService.create(surveyParams);
        dbQueueTestUtil.assertTasksHasSize(CREATE_SURVEY_PARTNER_BATCH, 0);

        verify(owClient, times(2)).createSurveyPartnerTicket(owTicketDtoArgumentCaptor.capture());
    }

}
