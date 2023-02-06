package ru.yandex.direct.api.v5.entity.features.delegate;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.yandex.direct.api.v5.features.FeatureGetItem;
import com.yandex.direct.api.v5.features.GetRequest;
import com.yandex.direct.api.v5.features.GetResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.context.ApiContext;
import ru.yandex.direct.api.v5.context.ApiContextHolder;
import ru.yandex.direct.api.v5.entity.GenericApiService;
import ru.yandex.direct.api.v5.security.ApiAuthenticationSource;
import ru.yandex.direct.api.v5.service.accelinfo.AccelInfoHeaderSetter;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.api.v5.units.ApiUnitsService;
import ru.yandex.direct.core.entity.campaign.service.accesschecker.RequestCampaignAccessibilityCheckerProvider;
import ru.yandex.direct.core.entity.feature.model.ClientFeature;
import ru.yandex.direct.core.entity.feature.model.Feature;
import ru.yandex.direct.core.entity.feature.model.FeatureSettings;
import ru.yandex.direct.core.entity.feature.model.FeatureState;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.entity.user.model.ApiUser;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;


@Api5Test
@RunWith(SpringRunner.class)
public class GetFeaturesDelegateTest {

    private static final String NOT_EXISTING_FEATURE_NAME = "not_existing_feature";
    private static final String EXISTING_FEATURE_NAME_FOR_PERCENT_100 = "existing_feature_name_for_percent_100";
    private static final String EXISTING_FEATURE_NAME_FOR_PERCENT_0 = "existing_feature_name_for_percent_0";

    @Autowired
    private FeatureService featureService;
    @Autowired
    private Steps steps;

    private GenericApiService genericApiService;
    private GetFeaturesDelegate delegate;
    private final Map<String, ClientFeature> features = new HashMap<>();

    @Before
    public void before() {
        ClientInfo clientInfo = steps.clientSteps().createClient(new ClientInfo());
        Feature firstFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature firstClientFeature = new ClientFeature()
                .withClientId(clientInfo.getClientId())
                .withId(firstFeature.getId())
                .withState(FeatureState.ENABLED);
        features.put(firstFeature.getFeatureTextId(), firstClientFeature);
        Feature secondFeature = steps.featureSteps().addDefaultFeature();
        ClientFeature secondClientFeature = new ClientFeature()
                .withClientId(clientInfo.getClientId())
                .withId(secondFeature.getId())
                .withState(FeatureState.DISABLED);
        features.put(secondFeature.getFeatureTextId(), secondClientFeature);
        steps.featureSteps().addClientFeatures(features.values());

        ApiAuthenticationSource auth = mock(ApiAuthenticationSource.class);
        when(auth.getChiefSubclient()).thenReturn(new ApiUser().withClientId(clientInfo.getClientId()));

        delegate = new GetFeaturesDelegate(auth, featureService);

        ApiContextHolder apiContextHolder = mock(ApiContextHolder.class);
        when(apiContextHolder.get()).thenReturn(new ApiContext());
        genericApiService = new GenericApiService(apiContextHolder,
                mock(ApiUnitsService.class),
                mock(AccelInfoHeaderSetter.class),
                mock(RequestCampaignAccessibilityCheckerProvider.class));
    }

    @Test
    public void getOneRandomFeature_successful() {
        Map.Entry<String, FeatureState> randomFeature = getRandomFeatureWithState();
        GetRequest getRequest = new GetRequest().withFeatureNames(randomFeature.getKey());
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<FeatureGetItem> actual = getResponse.getFeatures();
        assertThat(actual).hasSize(1);
        assertThat(actual.get(0)).is(matchedBy(beanDiffer(delegate.convertFeature(randomFeature))));
    }

    @Test
    public void getNotClientFeature_successful() {
        Feature notClientFeature = steps.featureSteps().addDefaultFeature();
        String featureName = notClientFeature.getFeatureTextId();
        GetRequest getRequest = new GetRequest().withFeatureNames(featureName);
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<FeatureGetItem> actual = getResponse.getFeatures();
        assertThat(actual).hasSize(1);
        Map.Entry<String, FeatureState> featureEntry = new AbstractMap.SimpleEntry<>(featureName,
                FeatureState.DISABLED);
        assertThat(actual.get(0)).is(matchedBy(beanDiffer(delegate.convertFeature(featureEntry))));
    }

    @Test
    public void getNotExistingFeature_successful() {
        GetRequest getRequest = new GetRequest().withFeatureNames(NOT_EXISTING_FEATURE_NAME);
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<FeatureGetItem> actual = getResponse.getFeatures();
        assertThat(actual).hasSize(1);
        Map.Entry<String, FeatureState> featureEntry =
                new AbstractMap.SimpleEntry<>(NOT_EXISTING_FEATURE_NAME, FeatureState.UNKNOWN);
        assertThat(actual.get(0)).is(matchedBy(beanDiffer(delegate.convertFeature(featureEntry))));
    }

    @Test
    public void getEnabledOnPercent_success() {
        steps.featureSteps().addFeature(EXISTING_FEATURE_NAME_FOR_PERCENT_100, new FeatureSettings().withPercent(100));

        GetRequest getRequest = new GetRequest().withFeatureNames(EXISTING_FEATURE_NAME_FOR_PERCENT_100);
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<FeatureGetItem> actual = getResponse.getFeatures();
        assertThat(actual).hasSize(1);
        Map.Entry<String, FeatureState> featureEntry =
                new AbstractMap.SimpleEntry<>(EXISTING_FEATURE_NAME_FOR_PERCENT_100, FeatureState.ENABLED);
        assertThat(actual.get(0)).is(matchedBy(beanDiffer(delegate.convertFeature(featureEntry))));
    }

    @Test
    public void getDisabledOnPercent_success() {
        steps.featureSteps().addFeature(EXISTING_FEATURE_NAME_FOR_PERCENT_0, new FeatureSettings().withPercent(0));

        GetRequest getRequest = new GetRequest().withFeatureNames(EXISTING_FEATURE_NAME_FOR_PERCENT_0);
        GetResponse getResponse = genericApiService.doAction(delegate, getRequest);
        List<FeatureGetItem> actual = getResponse.getFeatures();
        assertThat(actual).hasSize(1);
        Map.Entry<String, FeatureState> featureEntry =
                new AbstractMap.SimpleEntry<>(EXISTING_FEATURE_NAME_FOR_PERCENT_0, FeatureState.DISABLED);
        assertThat(actual.get(0)).is(matchedBy(beanDiffer(delegate.convertFeature(featureEntry))));
    }

    private Map.Entry<String, FeatureState> getRandomFeatureWithState() {
        List<String> keys = new ArrayList<>(features.keySet());
        int rand = RandomNumberUtils.nextPositiveInteger(keys.size());
        String key = keys.get(rand);
        return new AbstractMap.SimpleEntry<>(key, features.get(key).getState());
    }

}
