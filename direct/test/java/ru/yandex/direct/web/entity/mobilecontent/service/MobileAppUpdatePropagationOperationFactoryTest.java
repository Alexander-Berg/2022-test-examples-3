package ru.yandex.direct.web.entity.mobilecontent.service;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.MobileAppInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.operation.EmptyOperation;
import ru.yandex.direct.web.configuration.DirectWebTest;
import ru.yandex.direct.web.entity.mobilecontent.model.PropagationMode;
import ru.yandex.direct.web.entity.mobilecontent.model.PropagationRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;


@DirectWebTest
@RunWith(SpringJUnit4ClassRunner.class)
public class MobileAppUpdatePropagationOperationFactoryTest {
    private static final String STORE_URL = "https://play.google.com/store/apps/details?id=com.kiloo.subwaysurf";

    @Autowired
    private Steps steps;

    @Autowired
    private MobileAppUpdatePropagationOperationFactory mobileAppUpdatePropagationOperationFactory;

    private ClientInfo clientInfo;
    private MobileAppInfo mobileAppInfo;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        mobileAppInfo = steps.mobileAppSteps().createMobileApp(clientInfo, STORE_URL);
    }

    @Test
    public void whenDoNotApplyToBanner_ThenReturnsEmptyOperation() {
        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId())
                .withDisplayedAttributes(Collections.emptySet())
                .withPropagationMode(PropagationMode.DO_NOT_APPLY_TO_BANNERS)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request);
        assertThat("Операция создана", propagationOperation.hasOperation(), equalTo(true));
        assertThat("Создана пустая операция", propagationOperation.getOperation(), instanceOf(EmptyOperation.class));
    }

    @Test
    public void whenThereIsNoAnyLinedBanners_ThenReturnsEmptyOperation() {
        PropagationRequest request = PropagationRequest.builder()
                .withMobileAppId(mobileAppInfo.getMobileAppId())
                .withDisplayedAttributes(Collections.emptySet())
                .withPropagationMode(PropagationMode.APPLY_TO_ANY_RELATED_BANNERS_AND_REPLACE_ALL)
                .build();
        MobileAppUpdatePropagationOperationFactory.Result propagationOperation =
                mobileAppUpdatePropagationOperationFactory
                        .createPropagationOperation(clientInfo.getUid(), clientInfo.getClientId(), request);
        assertThat("Операция создана", propagationOperation.hasOperation(), equalTo(true));
        assertThat("Создана пустая операция", propagationOperation.getOperation(), instanceOf(EmptyOperation.class));
    }
}
