package ru.yandex.chemodan.app.psbilling.core;

import java.util.Currency;

import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.psbilling.core.config.PsBillingCoreMocksConfig;
import ru.yandex.chemodan.app.psbilling.core.config.Settings;
import ru.yandex.chemodan.app.psbilling.core.config.featureflags.FeatureFlags;
import ru.yandex.chemodan.app.psbilling.core.dao.products.UserProductBucketDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.OrderDao;
import ru.yandex.chemodan.app.psbilling.core.mocks.Blackbox2MockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.MailSenderMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.mocks.TextsManagerMockConfiguration;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.users.UserInfoService;
import ru.yandex.chemodan.app.psbilling.core.users.UserServiceManager;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PaymentFactory;
import ru.yandex.chemodan.app.psbilling.core.utils.factories.PsBillingPromoFactory;
import ru.yandex.chemodan.balanceclient.BalanceXmlRpcClientConfig;
import ru.yandex.chemodan.zk.registries.tvm.TvmDstClientsZkRegistry;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;

@SqlConfig(dataSource = "psBillingDataSource")
@TestExecutionListeners({AbstractPsBillingCoreTest.TruncateDbExecutionListener.class,
        SqlScriptsTestExecutionListener.class})
public abstract class AbstractPsBillingCoreTest extends AbstractPsBillingDBTest {
    protected final Currency rub = Currency.getInstance("RUB");
    protected final Currency usd = Currency.getInstance("USD");
    protected final PassportUid uid = PassportUid.cons(123);
    protected final Option<PassportUid> uidO = Option.of(uid);

    protected static final String PROMO_PAYLOAD_TYPE_WEB_DISK = "web_disk";
    protected static final String PROMO_PAYLOAD_TYPE_WEB_MAIL = "web_mail";
    protected static final String PROMO_PAYLOAD_TYPE_WEB_TUNING = "web_tuning";
    protected static final String PROMO_PAYLOAD_TYPE_MOBILE = "mobile";

    @Autowired
    protected PsBillingCoreMocksConfig psBillingCoreMocksConfig;
    @Autowired
    protected PsBillingGroupsFactory psBillingGroupsFactory;
    @Autowired
    protected PsBillingBalanceFactory psBillingBalanceFactory;
    @Autowired
    protected PsBillingUsersFactory psBillingUsersFactory;
    @Autowired
    protected PsBillingTextsFactory psBillingTextsFactory;
    @Autowired
    protected PsBillingProductsFactory psBillingProductsFactory;
    @Autowired
    protected PsBillingOrdersFactory psBillingOrdersFactory;
    @Autowired
    protected PsBillingTransactionsFactory psBillingTransactionsFactory;
    @Autowired
    protected PaymentFactory paymentFactory;
    @Autowired
    protected PsBillingPromoFactory psBillingPromoFactory;
    @Autowired
    private TvmDstClientsZkRegistry tvmDstClientsZkRegistry;
    @Autowired
    private BalanceXmlRpcClientConfig balanceXmlRpcClientConfig;
    @Value("${balance.tvm-client-id}")
    private Integer balanceTvmClientId;
    @Autowired
    protected UserInfoService userInfoService;
    @Autowired
    protected UserProductManager userProductManager;
    @Autowired
    protected UserServiceManager userServiceManager;
    @Autowired
    protected OrderDao orderDao;
    @Autowired
    protected UserProductBucketDao userProductBucketDao;

    @Autowired
    protected Blackbox2MockConfiguration blackbox2MockConfig;
    @Autowired
    protected TextsManagerMockConfiguration textsManagerMockConfig;
    @Autowired
    protected MailSenderMockConfiguration mailSenderMockConfig;
    @Autowired
    protected Settings settings;
    @Autowired
    protected FeatureFlags featureFlags;
    @Autowired
    protected JdbcTemplate3 jdbcTemplate;

    @Before
    public void initialize() {
        DateUtils.freezeTime();
        tvmDstClientsZkRegistry.putIfAbsent(new TvmDstClientsZkRegistry.TvmDstClientInfo(
                balanceXmlRpcClientConfig.getServerUrl().getHost(), Option.of(balanceTvmClientId), false));
    }

    public static class TruncateDbExecutionListener extends AbstractTestExecutionListener {
        @Override
        public void beforeTestMethod(TestContext testContext) {
            PreparedDbProvider.truncateDatabases("public");
        }

        @Override
        public int getOrder() {
            return 4000;
        }
    }
}
