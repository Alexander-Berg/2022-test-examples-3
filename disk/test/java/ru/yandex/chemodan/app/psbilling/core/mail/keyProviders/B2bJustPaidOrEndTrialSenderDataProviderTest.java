package ru.yandex.chemodan.app.psbilling.core.mail.keyProviders;

import java.math.BigDecimal;
import java.util.Currency;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingCoreTest;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerKeyEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerTranslationEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.GroupServiceData;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mail.dataproviders.B2bJustPaidOrEndTrialSenderDataProvider;
import ru.yandex.chemodan.app.psbilling.core.mail.dataproviders.model.SenderContext;
import ru.yandex.chemodan.app.psbilling.core.mocks.BalanceClientStub;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.texts.TankerTranslation;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.balanceclient.model.response.FindClientResponseItem;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

import static org.mockito.Mockito.doAnswer;

public class B2bJustPaidOrEndTrialSenderDataProviderTest extends AbstractPsBillingCoreTest {

    @Autowired
    public B2bJustPaidOrEndTrialSenderDataProvider provider;
    @Autowired
    public BalanceClientStub balanceClientStub;

    @Test
    public void noFeatureValues() {
        textsManagerMockConfig.reset();
        TankerKeyEntity deskKey = psBillingTextsFactory.create();
        TankerKeyEntity valueKey = psBillingTextsFactory.create2();

        doAnswer(x -> new TankerTranslation(x.getArgument(0),
                Cf.list(new TankerTranslationEntity(x.getArgument(0), "ru", "Описание"))))
                .when(textsManagerMockConfig.getMock()).findTranslation(Mockito.eq(deskKey.getId()));
        doAnswer(x -> new TankerTranslation(x.getArgument(0),
                Cf.list(new TankerTranslationEntity(x.getArgument(0), "ru", "Значение"))))
                .when(textsManagerMockConfig.getMock()).findTranslation(Mockito.eq(valueKey.getId()));

        long clientId = 1;

        balanceClientStub.turnOnMockitoForMethod("findClient");
        Mockito.when(balanceClientStub.getBalanceClientMock().findClient(Mockito.any()))
                .thenReturn(Cf.list(new FindClientResponseItem()));
        PassportUid uid = PassportUid.cons(111);

        Group group = psBillingGroupsFactory.createGroup(x -> x.paymentInfo(new BalancePaymentInfo(clientId, uid)));
        GroupProduct groupProduct = psBillingProductsFactory.createGroupProduct();
        FeatureEntity feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
        psBillingProductsFactory.createProductFeature(groupProduct.getUserProduct().getId(), feature,
                x -> x.descriptionTankerKeyId(Option.of(deskKey.getId())).valueTankerKeyId(Option.of(valueKey.getId())));
        GroupService groupService = psBillingGroupsFactory.createGroupService(group, groupProduct);

        psBillingBalanceFactory.createContractBalance(
                psBillingBalanceFactory.createContract(clientId, 2L, Currency.getInstance("RUB")),
                x -> x.withActSum(BigDecimal.valueOf(100))
                        .withFirstDebtAmount(BigDecimal.valueOf(100))
                        .withFirstDebtPaymentTermDT(DateUtils.futureDate()));

        MailContext mailContext = MailContext.builder()
                .to(uid)
                .groupIds(Cf.list(group.getId().toString()))
                .groupServices(Cf.list(GroupServiceData.fromGroupService(groupService)))
                .userServiceId(Option.empty())
                .language(Option.of(Language.RUSSIAN))
                .build();

        SenderContext context = provider.buildSenderContext(mailContext).get();
        @SuppressWarnings("unchecked")
        ListF<String> features = (ListF<String>) context.getArgs().getTs("features");

        Assert.equals(1, features.size());
        Assert.equals("Описание", features.get(0));
    }
}
