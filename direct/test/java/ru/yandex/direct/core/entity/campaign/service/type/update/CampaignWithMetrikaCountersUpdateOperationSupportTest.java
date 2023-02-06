package ru.yandex.direct.core.entity.campaign.service.type.update;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.direct.core.entity.campaign.model.BaseCampaign;
import ru.yandex.direct.core.entity.campaign.model.CampaignType;
import ru.yandex.direct.core.entity.campaign.model.CampaignWithMetrikaCounters;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.core.entity.campaign.repository.CampaignTypedRepository;
import ru.yandex.direct.core.entity.campaign.service.CampaignOptions;
import ru.yandex.direct.core.entity.campaign.service.RequestBasedMetrikaClientAdapter;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainer;
import ru.yandex.direct.core.entity.campaign.service.type.update.container.RestrictedCampaignsUpdateOperationContainerImpl;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCampaigns;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.metrika.client.MetrikaClient;
import ru.yandex.direct.model.AppliedChanges;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(Parameterized.class)
public class CampaignWithMetrikaCountersUpdateOperationSupportTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
    @Rule
    public SpringMethodRule springMethodRule = new SpringMethodRule();
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule().silent();

    @Autowired
    private CampaignWithMetrikaCountersUpdateOperationSupport campaignWithMetrikaCountersUpdateOperationSupport;

    @Autowired
    private MetrikaClient metrikaClient;

    @Autowired
    private Steps steps;

    @Autowired
    CampaignTypedRepository campaignTypedRepository;

    @Autowired
    DslContextProvider dslContextProvider;

    @Parameterized.Parameter
    public CampaignType campaignType;

    @Parameterized.Parameters(name = "{0}")
    public static Collection typeOfCampaignParameter() {
        return Arrays.asList(new Object[][]{
                {CampaignType.TEXT},
                {CampaignType.PERFORMANCE},
                {CampaignType.MCBANNER},
                {CampaignType.DYNAMIC}
        });
    }

    @Test
    public void enrich() {
        long id = RandomNumberUtils.nextPositiveLong();
        ModelChanges<CampaignWithMetrikaCounters> modelChanges =
                new ModelChanges<>(id, CampaignWithMetrikaCounters.class);

        List<Long> updatingMetrikaCounters = List.of(1L);
        modelChanges.process(updatingMetrikaCounters, CampaignWithMetrikaCounters.METRIKA_COUNTERS);

        List<Long> currentMetrikaCounters = List.of(1L);
        AppliedChanges<CampaignWithMetrikaCounters> appliedChanges =
                modelChanges.applyTo(((CampaignWithMetrikaCounters) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(id)
                        .withMetrikaCounters(currentMetrikaCounters));

        campaignWithMetrikaCountersUpdateOperationSupport.onChangesApplied(null, List.of(appliedChanges));
        assertThat(appliedChanges.getActuallyChangedProps()).isEmpty();
    }

    @Test
    public void update() {
        CampaignInfo textCampaignInfo =
                steps.campaignSteps().createActiveTextCampaign();

        List<Long> metrikaCounters = List.of((long) RandomNumberUtils.nextPositiveInteger());

        updateMetrikaCounters(textCampaignInfo, metrikaCounters);

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        Collections.singletonList(textCampaignInfo.getCampaignId()));
        List<CampaignWithMetrikaCounters> campaignsWithMetrikaCounters = mapList(typedCampaigns,
                CampaignWithMetrikaCounters.class::cast);
        CampaignWithMetrikaCounters actualCampaign = campaignsWithMetrikaCounters.get(0);

        assertThat(actualCampaign.getMetrikaCounters()).isEqualTo(metrikaCounters);
    }

    @Test
    public void removeFromCampaignMetrikaCounters() {
        CampaignInfo textCampaignInfo =
                steps.campaignSteps().createActiveTextCampaign();

        List<Long> metrikaCounters = List.of((long) RandomNumberUtils.nextPositiveInteger());

        updateMetrikaCounters(textCampaignInfo, metrikaCounters);

        updateMetrikaCounters(textCampaignInfo, null);

        List<? extends BaseCampaign> typedCampaigns =
                campaignTypedRepository.getTypedCampaigns(textCampaignInfo.getShard(),
                        Collections.singletonList(textCampaignInfo.getCampaignId()));
        List<TextCampaign> textCampaigns = mapList(typedCampaigns, TextCampaign.class::cast);
        TextCampaign actualCampaign = textCampaigns.get(0);

        assertThat(actualCampaign.getMetrikaCounters()).isNull();
    }

    private void updateMetrikaCounters(CampaignInfo textCampaignInfo, List<Long> metrikaCounters) {
        ModelChanges<CampaignWithMetrikaCounters> modelChanges =
                new ModelChanges<>(textCampaignInfo.getCampaignId(), CampaignWithMetrikaCounters.class);

        modelChanges.process(metrikaCounters, CampaignWithMetrikaCounters.METRIKA_COUNTERS);

        AppliedChanges<CampaignWithMetrikaCounters> appliedChanges =
                modelChanges.applyTo(((CampaignWithMetrikaCounters) TestCampaigns.newCampaignByCampaignType(campaignType))
                        .withId(textCampaignInfo.getCampaignId())
                        .withMetrikaCounters(List.of()));

        var metrikaClientAdapter = new RequestBasedMetrikaClientAdapter(metrikaClient, List.of(textCampaignInfo.getClientId().asLong()), Set.of());
        RestrictedCampaignsUpdateOperationContainer updateParameters = new RestrictedCampaignsUpdateOperationContainerImpl(
                textCampaignInfo.getShard(),
                textCampaignInfo.getUid(),
                textCampaignInfo.getClientId(),
                textCampaignInfo.getUid(),
                textCampaignInfo.getUid(),
                metrikaClientAdapter,
                new CampaignOptions(),
                null,
                emptyMap()
        );

        campaignWithMetrikaCountersUpdateOperationSupport.updateRelatedEntitiesInTransaction(
                dslContextProvider.ppc(textCampaignInfo.getShard()), updateParameters, List.of(appliedChanges));
    }
}
