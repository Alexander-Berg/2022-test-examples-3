package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.PaymentDeadlineKeyDataProvider;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.time.MoscowTime;

public class PaymentDeadlineKeyDataProviderTest extends AbstractPsBillingCoreTest {

    @Autowired
    private PaymentDeadlineKeyDataProvider keyDataProvider;

    @Test
    public void deadlineDate() {
        LocalDate date = new LocalDate(2021, 2, 14);

        MailContext context = createMailContextWithVoidDate(date);
        Assert.some("14 февраля 2021 года", keyDataProvider.getKeyData("payment_deadline", context));
    }

    @Test
    public void deadlineInThreeDays() {
        DateTime now = DateTime.now(MoscowTime.TZ);

        MailContext context = createMailContextWithVoidDate(now.plusDays(3).toLocalDate());

        DateUtils.freezeTime(now);
        Assert.some("3", keyDataProvider.getKeyData("days_for_payment", context));
    }

    private MailContext createMailContextWithVoidDate(LocalDate date) {
        return MailContext.builder()
                .language(Option.of(Language.RUSSIAN))
                .balanceVoidDate(Option.of(date))
                .build();
    }

    @Before
    public void initialize() {
        super.initialize();
        textsManagerMockConfig.reset();
    }
}
