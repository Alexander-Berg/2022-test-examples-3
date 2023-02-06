package ru.yandex.chemodan.app.psbilling.core.mail.keydataproviders;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.AbstractPsBillingDBTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingProductsFactory;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductPricesDao;
import ru.yandex.chemodan.app.psbilling.core.entities.CustomPeriodUnit;
import ru.yandex.chemodan.app.psbilling.core.entities.products.UserProductPriceEntity;
import ru.yandex.chemodan.app.psbilling.core.mail.MailContext;
import ru.yandex.chemodan.app.psbilling.core.mocks.TextsManagerMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.texts.TextsManager;
import ru.yandex.inside.utils.Language;
import ru.yandex.misc.test.Assert;

public class PriceDataKeyDataProviderTest extends AbstractPsBillingDBTest {

    @Autowired
    PriceDataKeyDataProvider provider;
    @Autowired
    PsBillingUsersFactory usersFactory;
    @Autowired
    PsBillingProductsFactory productsFactory;
    @Autowired
    UserProductPricesDao pricesDao;
    @Autowired
    TextsManagerMockConfiguration textsManagerMockConfiguration;
    @Autowired
    TextsManager textsManager;


    private UserProductPriceEntity userProductPrice;
    private final static int SUM = 100;

    @Before
    public void setup() {
        userProductPrice = productsFactory.createUserProductPrices(productsFactory.createUserProduct(),
                CustomPeriodUnit.ONE_MONTH,
                BigDecimal.valueOf(SUM));
        textsManagerMockConfiguration.reset();
        textsManager.updateTranslations();
    }

    //TODO may fail when run all tests. Looks like some kind of context pollution. Solo run is ok.
    @Test
    public void directCase() {
        MailContext context = MailContext.builder()
                .language(Option.of(Language.RUSSIAN))
                .userProductPriceId(Option.of(userProductPrice.getId())).build();

        String sum = provider.getKeyData("user_product_price", context).get();
        String currency = provider.getKeyData("user_product_currency", context).get();
        String period = provider.getKeyData("user_product_period", context).get();
        Assert.equals(sum, String.valueOf(SUM));
        Assert.equals(currency, "RUB");
        //this value is provided from tanker, test may fail if tanker settings changed
        //https://tanker.yandex-team.ru/project/disk-ps-billing/keyset/other_texts/key/month_period
        Assert.equals(period, "на месяц");

    }
}
