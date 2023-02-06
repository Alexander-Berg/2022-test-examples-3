package ru.yandex.chemodan.app.psbilling.core.mail.sending;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingDBTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingGroupsFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.mail.EmailTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.dao.promos.PromoTemplateDao;
import ru.yandex.chemodan.app.psbilling.core.entities.mail.EmailTemplateEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.dataproviders.LocalizedEmailSenderDataProvider;
import ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders.EmailKeyDataProvider;
import ru.yandex.chemodan.app.psbilling.core.promos.PromoService;
import ru.yandex.misc.test.Assert;

public class LocalizedEmailParamsTest extends AbstractPsBillingDBTest {
    @Autowired
    private EmailTemplateDao emailTemplateDao;
    @Autowired
    PsBillingGroupsFactory groupsFactory;
    @Autowired
    PromoTemplateDao promoTemplateDao;
    @Autowired
    PromoService promoService;

    @Autowired
    LocalizedEmailSenderDataProvider localizedEmailSenderDataProvider;

    @Autowired
    List<EmailKeyDataProvider> emailKeyDataProviders;

    @Test
    public void allParamsCovered() {
        Set<String> allParams = new TreeSet<>();
        for (EmailTemplateEntity template : emailTemplateDao.findAll()) {
            allParams.addAll(template.getArgs());
        }
        for (EmailKeyDataProvider emailKeyDataProvider : emailKeyDataProviders) {
            emailKeyDataProvider.getAcceptKeys().forEach(allParams::remove);
        }
        Assert.assertTrue(allParams + " parameter is not covered by any emailKeyDataProvider", allParams.isEmpty());
    }
}
