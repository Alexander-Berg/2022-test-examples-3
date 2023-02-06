package ru.yandex.chemodan.app.psbilling.core.mail.sending;

import lombok.SneakyThrows;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.users.UserServiceEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.EventMailType;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mocks.BazingaTaskManagerMock;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductPrice;
import ru.yandex.chemodan.app.psbilling.core.tasks.execution.TaskScheduler;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class SendTransactionMailTest extends BaseEmailSendTest {
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    BazingaTaskManagerMock bazingaTaskManagerMock;
    @Autowired
    TaskScheduler taskScheduler;

    @Test
    @SneakyThrows
    public void simpleSend() {
        String uid = PsBillingUsersFactory.DEFAULT_UID;
        UserProductEntity userProduct = psBillingProductsFactory.createUserProduct(x ->
                x.productOwnerId(psBillingProductsFactory.getOrCreateProductOwner("yandex_mail").getId()));
        UserProductPrice price = userProductManager.findPrice(
                psBillingProductsFactory.createUserProductPrices(userProduct, CustomPeriodUnit.TEN_MINUTES).getId());
        UserServiceEntity userService = psBillingUsersFactory.createUserService(userProduct.getId(),
                x -> x.userProductPriceId(Option.of(price.getId())).nextCheckDate(Option.of(DateUtils.farFutureDate())));

        MailContext mailContext = MailContext.builder()
                .to(PassportUid.cons(Long.parseLong(uid)))
                .userServiceId(Option.of(userService.getId().toString()))
                .groupIds(Cf.list())
                .groupServices(Cf.list())
                .language(Option.empty())
                .build();

        taskScheduler.scheduleTransactionalEmailTask(EventMailType.JUST_PAID, mailContext);
        bazingaTaskManagerMock.executeTasks(applicationContext);

        HttpUriRequest request = mailSenderMockConfig.verifyEmailSent();
        Assert.assertContains(request.getURI().toString(), "8YPXBSN3-WHQ");
    }

    @Before
    public void setup() {
        mailSenderMockConfig.mockConfigToEnabled();
    }
}
