package ru.yandex.direct.core.entity.campaign.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampAimType;
import ru.yandex.direct.core.entity.campaign.model.CampaignAttributionModel;
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

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultDynamicCampaign;
import static ru.yandex.direct.core.testing.data.TestCampaigns.defaultTextCampaign;
import static ru.yandex.direct.dbschema.ppc.tables.CampAdditionalData.CAMP_ADDITIONAL_DATA;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CampaignAdditionalDataTableSupportTest {
    private static final String HREF_FIRST = "https://ya.ru";
    private static final String HREF_SECOND = "https://ay.ru";
    private static final String COMPANY_NAME_FIRST = "Рога и копыта";
    private static final String COMPANY_NAME_SECOND = "Копыта и рога";
    private static final String BUSINESS_CATEGORY_FIRST = "Ферма";
    private static final String BUSINESS_CATEGORY_SECOND = "Кино";

    @Autowired
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
    public void addCampaignWithHrefAndAimSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void addCampaignWitoutHrefAndAimNoTableEntry() {
        Long id = addReturnId(defaultCampaign());
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("row has to be absent", isRowNotPresent(id), is(true));
    }

    @Test
    public void addCampaignWithAimNoHrefSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withCampAimType(CampAimType.PHONE_CALLS));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void addCampaignWithHrefNoAimSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
    }

    @Test
    public void addCampaignWithAimHrefNameCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withHref(HREF_FIRST)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_FIRST));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_FIRST));
    }

    @Test
    public void addCampaignWithAimHrefNameNoCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withHref(HREF_FIRST)
                .withCompanyName(COMPANY_NAME_FIRST));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_FIRST));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                nullValue());
    }

    @Test
    public void addCampaignWithNoAimHrefNameCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_FIRST));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_FIRST));
    }

    @Test
    public void updateNameCampaignWithHrefAndAimFieldsUnchanged() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process("some awesome name", TextCampaign.NAME)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void updateDefaultCampaignHrefAndAimAdded() {
        Long id = addReturnId(defaultCampaign());
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_FIRST, TextCampaign.HREF)
                .process(CampAimType.PHONE_CALLS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void updateDefaultCampaignHrefAddedAimIsNull() {
        Long id = addReturnId(defaultCampaign());
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_FIRST, TextCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
    }

    @Test
    public void updateDefaultCampaignAimAddedHrefIsNull() {
        Long id = addReturnId(defaultCampaign());
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(CampAimType.PHONE_CALLS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void deleteHrefAndAimTableEntryDeleted() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.HREF)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("row has to be deleted", isRowNotPresent(id), is(true));
    }

    @Test
    public void deleteHrefNoAimTableEntryDeleted() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("row has to be deleted", isRowNotPresent(id), is(true));
    }

    @Test
    public void deleteAimNoHrefTableEntryDeleted() {
        Long id = addReturnId(defaultCampaign().withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("row has to be deleted", isRowNotPresent(id), is(true));
    }

    @Test
    public void deleteNameCategoryTableEntryDeleted() {
        Long id = addReturnId(defaultCampaign()
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .process(null, TextCampaign.COMPANY_NAME)
                .process(null, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                nullValue());
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                nullValue());
        assertThat("row has to be deleted", isRowNotPresent(id), is(true));
    }

    @Test
    public void deleteNameCategoryAimLeftTableEntryNotDeleted() {
        Long id = addReturnId(defaultCampaign()
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.COMPANY_NAME)
                .process(null, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(), nullValue());
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                nullValue());
        assertThat("row has not to be deleted", isRowNotPresent(id), is(false));
    }

    @Test
    public void deleteHrefAimPresentTableEntryNotDeleted() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void deleteAimHrefPresentTableEntryNotDeleted() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
    }

    @Test
    public void deleteHrefUpdateAimSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.HREF)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
    }

    @Test
    public void deleteAimUpdateHrefSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .process(HREF_SECOND, TextCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
    }

    @Test
    public void updateHrefDeleteAimSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
    }

    @Test
    public void updateHrefAndNameDeleteAimAndCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(COMPANY_NAME_SECOND, TextCampaign.COMPANY_NAME)
                .process(null, TextCampaign.CAMP_AIM_TYPE)
                .process(null, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_SECOND));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                nullValue());
    }

    @Test
    public void updateAimCategoryHrefDeleteNameSavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(null, TextCampaign.COMPANY_NAME)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .process(BUSINESS_CATEGORY_SECOND, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(), nullValue());
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_SECOND));
    }

    @Test
    public void updateAimCategoryHrefNameSavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(COMPANY_NAME_SECOND, TextCampaign.COMPANY_NAME)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .process(BUSINESS_CATEGORY_SECOND, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_SECOND));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_SECOND));
    }

    @Test
    public void updateAimHrefAddNameCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(COMPANY_NAME_SECOND, TextCampaign.COMPANY_NAME)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .process(BUSINESS_CATEGORY_SECOND, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_SECOND));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_SECOND));
    }

    @Test
    public void updateAimHrefNameUnchangeCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(COMPANY_NAME_SECOND, TextCampaign.COMPANY_NAME)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_SECOND));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_FIRST));
    }

    @Test
    public void updateAimHrefCategoryUnchangeNameSavedCorrectly() {
        Long id = addReturnId(defaultCampaign()
                .withHref(HREF_FIRST)
                .withCampAimType(CampAimType.PHONE_CALLS)
                .withCompanyName(COMPANY_NAME_FIRST)
                .withBusinessCategory(BUSINESS_CATEGORY_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(BUSINESS_CATEGORY_SECOND, TextCampaign.BUSINESS_CATEGORY)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_FIRST));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_SECOND));
    }

    @Test
    public void updateAimDeleteHrefSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(null, TextCampaign.HREF)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
    }

    @Test
    public void updateHrefSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void updateAimSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST).withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
    }

    @Test
    public void updateHrefAddAimSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(CampAimType.PHONE_CALLS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.PHONE_CALLS));
    }

    @Test
    public void updateHrefAddCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(BUSINESS_CATEGORY_SECOND, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(), nullValue());
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                is(BUSINESS_CATEGORY_SECOND));
    }

    @Test
    public void updateHrefAddNameDeleteCategorySavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_SECOND, TextCampaign.HREF)
                .process(COMPANY_NAME_SECOND, TextCampaign.COMPANY_NAME)
                .process(null, TextCampaign.BUSINESS_CATEGORY)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_SECOND));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(), nullValue());
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_SECOND));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                nullValue());
    }

    @Test
    public void updateAimAddNameSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(COMPANY_NAME_FIRST, TextCampaign.COMPANY_NAME)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), nullValue());
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
        assertThat("company name aim has to be appropriate", actualCampaign.getCompanyName(),
                is(COMPANY_NAME_FIRST));
        assertThat("business category has to be appropriate", actualCampaign.getBusinessCategory(),
                nullValue());
    }

    @Test
    public void updateAimAddHrefSavedCorrectly() {
        Long id = addReturnId(defaultCampaign().withCampAimType(CampAimType.PHONE_CALLS));
        update(new ModelChanges<>(id, TextCampaign.class)
                .process(HREF_FIRST, TextCampaign.HREF)
                .process(CampAimType.SITE_ACTIONS, TextCampaign.CAMP_AIM_TYPE)
                .castModelUp(BaseCampaign.class));
        Map<Long, ? extends BaseCampaign> typedCampaigns = campaignTypedRepository.getTypedCampaignsMap(
                clientInfo.getShard(), Collections.singletonList(id));
        TextCampaign actualCampaign = (TextCampaign) typedCampaigns.get(id);
        assertThat("campaign is found", actualCampaign, notNullValue());
        assertThat("href has to be appropriate", actualCampaign.getHref(), is(HREF_FIRST));
        assertThat("camp aim has to be appropriate", actualCampaign.getCampAimType(),
                is(CampAimType.SITE_ACTIONS));
    }

    @Test
    public void addDynamicCampaignWithHrefSavedCorrectly() {
        Long id = addReturnId(defaultDynamicCampaign().withHref(HREF_FIRST));
        List<DynamicCampaign> campaigns
                = campaignTypedRepository.getStrictly(clientInfo.getShard(), List.of(id), DynamicCampaign.class);
        DynamicCampaign campaign = campaigns.get(0);
        assertThat("campaign is found", campaign, notNullValue());
        assertThat("href has to be appropriate", campaign.getHref(), is(HREF_FIRST));
    }

    @Test
    public void addDynamicCampaignWithoutHrefSavedCorrectly() {
        Long id = addReturnId(defaultDynamicCampaign());
        List<DynamicCampaign> campaigns
                = campaignTypedRepository.getStrictly(clientInfo.getShard(), List.of(id), DynamicCampaign.class);
        DynamicCampaign campaign = campaigns.get(0);
        assertThat("campaign is found", campaign, notNullValue());
        assertThat("href has to be appropriate", campaign.getHref(), nullValue());
        assertThat("row is not present", isRowNotPresent(id), is(true));
    }

    @Test
    public void updateDynamicCampaignNameHrefUnchanged() {
        Long id = addReturnId(defaultDynamicCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, DynamicCampaign.class)
                .process("Another name", DynamicCampaign.NAME)
                .castModelUp(BaseCampaign.class));
        List<DynamicCampaign> campaigns
                = campaignTypedRepository.getStrictly(clientInfo.getShard(), List.of(id), DynamicCampaign.class);
        DynamicCampaign campaign = campaigns.get(0);
        assertThat("campaign is found", campaign, notNullValue());
        assertThat("href has to be appropriate", campaign.getHref(), is(HREF_FIRST));
    }

    @Test
    public void updateDynamicCampaignHrefChanged() {
        Long id = addReturnId(defaultDynamicCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, DynamicCampaign.class)
                .process("Another name", DynamicCampaign.NAME)
                .process(HREF_SECOND, DynamicCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        List<DynamicCampaign> campaigns
                = campaignTypedRepository.getStrictly(clientInfo.getShard(), List.of(id), DynamicCampaign.class);
        DynamicCampaign campaign = campaigns.get(0);
        assertThat("campaign is found", campaign, notNullValue());
        assertThat("href has to be appropriate", campaign.getHref(), is(HREF_SECOND));
    }

    @Test
    public void updateDynamicCampaignHrefAdded() {
        Long id = addReturnId(defaultDynamicCampaign());
        update(new ModelChanges<>(id, DynamicCampaign.class)
                .process("Another name", DynamicCampaign.NAME)
                .process(HREF_FIRST, DynamicCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        List<DynamicCampaign> campaigns
                = campaignTypedRepository.getStrictly(clientInfo.getShard(), List.of(id), DynamicCampaign.class);
        DynamicCampaign campaign = campaigns.get(0);
        assertThat("campaign is found", campaign, notNullValue());
        assertThat("href has to be appropriate", campaign.getHref(), is(HREF_FIRST));
    }

    @Test
    public void deleteDynamicCampaignHrefDeleted() {
        Long id = addReturnId(defaultDynamicCampaign().withHref(HREF_FIRST));
        update(new ModelChanges<>(id, DynamicCampaign.class)
                .process("Another name", DynamicCampaign.NAME)
                .process(null, DynamicCampaign.HREF)
                .castModelUp(BaseCampaign.class));
        List<DynamicCampaign> campaigns
                = campaignTypedRepository.getStrictly(clientInfo.getShard(), List.of(id), DynamicCampaign.class);
        DynamicCampaign campaign = campaigns.get(0);
        assertThat("campaign is found", campaign, notNullValue());
        assertThat("href has to be appropriate", campaign.getHref(), nullValue());
        assertThat("row is not present", isRowNotPresent(id), is(true));
    }

    private TextCampaign defaultCampaign() {
        return defaultTextCampaign()
                .withAttributionModel(CampaignAttributionModel.LAST_CLICK);
    }

    void update(ModelChanges<? extends BaseCampaign> modelChanges) {
        var options = new CampaignOptions();
        MassResult<Long> result = campaignOperationService.createRestrictedCampaignUpdateOperation(
                List.of(modelChanges), clientInfo.getUid(),
                UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()), options).apply();
        assertThat("campaign updated successfully", result.getSuccessfulCount(), is(1));
    }

    Long addReturnId(BaseCampaign campaign) {
        MassResult<Long> result = campaignOperationService.createRestrictedCampaignAddOperation(List.of(campaign),
                clientInfo.getUid(), UidAndClientId.of(clientInfo.getUid(), clientInfo.getClientId()),
                new CampaignOptions()).prepareAndApply();
        assertThat("campaign added successfully", result.getSuccessfulCount(), is(1));
        Long id = result.get(0).getResult();
        assertThat("campaign added successfully", id, notNullValue());
        return id;
    }

    private boolean isRowNotPresent(Long id) {
        return dslContextProvider.ppc(clientInfo.getShard())
                .select(CAMP_ADDITIONAL_DATA.CID)
                .from(CAMP_ADDITIONAL_DATA)
                .where(CAMP_ADDITIONAL_DATA.CID.eq(id))
                .fetchOne(CAMP_ADDITIONAL_DATA.CID) == null;
    }
}
