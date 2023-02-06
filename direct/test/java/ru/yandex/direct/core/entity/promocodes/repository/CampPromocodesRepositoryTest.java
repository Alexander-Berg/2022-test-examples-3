package ru.yandex.direct.core.entity.promocodes.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.assertj.core.api.SoftAssertions;
import org.jooq.Record3;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.promocodes.model.CampPromocodes;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.utils.JsonUtils;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFieldsExcept;
import static ru.yandex.direct.core.testing.data.TestPromocodeInfo.createPromocodeInfo;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMP_PROMOCODES;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampPromocodesRepositoryTest {
    private static final int TEST_SHARD = 2;
    private static final String TEST_DOMAIN = "test.domain.org";
    private static final CompareStrategy EXCEPT_LAST_CHANGE = allFieldsExcept(newPath("lastChange"));

    @Autowired
    private CampPromocodesRepository repository;

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private Steps steps;

    private Long campaignId;

    @Before
    public void createTestData() {
        campaignId =
                steps.campaignSteps().createActiveTextCampaign(new ClientInfo().withShard(TEST_SHARD)).getCampaignId();
    }

    @Test
    public void addGetCampaignPromocodesSmokeTest() {
        assumeThat("record should not exists before added", repository.getCampaignPromocodes(TEST_SHARD, campaignId),
                nullValue());

        CampPromocodes campPromocodes = testData();
        repository.addCampaignPromocodes(TEST_SHARD, campPromocodes);
        assertEquals(campPromocodes, repository.getCampaignPromocodes(TEST_SHARD, campaignId));
    }

    @Test
    public void addCampaignPromocodesTest() {
        assumeThat("record should not exists before added", repository.getCampaignPromocodes(TEST_SHARD, campaignId),
                nullValue());

        CampPromocodes campPromocodes = testData();
        repository.addCampaignPromocodes(TEST_SHARD, campPromocodes);

        Record3<String, String, LocalDateTime> row = dslContextProvider.ppc(TEST_SHARD)
                .select(CAMP_PROMOCODES.PROMOCODES_INFO, CAMP_PROMOCODES.RESTRICTED_DOMAIN, CAMP_PROMOCODES.LAST_CHANGE)
                .from(CAMP_PROMOCODES)
                .where(CAMP_PROMOCODES.CID.eq(campaignId)).fetchOne();

        SoftAssertions soft = new SoftAssertions();
        JsonNode jsonPromocodesList = JsonUtils.fromJson(row.get(CAMP_PROMOCODES.PROMOCODES_INFO));
        soft.assertThat(jsonPromocodesList).isNotNull();
        soft.assertThat(jsonPromocodesList.getNodeType()).isEqualTo(JsonNodeType.ARRAY);
        soft.assertThat(jsonPromocodesList.size()).isEqualTo(campPromocodes.getPromocodes().size());

        PromocodeInfo expectedPromocode = campPromocodes.getPromocodes().get(0);
        JsonNode jsonPromocode = jsonPromocodesList.get(0);
        soft.assertThat(jsonPromocode).isNotNull();
        soft.assertThat(jsonPromocode.getNodeType()).isEqualTo(JsonNodeType.OBJECT);
        soft.assertThat(jsonPromocode.get("promocode_id").asLong()).isEqualTo(expectedPromocode.getId());
        soft.assertThat(jsonPromocode.get("promocode").asText()).isEqualTo(expectedPromocode.getCode());
        soft.assertThat(jsonPromocode.get("invoice_id").asLong()).isEqualTo(expectedPromocode.getInvoiceId());
        soft.assertThat(jsonPromocode.get("invoice_eid").asText()).isEqualTo(expectedPromocode.getInvoiceExternalId());
        soft.assertThat(jsonPromocode.get("invoice_enabled_at").getNodeType()).isEqualTo(JsonNodeType.STRING);

        soft.assertThat(row.get(CAMP_PROMOCODES.RESTRICTED_DOMAIN)).isEqualTo(campPromocodes.getRestrictedDomain());
        soft.assertThat(row.get(CAMP_PROMOCODES.LAST_CHANGE)).isEqualTo(campPromocodes.getLastChange());
        soft.assertAll();
    }

    @Test
    public void getCampaignPromocodesTest() {
        assumeThat("record should not exists before added", repository.getCampaignPromocodes(TEST_SHARD, campaignId),
                nullValue());

        String promocodesList = "[{\"promocode\": \"HbNEzPvpHv\""
                + ", \"invoice_id\": 8443042388574547050"
                + ", \"invoice_eid\": \"Б-2035872421-125\""
                + ", \"promocode_id\": 5528427603952989647"
                + ", \"invoice_enabled_at\": \"2018-09-17 15:32:55\"}]";
        LocalDateTime lastChange = LocalDateTime.of(2018, 9, 17, 16, 8, 5);

        dslContextProvider.ppc(TEST_SHARD)
                .insertInto(CAMP_PROMOCODES)
                .set(CAMP_PROMOCODES.CID, campaignId)
                .set(CAMP_PROMOCODES.PROMOCODES_INFO, promocodesList)
                .set(CAMP_PROMOCODES.RESTRICTED_DOMAIN, TEST_DOMAIN)
                .set(CAMP_PROMOCODES.LAST_CHANGE, lastChange)
                .execute();

        CampPromocodes expected = new CampPromocodes()
                .withCampaignId(campaignId)
                .withRestrictedDomain(TEST_DOMAIN)
                .withLastChange(lastChange)
                .withPromocodes(Collections.singletonList(new PromocodeInfo()
                        .withId(5528427603952989647L)
                        .withCode("HbNEzPvpHv")
                        .withInvoiceId(8443042388574547050L)
                        .withInvoiceExternalId("Б-2035872421-125")
                        .withInvoiceEnabledAt(LocalDateTime.of(2018, 9, 17, 15, 32, 55))
                ));
        assertEquals(expected, repository.getCampaignPromocodes(TEST_SHARD, campaignId));
    }

    @Test
    public void deleteCampaignPromocodesTest() {
        CampPromocodes campPromocodes = testData();
        repository.addCampaignPromocodes(TEST_SHARD, campPromocodes);

        assumeThat("record should exists before deleted", repository.getCampaignPromocodes(TEST_SHARD, campaignId),
                notNullValue());

        repository.deleteCampaignPromocodes(TEST_SHARD, campaignId);
        assertNull(repository.getCampaignPromocodes(TEST_SHARD, campaignId));
    }

    @Test
    public void updateCampaignPromocodesList_AddPromocode_Test() {
        CampPromocodes campPromocodes = testData()
                .withLastChange(LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS));
        repository.addCampaignPromocodes(TEST_SHARD, campPromocodes);


        List<PromocodeInfo> newPromocodes = new ArrayList<>(campPromocodes.getPromocodes());
        newPromocodes.add(createPromocodeInfo());
        newPromocodes.add(createPromocodeInfo());

        repository.updateCampaignPromocodesList(TEST_SHARD, campaignId, newPromocodes);

        CampPromocodes expected = new CampPromocodes()
                .withCampaignId(campPromocodes.getCampaignId())
                .withRestrictedDomain(campPromocodes.getRestrictedDomain())
                .withPromocodes(newPromocodes);

        CampPromocodes actual = repository.getCampaignPromocodes(TEST_SHARD, campaignId);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCEPT_LAST_CHANGE)));
        soft.assertThat(actual.getLastChange()).isAfter(campPromocodes.getLastChange());
        soft.assertAll();
    }

    @Test
    public void updateCampaignPromocodesList_RemovePromocode_Test() {
        CampPromocodes campPromocodes = testData()
                .withLastChange(LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS));
        repository.addCampaignPromocodes(TEST_SHARD, campPromocodes);


        repository.updateCampaignPromocodesList(TEST_SHARD, campaignId, Collections.emptyList());

        CampPromocodes expected = new CampPromocodes()
                .withCampaignId(campPromocodes.getCampaignId())
                .withRestrictedDomain(campPromocodes.getRestrictedDomain())
                .withPromocodes(Collections.emptyList());

        CampPromocodes actual = repository.getCampaignPromocodes(TEST_SHARD, campaignId);

        SoftAssertions soft = new SoftAssertions();
        soft.assertThat(actual).is(matchedBy(beanDiffer(expected).useCompareStrategy(EXCEPT_LAST_CHANGE)));
        soft.assertThat(actual.getLastChange()).isAfter(campPromocodes.getLastChange());
        soft.assertAll();
    }

    private CampPromocodes testData() {
        return new CampPromocodes()
                .withCampaignId(campaignId)
                .withPromocodes(Collections.singletonList(createPromocodeInfo()))
                .withRestrictedDomain(TEST_DOMAIN)
                .withLastChange(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
    }
}
