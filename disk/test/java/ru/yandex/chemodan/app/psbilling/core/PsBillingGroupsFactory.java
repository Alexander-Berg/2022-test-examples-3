package ru.yandex.chemodan.app.psbilling.core;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.joda.time.Duration;
import org.joda.time.Instant;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.dao.features.GroupServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServicePriceOverrideDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.ProductTemplateFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.texts.TankerKeyDao;
import ru.yandex.chemodan.app.psbilling.core.entities.AbstractEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.features.GroupServiceFeature;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupPaymentType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServiceMember;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupServicePriceOverride;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupType;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.PriceOverrideReason;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.entities.products.ProductFeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.texts.TankerTranslationEntity;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.groups.SubscriptionContext;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProductManager;
import ru.yandex.chemodan.app.psbilling.core.products.UserProductManager;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.inside.passport.PassportUid;

import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.FAN_MAIL_LIMIT_FEATURE_CODE;
import static ru.yandex.chemodan.app.psbilling.core.products.UserProductFeatureRegistry.MPFS_SPACE_FEATURE_CODE;

@RequiredArgsConstructor
public class PsBillingGroupsFactory {
    public static final long DEFAULT_CLIENT_ID = 11111L;
    public static final PassportUid DEFAULT_UID = PassportUid.cons(1234);
    private final GroupDao groupDao;
    private final GroupServicesManager groupServicesManager;
    private final GroupServiceMemberDao groupServiceMemberDao;
    private final GroupServiceDao groupServiceDao;
    private final GroupProductManager groupProductManager;
    private final UserProductManager userProductManager;
    private final PsBillingProductsFactory productsFactory;
    private final PsBillingTextsFactory psBillingTextsFactory;
    private final GroupProductDao groupProductDao;
    private final GroupServiceFeatureDao groupServiceFeatureDao;
    private final ProductTemplateFeatureDao productTemplateFeatureDao;
    private final GroupServicePriceOverrideDao groupServicePriceOverrideDao;
    private final TankerKeyDao tankerKeyDao;


    public Group createGroup() {
        return createGroup(Function.identityF());
    }

    @NotNull
    public Group createGroup(Long clientId){
        return createGroup(x->x.paymentInfo(new BalancePaymentInfo(clientId, "1111")));
    }

    @NotNull
    public Group createGroup(
            Function<GroupDao.InsertData.InsertDataBuilder, GroupDao.InsertData.InsertDataBuilder> customizer) {
        GroupDao.InsertData.InsertDataBuilder defaultBuilder = GroupDao.InsertData.builder()
                .externalId(UUID.randomUUID().toString())
                .type(GroupType.ORGANIZATION)
                .paymentInfo(new BalancePaymentInfo(DEFAULT_CLIENT_ID, DEFAULT_UID))
                .gracePeriod(Duration.standardDays(20))
                .clid(Option.empty())
                .ownerUid(DEFAULT_UID);
        defaultBuilder = customizer.apply(defaultBuilder);
        return groupDao.insert(defaultBuilder.build());
    }

    public GroupServiceMember createGroupServiceMember(GroupService groupService) {
        return createGroupServiceMember(groupService, Function.identityF());
    }

    public GroupServiceMember createGroupServiceMember(GroupService groupService, String uid) {
        return createGroupServiceMember(groupService, x -> x.uid(uid));
    }

    public GroupServiceMember createGroupServiceMember(GroupService groupService,
                                                       Function<GroupServiceMemberDao.InsertData.InsertDataBuilder,
                                                               GroupServiceMemberDao.InsertData.InsertDataBuilder> customizer
    ) {
        GroupServiceMemberDao.InsertData.InsertDataBuilder defaultBuilder = GroupServiceMemberDao.InsertData.builder()
                .uid(UUID.randomUUID().toString())
                .groupServiceId(groupService.getId());
        defaultBuilder = customizer.apply(defaultBuilder);
        GroupServiceMemberDao.InsertData insertData = defaultBuilder.build();
        groupServiceMemberDao.batchInsert(Cf.list(insertData), Target.ENABLED);
        return groupServiceMemberDao.findEnabledByParentId(groupService.getId()).filter(x -> x.getUid().equals(insertData.getUid())).first();
    }

    public GroupService createGroupService() {
        return createGroupService(Target.ENABLED);
    }

    public GroupService createGroupService(Target target) {
        return createGroupService(createGroup(), productsFactory.createGroupProduct(), target);
    }

    public GroupService createGroupService(Group group, GroupProduct product) {
        return createGroupService(group, product, Target.ENABLED);
    }

    public GroupService createActualGroupService(Group group, GroupProduct product) {
        GroupService groupService = createGroupService(group, product);
        groupServiceDao.setStatusActual(Cf.list(groupService.getId()), Target.ENABLED);
        return groupServiceDao.findById(groupService.getId());
    }

