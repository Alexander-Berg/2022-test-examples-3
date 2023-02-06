package ru.yandex.market.vendor;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.github.tomakehurst.wiremock.WireMockServer;
import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.cs.billing.balance.BalanceService;
import ru.yandex.cs.billing.balance.model.BalanceClient;
import ru.yandex.market.JettyFunctionalTest;
import ru.yandex.market.common.balance.xmlrpc.model.ClientStructure;
import ru.yandex.market.core.database.PreserveDictionariesDbUnitDataSet;
import ru.yandex.market.vendor.util.FunctionalTestHelper;
import ru.yandex.market.vendor.utils.StaticDomainAuthoritiesProvider;
import ru.yandex.vendor.documents.S3Connection;
import ru.yandex.vendor.security.Role;

import static net.javacrumbs.jsonunit.core.Option.IGNORING_ARRAY_ORDER;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.cs.billing.CsBillingCoreConstants.VENDOR_IDM_DOMAIN;

/**
 * Базовый класс для написания функциональных тестов в vendor-partner
 */
@SpringJUnitConfig(locations = "classpath:/ru/yandex/market/vendor/functional-test-config.xml")
@PreserveDictionariesDbUnitDataSet
public abstract class AbstractVendorPartnerFunctionalTest extends JettyFunctionalTest {
    protected static final Configuration JSON_ASSERT_CONFIG = JsonAssert.when(IGNORING_ARRAY_ORDER);
    private static final String SQL_INSERT_USER_ROLES = "" +
            "insert into vendors.user_roles " +
            "values (:userUid, :roleId, :vendorId, sysdate)";
    @Autowired
    private NamedParameterJdbcTemplate vendorNamedParameterJdbcTemplate;
    @Autowired
    private TransactionTemplate vendorTransactionTemplate;
    @Autowired
    private S3Connection s3Connection;

    @Autowired
    private BalanceService balanceService;

    @Autowired
    private WireMockServer reportMock;

    @Autowired
    private WireMockServer pricelabsMock;

    @Autowired
    private WireMockServer staffMock;

    @Autowired
    private WireMockServer blackboxMock;

    @Autowired
    public WireMockServer emailSenderMock;

    @Autowired
    private WireMockServer mbiBiddingMock;

    @Autowired
    private WireMockServer csBillingApiMock;

    @Autowired
    private WireMockServer abcMock;

    @Autowired
    private StaticDomainAuthoritiesProvider staticDomainAuthoritiesProvider;

    @BeforeEach
    void initS3() {
        AmazonS3 amazonS3 = mock(AmazonS3.class);
        when(amazonS3.listBuckets()).thenReturn(List.of());
        Bucket bucket = mock(Bucket.class);
        when(amazonS3.createBucket(anyString())).thenReturn(bucket);

        when(s3Connection.getS3()).thenReturn(amazonS3);
        S3Connection.S3Config config = new S3Connection.S3Config();
        config.setPublicUrlPattern("https://{bucket}.s3.mdst.yandex.net");
        when(s3Connection.getConfig()).thenReturn(config);

        resetWireMockServers();
    }

    @BeforeEach
    void resetStaticDomainAuthorities() {
        staticDomainAuthoritiesProvider.reset();
    }

    private void resetWireMockServers() {
        staffMock.resetAll();
        reportMock.resetAll();
        blackboxMock.resetAll();
        pricelabsMock.resetAll();
        mbiBiddingMock.resetAll();
        csBillingApiMock.resetAll();
        abcMock.resetAll();
    }

    protected void setVendorUserRoles(Collection<Role> roles, long uid) {
        if (roles.stream().map(Role::getId).anyMatch(id -> id >= 0)) {
            fail("Cannot set api role without vendorId.");
        }

        setVendorUserRoles(roles, uid, null);
    }

    protected void setVendorUserRoles(Collection<Role> roles, long uid, Long vendorId) {
        setVendorUserRolesForDomain(roles, uid, vendorId, VENDOR_IDM_DOMAIN);
    }

    private void setVendorUserRolesForDomain(Collection<Role> roles, long uid, Long vendorId, String domain) {
        for (Role role : roles) {
            if (role.getId() <= 0) {
                staticDomainAuthoritiesProvider.addRole(uid, role.toString());
            } else {
                vendorTransactionTemplate.execute(status -> vendorNamedParameterJdbcTemplate.update(
                        SQL_INSERT_USER_ROLES,
                        new MapSqlParameterSource()
                                .addValue("userUid", uid)
                                .addValue("vendorId", vendorId)
                                .addValue("roleId", role.getId())
                ));
            }
        }
    }

    protected void initBalanceServiceByClientId(int clientId) {
        doAnswer(invocation -> List.of(
                new BalanceClient(
                        new ClientStructure(
                                Map.ofEntries(
                                        Map.entry("CLIENT_ID", clientId),
                                        Map.entry("CLIENT_TYPE_ID", 2),
                                        Map.entry("NAME", "vasya"),
                                        Map.entry("EMAIL", "test@test"),
                                        Map.entry("PHONE", "12345678"),
                                        Map.entry("FAX", "87654321"),
                                        Map.entry("URL", "someurl"),
                                        Map.entry("CITY", "Leninburg"),
                                        Map.entry("REGION_ID", 1231231L)
                                )
                        )
                )
        )).when(balanceService).findClients(any());
    }

    public void assertHttpGet(String request, String expectedFile) {
        String actual = FunctionalTestHelper.get(baseUrl + request);
        String expected = getStringResource(expectedFile);
        JsonAssert.assertJsonEquals(expected, actual, JsonAssert.when(IGNORING_ARRAY_ORDER));
    }
}
