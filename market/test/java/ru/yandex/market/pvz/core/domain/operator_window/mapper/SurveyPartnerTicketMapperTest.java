package ru.yandex.market.pvz.core.domain.operator_window.mapper;

import java.time.Instant;
import java.time.ZoneId;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.pvz.core.domain.operator_window.dto.OwTicketDto;
import ru.yandex.market.pvz.core.domain.survey.SurveyPartnerDbQueueParams;
import ru.yandex.market.pvz.core.test.TransactionlessEmbeddedDbTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.COMMENT_META_CLASS;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PARTNER_BODY_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PARTNER_TITLE_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PICKUP_POINT_BODY_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PICKUP_POINT_CABINET_URL_TEST;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.PICKUP_POINT_TITLE_PATTERN;
import static ru.yandex.market.pvz.core.domain.operator_window.mapper.SurveyPartnerTicketMapper.SERVICE;

@TransactionlessEmbeddedDbTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SurveyPartnerTicketMapperTest {

    private static final long SURVEY_ID = 1L;
    private static final long LEGAL_PARTNER_ID = 2L;
    private static final long PARTNER_ID = 3L;
    private static final long PVZ_MARKET_ID = 4L;
    private static final String TITLE = "survey";
    private static final String BODY = "body";
    private static final String FORM_URL = "https://forms.test.yandex.ru/surveys/";
    private static final String DELEGATE_EMAIL = "test@test.ru";
    private static final String DELEGATE_NAME = "User Name";
    private static final String PICKUP_POINT_NAME = "Названиe ПВЗ";

    private final SurveyPartnerTicketMapper surveyPartnerTicketMapper;
    private final TestableClock clock;

    @BeforeEach
    public void setup() {
        clock.setFixed(Instant.parse("2021-01-15T12:00:00Z"), ZoneId.systemDefault());
    }

    @Test
    void surveyPartner() {
        OwTicketDto output = surveyPartnerTicketMapper.map(
                SurveyPartnerDbQueueParams.builder()
                        .surveyId(SURVEY_ID)
                        .legalPartnerId(LEGAL_PARTNER_ID)
                        .partnerId(PARTNER_ID)
                        .showDateTime(clock.instant())
                        .title(TITLE)
                        .body(BODY)
                        .url(FORM_URL)
                        .delegateEmail(DELEGATE_EMAIL)
                        .delegateName(DELEGATE_NAME)
                        .stTicket(TITLE)
                        .build()
        );
        OwTicketDto expected = OwTicketDto.builder()
                .service(SERVICE)
                .title(String.format(PARTNER_TITLE_PATTERN, TITLE, PARTNER_ID))
                .clientEmail(DELEGATE_EMAIL)
                .partnerId(PARTNER_ID)
                .stTicket(TITLE)
                .comment(
                        OwTicketDto.Comment.builder()
                                .body(String.format(PARTNER_BODY_PATTERN, BODY, FORM_URL))
                                .metaClass(COMMENT_META_CLASS)
                                .build()
                )
                .build();
        assertThat(output).isEqualTo(expected);
    }

    @Test
    void surveyByPickupPoint() {
        OwTicketDto output = surveyPartnerTicketMapper.map(
                SurveyPartnerDbQueueParams.builder()
                        .surveyId(SURVEY_ID)
                        .legalPartnerId(LEGAL_PARTNER_ID)
                        .partnerId(PARTNER_ID)
                        .pvzMarketId(PVZ_MARKET_ID)
                        .pickupPointName(PICKUP_POINT_NAME)
                        .showDateTime(clock.instant())
                        .title(TITLE)
                        .body(BODY)
                        .url(FORM_URL)
                        .delegateEmail(DELEGATE_EMAIL)
                        .delegateName(DELEGATE_NAME)
                        .stTicket(TITLE)
                        .build()
        );
        String body = String.format(
                PICKUP_POINT_BODY_PATTERN, String.format(PICKUP_POINT_CABINET_URL_TEST, PVZ_MARKET_ID),
                PICKUP_POINT_NAME, BODY, FORM_URL
        );
        OwTicketDto expected = OwTicketDto.builder()
                .service(SERVICE)
                .title(String.format(PICKUP_POINT_TITLE_PATTERN, TITLE, PARTNER_ID, PICKUP_POINT_NAME))
                .clientEmail(DELEGATE_EMAIL)
                .partnerId(PARTNER_ID)
                .pupMarketId(PVZ_MARKET_ID)
                .stTicket(TITLE)
                .comment(
                        OwTicketDto.Comment.builder()
                                .body(body)
                                .metaClass(COMMENT_META_CLASS)
                                .build()
                )
                .build();
        assertThat(output).isEqualTo(expected);
    }

}
