package ru.yandex.direct.core.entity.campaign.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
import ru.yandex.direct.core.entity.campaign.model.CampaignOpts;
import ru.yandex.direct.core.entity.campaign.model.DynamicCampaign;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.UidAndClientId;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.result.MassResult;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.common.util.RepositoryUtils.setToDb;
import static ru.yandex.direct.core.entity.campaign.converter.CampaignConverter.isUniversalFromDb;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaignWithSystemFields;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaignWithSystemFields;
import static ru.yandex.direct.dbschema.ppc.Tables.CAMPAIGNS;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignIsUniversalTest {@Autowired
    private Steps steps;

    @Autowired
    private CampaignTypedRepository campaignTypedRepository;

    @Autowired
    private CampaignOperationService campaignOperationService;

    @Autowired
    private DslContextProvider dslContextProvider;

    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void addUniversalTextCampaignSavedCorrectly() {
        Long id = addReturnId(defaultTextCampaign().withIsUniversal(true));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, Matchers.notNullValue());
        assertThat("is universal flag has to be appropriate", actualCampaign.getIsUniversal(),
                Matchers.is(true));
        String dbOpts = dslContextProvider.ppc(clientInfo.getShard())
                .select(CAMPAIGNS.CID, CAMPAIGNS.OPTS)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(id))
                .fetchOne(CAMPAIGNS.OPTS);
        boolean isUniversalDbValue = isUniversalFromDb(dbOpts);
        assertThat("is universal flag has to be appropriate in DB",
                isUniversalDbValue, Matchers.is(true));
    }

    @Test
    public void tryToAddUniversalDynamicCampaignUniversalityIgnored() {
        Long id = addReturnId(defaultDynamicCampaign().withIsUniversal(true));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        DynamicCampaign actualCampaign = (DynamicCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, Matchers.notNullValue());
        assertThat("is universal flag has to be appropriate", actualCampaign.getIsUniversal(),
                Matchers.is(false));
        String dbOpts = dslContextProvider.ppc(clientInfo.getShard())
                .select(CAMPAIGNS.CID, CAMPAIGNS.OPTS)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(id))
                .fetchOne(CAMPAIGNS.OPTS);
        boolean isUniversalDbValue = isUniversalFromDb(dbOpts);
        assertThat("is universal flag has to be appropriate in DB", isUniversalDbValue,
                Matchers.is(false));
    }

    @Test
    public void setUniversalFlagToTextCampaignSavedCorrectly() {
        Long id = addReturnId(defaultTextCampaign());
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(true, TextCampaign.IS_UNIVERSAL)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, Matchers.notNullValue());
        assertThat("is universal flag has to be appropriate", actualCampaign.getIsUniversal(),
                Matchers.is(true));
        String dbOpts = dslContextProvider.ppc(clientInfo.getShard())
                .select(CAMPAIGNS.CID, CAMPAIGNS.OPTS)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(id))
                .fetchOne(CAMPAIGNS.OPTS);
        boolean isUniversalDbValue = isUniversalFromDb(dbOpts);
        assertThat("is universal flag has to be appropriate in DB",
                isUniversalDbValue, Matchers.is(true));
    }

    @Test
    public void setUniversalFlagToDynamicCampaignUniversalityIgnored() {
        Long id = addReturnId(defaultDynamicCampaign());
        update(new ModelChanges<>(id, DynamicCampaign.class)
                .process(true, DynamicCampaign.IS_UNIVERSAL)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        DynamicCampaign actualCampaign = (DynamicCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, Matchers.notNullValue());
        assertThat("is universal flag has to be appropriate", actualCampaign.getIsUniversal(),
                Matchers.is(false));
        String dbOpts = dslContextProvider.ppc(clientInfo.getShard())
                .select(CAMPAIGNS.CID, CAMPAIGNS.OPTS)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(id))
                .fetchOne(CAMPAIGNS.OPTS);
        boolean isUniversalDbValue = isUniversalFromDb(dbOpts);
        assertThat("is universal flag has to be appropriate in DB",
                isUniversalDbValue, Matchers.is(false));
    }

    @Test
    public void universalDynamicCampaignInDbUniversalityIgnored() {
        Long id = addReturnId(defaultDynamicCampaign().withIsUniversal(true));
        dslContextProvider.ppc(clientInfo.getShard())
                .update(CAMPAIGNS)
                .set(CAMPAIGNS.OPTS, setToDb(Set.of(CampaignOpts.IS_UNIVERSAL), CampaignOpts::getTypedValue))
                .where(CAMPAIGNS.CID.eq(id))
                .execute();

        String dbOpts = dslContextProvider.ppc(clientInfo.getShard())
                .select(CAMPAIGNS.CID, CAMPAIGNS.OPTS)
                .from(CAMPAIGNS)
                .where(CAMPAIGNS.CID.eq(id))
                .fetchOne(CAMPAIGNS.OPTS);
        boolean isUniversalDbValue = isUniversalFromDb(dbOpts);
        assumeThat("is universal flag has to be appropriate in DB", isUniversalDbValue,
                Matchers.is(true));

        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        DynamicCampaign actualCampaign = (DynamicCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, Matchers.notNullValue());
        assertThat("is universal flag has to be appropriate", actualCampaign.getIsUniversal(),
                Matchers.is(false));
    }

    private TextCampaign defaultTextCampaign() {
        return defaultTextCampaignWithSystemFields(clientInfo)
                .withAgencyId(null)
                .withClientId(null)
                .withWalletId(null)
                .withIsServiceRequested(null)
                .withAttributionModel(CampaignAttributionModel.LAST_CLICK);
    }

    private DynamicCampaign defaultDynamicCampaign() {
        return defaultDynamicCampaignWithSystemFields()
                .withUid(clientInfo.getUid())
                .withClientId(clientInfo.getClientId().asLong())
                .withAgencyId(null)
                .withClientId(null)
                .withWalletId(null)
                .withIsServiceRequested(null)
                .withAttributionModel(CampaignAttributionModel.LAST_CLICK);
    }

    void update(ModelChanges<? extends BaseCampaign> modelChanges) {
        var options = new CampaignOptions();
        MassResult<Long> result = campaignOperationService.createRestrictedCampaignUpdateOperation(
                List.of(modelChanges), clientInfo.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()), options).apply();
        assertThat("campaign updated successfully", result.getSuccessfulCount(), Matchers.is(1));
    }

    Long addReturnId(BaseCampaign campaign) {
        MassResult<Long> result = campaignOperationService.createRestrictedCampaignAddOperation(List.of(campaign),
                clientInfo.getUid(), UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                new CampaignOptions()).prepareAndApply();
        assertThat("campaign added successfully", result.getSuccessfulCount(), Matchers.is(1));
        Long id = result.get(0).getResult();
        assertThat("campaign added successfully", id, Matchers.notNullValue());
        return id;
    }
}
