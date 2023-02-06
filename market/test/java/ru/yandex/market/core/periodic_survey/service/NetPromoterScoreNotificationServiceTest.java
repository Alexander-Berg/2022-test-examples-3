package ru.yandex.market.core.periodic_survey.service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.CampaignService;
import ru.yandex.market.core.contact.ContactService;
import ru.yandex.market.core.notification.context.impl.DualNotificationContext;
import ru.yandex.market.core.notification.service.NotificationSendContext;
import ru.yandex.market.core.notification.service.NotificationService;
import ru.yandex.market.core.partner.PartnerTypeAwareService;
import ru.yandex.market.core.periodic_survey.model.SurveyId;
import ru.yandex.market.core.periodic_survey.model.SurveyRecord;
import ru.yandex.market.core.periodic_survey.model.SurveyStatus;
import ru.yandex.market.core.periodic_survey.model.SurveyType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;

@DbUnitDataSet(before = "NetPromoterScoreNotificationServiceTest.before.csv")
class NetPromoterScoreNotificationServiceTest extends FunctionalTest {

    @Autowired
    private CampaignService campaignService;
    @Autowired
    private PartnerTypeAwareService partnerTypeAwareService;
    @Autowired
    private ContactService contactService;

    NotificationService notificationServiceMock;

    private NetPromoterScoreNotificationService netPromoterScoreNotificationService;

    @BeforeEach
    void setUp() {
        notificationServiceMock = Mockito.mock(NotificationService.class);
        netPromoterScoreNotificationService = new NetPromoterScoreNotificationService(
                campaignService,
                notificationServiceMock,
                partnerTypeAwareService,
                contactService
        );
    }

    @Test
    void notifyUser() {
        var surveys = Set.of(
                createSurveyRecord(104L, 1040L, SurveyType.NPS_DROPSHIP)
        );

        netPromoterScoreNotificationService.notifyUsers(surveys);

        ArgumentCaptor<NotificationSendContext> notificationContextCaptor =
                ArgumentCaptor.forClass(NotificationSendContext.class);


        Mockito.verify(notificationServiceMock).send(notificationContextCaptor.capture());

        var sendContext = notificationContextCaptor.getValue();
        assertThat(sendContext.getContext(), instanceOf(DualNotificationContext.class));
        DualNotificationContext notificationContext = (DualNotificationContext)sendContext.getContext();
        assertThat(notificationContext.getShopId(), equalTo(104L));
        assertThat(notificationContext.getUid(), equalTo(1040L));
        assertThat(sendContext.getRecipients().getToAddressList(), equalTo(List.of("1040@yandex.ru")));
    }

    @Test
    void notifyMultipleUsers() {
        var surveys = Set.of(
                createSurveyRecord(104L, 1030L, SurveyType.NPS_DROPSHIP),
                createSurveyRecord(104L, 1040L, SurveyType.NPS_DROPSHIP),
                createSurveyRecord(105L, 1050L, SurveyType.NPS_DBS)
        );

        netPromoterScoreNotificationService.notifyUsers(surveys);

        Mockito.verify(notificationServiceMock, Mockito.times(3)).send(Mockito.any());
    }

    @Test
    void noEmailsForUser() {
        var surveys = Set.of(
                createSurveyRecord(104L, 1020L, SurveyType.NPS_DROPSHIP)
        );

        netPromoterScoreNotificationService.notifyUsers(surveys);

        Mockito.verify(notificationServiceMock, Mockito.never()).send(Mockito.any());
    }

    @Test
    void noContactForUser() {
        var surveys = Set.of(
                createSurveyRecord(104L, 0xdeadL, SurveyType.NPS_DROPSHIP)
        );

        netPromoterScoreNotificationService.notifyUsers(surveys);

        Mockito.verify(notificationServiceMock, Mockito.never()).send(Mockito.any());
    }

    @Nonnull
    private SurveyRecord createSurveyRecord(long partnerId, long userId, SurveyType surveyType) {
        return SurveyRecord.newBuilder()
                .withSurveyId(SurveyId.of(partnerId, userId, surveyType, Instant.EPOCH))
                .withStatus(SurveyStatus.ACTIVE)
                .build();
    }
}
