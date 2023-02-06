package ru.yandex.chemodan.app.psbilling.core.synchronization;

import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;

import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.AbstractGroupServicesTest;
import ru.yandex.chemodan.app.psbilling.core.PsBillingUsersFactory;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureScope;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.groups.SubscriptionContext;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;

public class SynchronizationTest extends AbstractGroupServicesTest {
    private Group group;
    private GroupProduct groupProduct;

    @SneakyThrows
    @Test
    public void testTablesSynchronization() {
        createFeatures(x -> x);

        GroupService groupService = groupServicesManager.createGroupService(
                new SubscriptionContext(groupProduct, group, PsBillingUsersFactory.UID, Option.empty(), false));
        enableAndCheck(group, groupService, false);

        groupServicesManager.disableGroupService(groupService);

        disableAndCheck(group, groupService, false);
    }


    @Test
    public void testTablesSynchronizationWithSnoozing() {
        createFeatures(x -> x.errorProcessorName("mpfs"));
        mockPost(Mockito.startsWith("http:/some.yandex.ru/activate/"),
                "{\"message\":\"account has no password\",\"code\":113,\"response\":409}", HttpStatus.CONFLICT);

        GroupService groupService = groupServicesManager.createGroupService(
                new SubscriptionContext(groupProduct, group, PsBillingUsersFactory.UID, Option.empty(), false));
        enableAndCheck(group, groupService, true);

        groupServicesManager.disableGroupService(groupService);

        disableAndCheck(group, groupService, true);
    }

    @SneakyThrows
    @Test
    public void testTablesSynchronizationGroupFeature() {
        createFeatures(x -> x);

        GroupService groupService = groupServicesManager.createGroupService(
                new SubscriptionContext(groupProduct, group, PsBillingUsersFactory.UID, Option.empty(), false));
        enableAndCheck(group, groupService, false);

        groupServicesManager.disableGroupService(groupService);

        disableAndCheck(group, groupService, false);
    }

    @Before
    public void Init() {
        group = psBillingGroupsFactory.createGroup();
        groupProduct = psBillingProductsFactory.createGroupProduct();
    }


    private void createFeatures(Function<FeatureEntity.FeatureEntityBuilder, FeatureEntity.FeatureEntityBuilder> customizer) {
        FeatureEntity groupFeature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE,
                b -> customizer.apply(b
                        .activationRequestTemplate(post("http:/some.yandex.ru/activate/#{groupExternalId}"))
                        .deactivationRequestTemplate(post("http:/some.yandex.ru/deactivate/#{groupExternalId}")))
        );

        FeatureEntity userFeature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE,
                b -> customizer.apply(b
                        .activationRequestTemplate(post("http:/some.yandex.ru/activate/#{uid}"))
                        .deactivationRequestTemplate(post("http:/some.yandex.ru/deactivate/#{uid}")))
        );

        psBillingProductsFactory.createProductFeature(groupProduct.getUserProductId(), groupFeature,
                x -> x.scope(FeatureScope.GROUP));
        psBillingProductsFactory.createProductFeature(groupProduct.getUserProductId(), userFeature,
                x -> x.scope(FeatureScope.USER));
    }
}
