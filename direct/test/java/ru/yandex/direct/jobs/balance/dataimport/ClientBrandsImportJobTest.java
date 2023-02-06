package ru.yandex.direct.jobs.balance.dataimport;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.balance.client.BalanceClient;
import ru.yandex.direct.balance.client.BalanceXmlRpcClient;
import ru.yandex.direct.balance.client.BalanceXmlRpcClientConfig;
import ru.yandex.direct.balance.client.model.response.LinkedClientsItem;
import ru.yandex.direct.core.entity.client.model.ClientBrand;
import ru.yandex.direct.core.entity.client.service.ClientBrandsService;
import ru.yandex.direct.core.testing.info.ClientBrandInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.ClientBrandSteps;
import ru.yandex.direct.core.testing.steps.ClientSteps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.test.utils.MockedHttpWebServerExtention;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@JobsTest
@ExtendWith(SpringExtension.class)
class ClientBrandsImportJobTest {
    private static final Integer NEW_BRANDS_NUM = 30;
    private static final Integer KEEP_BRANDS_NUM = 10;
    private static final Integer OLD_BRANDS_NUM = 10;
    private static final String HTTP_PATH = "/xmlrpc";
    private static final String RESPONSE_TEMPLATE = "<?xml version='1.0'?>\n"
            + "<methodResponse>\n"
            + "<params>\n"
            + "<param>\n"
            + "<value><string>GROUP_ID\tCLIENT_ID\tLINK_TYPE\tBRAND_CLIENT_ID\n"
            + "%s\n"
            + "</string></value>\n"
            + "</param>\n"
            + "</params>\n"
            + "</methodResponse>";
    private static final String RESPONSE_ITEM_TEMPLATE = "261567\t%d\t7\t%d\n";

    @Autowired
    private ClientSteps clientSteps;

    @Autowired
    private ClientBrandSteps clientBrandSteps;

    @Autowired
    private ClientBrandsService service;

    @Autowired
    private ShardHelper shardHelper;

    private ClientBrandsImportJob clientBrandsImportJob;

    @RegisterExtension
    static MockedHttpWebServerExtention server = new MockedHttpWebServerExtention(ContentType.APPLICATION_XML);

    @BeforeEach
    void before() throws MalformedURLException {
        BalanceXmlRpcClientConfig config = new BalanceXmlRpcClientConfig(new URL(server.getServerURL() + HTTP_PATH));
        BalanceXmlRpcClient balanceXmlRpcClient = new BalanceXmlRpcClient(config);
        BalanceClient balanceClient = new BalanceClient(balanceXmlRpcClient, null);
        clientBrandsImportJob = new ClientBrandsImportJob(service, balanceClient, shardHelper);

        service.deleteEntriesOlderThanDateTime(LocalDateTime.now());
    }

    @Test
    void testJob() {
        List<ClientBrand> clientBrandsToKeep = new ArrayList<>();
        List<ClientBrand> clientBrandsInRequest = new ArrayList<>();

        for (int i = 0; i < OLD_BRANDS_NUM; i++) {
            clientBrandSteps
                    .createClientBrand(LocalDateTime.now().minus(ClientBrandsImportJob.BRAND_TTL).minusHours(1)
                            .truncatedTo(SECONDS));
        }

        for (int i = 0; i < KEEP_BRANDS_NUM; i++) {
            ClientBrandInfo clientBrandInfo = clientBrandSteps
                    .createClientBrand(LocalDateTime.now().minus(ClientBrandsImportJob.BRAND_TTL).plusHours(1)
                            .truncatedTo(SECONDS));
            clientBrandsToKeep.add(clientBrandInfo.getClientBrand());
        }

        for (int i = 0; i < NEW_BRANDS_NUM; i++) {
            ClientInfo clientInfo = clientSteps.createDefaultClient();
            ClientInfo brandClientInfo = clientSteps.createDefaultClient();
            ClientBrand brand = new ClientBrand()
                    .withClientId(clientInfo.getClientId().asLong())
                    .withBrandClientId(brandClientInfo.getClientId().asLong());
            clientBrandsInRequest.add(brand);
            clientBrandsToKeep.add(brand);
        }

        StringBuilder responseData = new StringBuilder();
        for (ClientBrand brand : clientBrandsInRequest) {
            responseData.append(String.format(RESPONSE_ITEM_TEMPLATE, brand.getClientId(), brand.getBrandClientId()));
        }

        server.addResponse(HTTP_PATH, String.format(RESPONSE_TEMPLATE, responseData.toString()));
        clientBrandsImportJob.execute();

        List<ClientBrand> clientBrandsInDatabase =
                service.getEntriesOlderThanDateTime(LocalDateTime.now().plusMinutes(1));
        assertThat("В базу добавлены новые и оставлены старые с достаточным TTL",
                mapList(clientBrandsInDatabase, cb -> cb.withLastSync(null)),
                containsInAnyOrder(mapList(clientBrandsToKeep, cb -> beanDiffer(cb.withLastSync(null)))));
    }

