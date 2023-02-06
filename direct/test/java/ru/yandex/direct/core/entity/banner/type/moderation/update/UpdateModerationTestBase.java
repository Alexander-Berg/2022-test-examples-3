package ru.yandex.direct.core.entity.banner.type.moderation.update;

import java.util.Set;
import java.util.function.BiFunction;

import org.junit.runners.Parameterized;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.banner.model.BannerWithSystemFields;
import ru.yandex.direct.core.entity.banner.service.BannersUpdateOperationFactory;
import ru.yandex.direct.core.entity.banner.service.moderation.ModerationMode;
import ru.yandex.direct.core.entity.banner.type.moderation.ModerationTestBase;
import ru.yandex.direct.core.entity.feature.service.FeatureService;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.model.ModelProperty;
import ru.yandex.direct.operation.Applicability;
import ru.yandex.direct.operation.Operation;

import static java.util.Collections.singletonList;

public abstract class UpdateModerationTestBase extends ModerationTestBase {

    protected static final String TITLE = "title";
    protected static final String NEW_TITLE = "new title";

    @Autowired
    public BannersUpdateOperationFactory operationFactory;

    @Autowired
    public FeatureService featureService;

    @Parameterized.Parameter(4)
    public ModerationMode moderationMode;

    @Parameterized.Parameter(10)
    public ModelProperty<? super BannerWithSystemFields, Object> modelProperty;

    @Parameterized.Parameter(11)
    public BiFunction<Steps, AdGroupInfo, Object> valueGetter;

    @Parameterized.Parameter(12)
    public Class<? extends BannerWithSystemFields> clazz;

    protected abstract Long createBanner(AdGroupInfo adGroupInfo);

    protected Operation<Long> createOperation(AdGroupInfo adGroupInfo) {
        var bannerId = createBanner(adGroupInfo);
        ModelChanges<BannerWithSystemFields> modelChange = new ModelChanges<>(bannerId, clazz)
                .processNotNull(valueGetter.apply(steps, adGroupInfo), modelProperty)
                .castModelUp(BannerWithSystemFields.class);
        Set<String> clientEnabledFeatures = featureService.getEnabledForClientId(adGroupInfo.getClientId());

        return operationFactory.createUpdateOperation(Applicability.FULL, false,
                moderationMode, singletonList(modelChange),
                adGroupInfo.getShard(), adGroupInfo.getClientId(),
                adGroupInfo.getUid(), clientEnabledFeatures, false);
    }

    protected static BiFunction<Steps, AdGroupInfo, Object> newValue(Object value) {
        return (steps, adGroupInf) -> value;
    }
}