    public GroupService createGroupService(Group group, GroupProduct product, Target target) {
        GroupService groupService = groupServicesManager.createGroupService(
                new SubscriptionContext(product, group, PsBillingUsersFactory.UID, Option.empty(), false));
        if (target == Target.DISABLED) {
            groupServiceDao.setTargetToDisabled(groupService.getId(), groupService.getTarget());
            return groupServiceDao.findById(groupService.getId());
        }
        return groupService;
    }

    public GroupServiceFeature createGroupServiceFeature(
            Group group,
            FeatureEntity feature,
            java.util.function.Function<ProductFeatureDao.InsertData.InsertDataBuilder,
                    ProductFeatureDao.InsertData.InsertDataBuilder> featureCustomizer
    ) {
        return createGroupServiceFeatures(group, Cf.list(feature), Cf.list(featureCustomizer)).get(0);
    }

    public ListF<GroupServiceFeature> createGroupServiceFeatures(
            Group group,
            ListF<FeatureEntity> features,
            ListF<java.util.function.Function<ProductFeatureDao.InsertData.InsertDataBuilder,
                    ProductFeatureDao.InsertData.InsertDataBuilder>> featureCustomizers
    ) {
        UUID tankerKeyId = psBillingTextsFactory.create("whatever").getId();
        tankerKeyDao.mergeTranslations(Cf.list(new TankerTranslationEntity(tankerKeyId, "ru", "whatever")));
        GroupProduct product = productsFactory.createGroupProduct(x -> x.titleTankerKeyId(Option.of(tankerKeyId)));

        ListF<ProductFeatureEntity> productFeatures = Cf.arrayList();
        for (int i = 0; i < features.size(); i++) {
            productFeatures.add(productsFactory.createProductFeature(
                    product.getUserProductId(), features.get(i), featureCustomizers.get(i)));
        }
        return createGroupServiceFeatures(group, productFeatures);
    }

    public GroupServiceFeature createGroupServiceFeature(Group group, ProductFeatureEntity productFeature) {
        return createGroupServiceFeatures(group, Cf.list(productFeature)).get(0);
    }

    public ListF<GroupServiceFeature> createGroupServiceFeatures(Group group,
                                                                 ListF<ProductFeatureEntity> productFeatures) {
        productFeatures.forEach(pf -> userProductManager.findById(pf.getUserProductId()));

        GroupProduct groupProduct = groupProductManager.findById(groupProductDao.findAll()
                .filter(x -> x.getUserProductId().equals(productFeatures.get(0).getUserProductId())).single().getId());
        GroupService groupService = createGroupService(group, groupProduct);
        ListF<GroupServiceFeature> groupServiceFeatures = Cf.arrayList();
        for (ProductFeatureEntity productFeature : productFeatures) {
            groupServiceFeatures.add(groupServiceFeatureDao.insert(GroupServiceFeatureDao.InsertData.builder()
                    .productFeatureId(productFeature.getId())
                    .productTemplateFeatureId(productTemplateFeatureDao.findByProductFeatureId(productFeature.getId())
                            .map(AbstractEntity::getId).orElse((UUID) null))
                    .groupServiceId(groupService.getId())
                    .groupId(group.getId())
                    .build(), Target.ENABLED));
        }
        return groupServiceFeatures;
    }

    public GroupServicePriceOverride createServicePriceOverrides(GroupService groupService, double price,
                                                                 Instant from, Instant to) {
        return groupServicePriceOverrideDao.insert(GroupServicePriceOverrideDao.InsertData.builder()
                .pricePerUserInMonth(BigDecimal.valueOf(price))
                .startDate(from)
                .endDate(Option.of(to))
                .groupServiceId(groupService.getId())
                .hidden(false)
                .reason(PriceOverrideReason.GIFT)
                .trialUsageId(Option.empty())
                .build());
    }

    public void createDefaultProductService(Group group) {
        GroupProduct defaultProduct =
                productsFactory.createGroupProduct(x -> x.paymentType(GroupPaymentType.POSTPAID).pricePerUserInMonth(BigDecimal.ZERO).skipTransactionsExport(true));
        createGroupService(group, defaultProduct);

    }

    public void createCommonEmailFeatures(Group group) {
        ListF<FeatureEntity> features = Cf.list(productsFactory.createFeature(FeatureType.ADDITIVE),
                productsFactory.createFeature(FeatureType.ADDITIVE));
        ListF<java.util.function.Function<ProductFeatureDao.InsertData.InsertDataBuilder,
                ProductFeatureDao.InsertData.InsertDataBuilder>> featureCustomizers = Cf.list(
                        b -> b.code(MPFS_SPACE_FEATURE_CODE)
                                .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_3_gb").getId())),
                        b -> b.code(FAN_MAIL_LIMIT_FEATURE_CODE)
                                .valueTankerKeyId(Option.of(psBillingTextsFactory.create("test_1234").getId()))
        );
        createGroupServiceFeatures(group, features, featureCustomizers);
    }
}
