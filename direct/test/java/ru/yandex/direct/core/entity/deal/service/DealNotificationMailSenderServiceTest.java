package ru.yandex.direct.core.entity.deal.service;

import java.util.GregorianCalendar;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.deal.model.Deal;
import ru.yandex.direct.core.entity.logmail.service.LogMailService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.mail.MailMessage;
import ru.yandex.direct.mail.MailSender;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ParametersAreNonnullByDefault
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DealNotificationMailSenderServiceTest {

    private final String mdsUrl = "http://localhost/get-direct-files/1.pdf";
    private final String email = "at-tester@yandex.ru";
    private final String notificationId = "20180101-123456";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

    @Mock
    private MailSender mailSender;

    @Autowired
    private LogMailService logMailService;

    @Before
    public void setUp() throws Exception {
        DealNotificationMailSenderService mailSenderService =
                new DealNotificationMailSenderService(mailSender, logMailService);

        mailSenderService.sendDealNotificationEmail(new DealNotificationEmailParameters.Builder()
                .setDeal((Deal) new Deal().withId(472L))
                .setNotificationId(notificationId)
                .setContractId("2018/01")
                .setContractDate(new GregorianCalendar(2018, 1, 1))
                .setContractAdditionalAgreementId("03")
                .setPdfFileName("1.pdf")
                .setSignedPdfContent(new byte[]{5, 6, 7})
                .setEmailTo(email)
                .build(), mdsUrl);
    }

    @Test
    public void sendDealNotificationEmail_EmailIsSent() {
        verify(mailSender).send(any(MailMessage.class));
        verifyNoMoreInteractions(mailSender);
    }

    // Раньше здесь было несколько тестов, читающих логи писем из ppclog. Были удалены с удалением записи в ppclog: DIRECT-116162
    // Насколько этот класс имеет смысл без них?
}