    @Test
    void testConvertion() {
        List<ClientBrand> clientBrands = clientBrandsImportJob.convertResponseDataToModel(Arrays.asList(
                new LinkedClientsItem().withClientId(41L).withBrandClientId(43L),
                new LinkedClientsItem().withClientId(42L).withBrandClientId(43L),
                new LinkedClientsItem().withClientId(43L).withBrandClientId(43L),
                new LinkedClientsItem().withClientId(41L).withBrandClientId(43L),
                new LinkedClientsItem().withClientId(42L).withBrandClientId(43L),
                new LinkedClientsItem().withClientId(10L).withBrandClientId(20L),
                new LinkedClientsItem().withClientId(11L).withBrandClientId(21L)
        ));
        assertThat("Список не пустой", clientBrands.size(), greaterThan(0));
        LocalDateTime syncTime = clientBrands.get(0).getLastSync();
        assertThat("Получили корректный список", clientBrands,
                containsInAnyOrder(Arrays.asList(
                        beanDiffer(new ClientBrand().withClientId(41L).withBrandClientId(43L).withLastSync(syncTime)),
                        beanDiffer(new ClientBrand().withClientId(42L).withBrandClientId(43L).withLastSync(syncTime)),
                        beanDiffer(new ClientBrand().withClientId(43L).withBrandClientId(43L).withLastSync(syncTime)),
                        beanDiffer(new ClientBrand().withClientId(10L).withBrandClientId(20L).withLastSync(syncTime)),
                        beanDiffer(new ClientBrand().withClientId(11L).withBrandClientId(21L).withLastSync(syncTime))
                )));
    }

    @Test
    public void getValidClientsTest_negativeClientIdFileterd() {
        ClientBrand clientBrand = new ClientBrand()
                .withBrandClientId(-1L)
                .withClientId(10L)
                .withLastSync(LocalDateTime.now());
        assertThat(clientBrandsImportJob.filterNonexistanceClients(singletonList(clientBrand)), empty());
    }

    @Test
    public void getValidClientsTest_negativeBrandClientIdFileterd() {
        ClientBrand clientBrand = new ClientBrand()
                .withClientId(1L)
                .withBrandClientId(-10L)
                .withLastSync(LocalDateTime.now());
        assertThat(clientBrandsImportJob.filterNonexistanceClients(singletonList(clientBrand)), empty());
    }

    @Test
    public void getValidClientsTest_nullClientIdFileterd() {
        ClientBrand clientBrand = new ClientBrand()
                .withClientId(null)
                .withBrandClientId(10L)
                .withLastSync(LocalDateTime.now());
        assertThat(clientBrandsImportJob.filterNonexistanceClients(singletonList(clientBrand)), empty());
    }

    @Test
    public void getValidClientsTest_nullBrandClientIdFileterd() {
        ClientBrand clientBrand = new ClientBrand()
                .withClientId(1L)
                .withBrandClientId(null)
                .withLastSync(LocalDateTime.now());
        assertThat(clientBrandsImportJob.filterNonexistanceClients(singletonList(clientBrand)), empty());
    }

    @Test
    public void getValidClientsTest_validClientIdNotInDb() {
        ClientBrand clientBrand = new ClientBrand()
                .withClientId(Long.MAX_VALUE)
                .withBrandClientId(Long.MAX_VALUE)
                .withLastSync(LocalDateTime.now());
        assertThat(clientBrandsImportJob.filterNonexistanceClients(singletonList(clientBrand)), empty());
    }

    @Test
    public void getValidClientsTest_validClientIdInDb() {
        ClientInfo clientInfo = clientSteps.createDefaultClient();
        ClientBrand clientBrand = new ClientBrand()
                .withClientId(clientInfo.getClientId().asLong())
                .withBrandClientId(clientInfo.getClientId().asLong())
                .withLastSync(LocalDateTime.now());
        assertThat(clientBrandsImportJob.filterNonexistanceClients(singletonList(clientBrand)),
                contains(clientBrand));
    }
}
