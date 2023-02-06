package ru.yandex.chemodan.app.psbilling.core;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.joda.time.Instant;
import org.joda.time.LocalDate;
import org.mockito.Mockito;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.function.Function;
import ru.yandex.chemodan.app.psbilling.core.billing.groups.GroupServiceTransactionsCalculationService;
import ru.yandex.chemodan.app.psbilling.core.dao.features.UserServiceFeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupProductDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceMemberDao;
import ru.yandex.chemodan.app.psbilling.core.dao.groups.GroupServiceTransactionsDao;
import ru.yandex.chemodan.app.psbilling.core.dao.products.FeatureDao;
import ru.yandex.chemodan.app.psbilling.core.dao.users.UserServiceDao;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.BalancePaymentInfo;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.Group;
import ru.yandex.chemodan.app.psbilling.core.entities.groups.GroupService;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureEntity;
import ru.yandex.chemodan.app.psbilling.core.entities.products.FeatureType;
import ru.yandex.chemodan.app.psbilling.core.groups.GroupServicesManager;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProduct;
import ru.yandex.chemodan.app.psbilling.core.products.GroupProductManager;
import ru.yandex.chemodan.app.psbilling.core.synchronization.engine.Target;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupmember.GroupServiceMemberActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.groupservice.GroupServicesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservice.UserServiceActualizationService;
import ru.yandex.chemodan.app.psbilling.core.synchronization.userservicefeature.UserFeaturesActualizationService;
import ru.yandex.chemodan.app.psbilling.core.utils.DateUtils;
import ru.yandex.chemodan.directory.client.DirectoryClient;
import ru.yandex.chemodan.directory.client.DirectoryOrganizationFeaturesResponse;

@AllArgsConstructor
public class PsBillingTransactionsFactory {
    private PsBillingGroupsFactory psBillingGroupsFactory;
    private PsBillingProductsFactory psBillingProductsFactory;
    private DirectoryClient directoryClient;
    private GroupServicesActualizationService groupServicesActualizationService;
    private GroupServiceMemberActualizationService groupServiceMemberActualizationService;
    private UserServiceActualizationService userServiceActualizationService;
    private GroupServiceMemberDao groupServiceMemberDao;
    private UserServiceDao userServiceDao;
    private GroupServiceDao groupServiceDao;
    private GroupProductDao groupProductDao;
    private GroupProductManager groupProductManager;
    private UserServiceFeatureDao userServiceFeatureDao;
    private FeatureDao featureDao;
    private UserFeaturesActualizationService userFeaturesActualizationService;
    private GroupServicesManager groupServicesManager;
    private GroupServiceTransactionsCalculationService groupServiceTransactionsCalculationService;
    private GroupServiceTransactionsDao groupServiceTransactionsDao;
    private GroupDao groupDao;

    public void setupMocks() {
        Mockito.when(directoryClient.getOrganizationFeatures(Mockito.anyString()))
                .thenReturn(new DirectoryOrganizationFeaturesResponse(false, false));
    }

    public Group createGroup(Long clientId, ListF<Service> services, int userCount) {
        return createGroup(x -> x, clientId, services, userCount);
    }

    public Group createGroup(Function<GroupDao.InsertData.InsertDataBuilder, GroupDao.InsertData.InsertDataBuilder> customizer,
                             Long clientId, ListF<Service> services, int userCount) {
        Group group = psBillingGroupsFactory.createGroup(x ->
                customizer.apply(x.paymentInfo(new BalancePaymentInfo(clientId, "1234"))));
        services.forEach(x -> createService(group, x, userCount));
        return group;
    }

    public GroupService createService(Group group, Service service, int userCount) {
        Instant oldTime = Instant.now();
        DateUtils.freezeTime(service.createdAt);

        FeatureEntity feature;
        GroupProduct groupProduct;
        Option<GroupProduct> existProduct = groupProductDao.findByProductCodes(Cf.list(service.productCode)).firstO()
                .map(x -> groupProductManager.findById(x.getId()));
        if (!existProduct.isPresent()) {
            groupProduct = psBillingProductsFactory.createGroupProduct(x -> x.code(service.productCode));
        } else {
            groupProduct = existProduct.get();
        }

        ListF<FeatureEntity> productFeatures = groupProduct.getFeatures()
                .filter(x -> x.getFeatureId().isPresent())
                .map(fo -> featureDao.findById(fo.getFeatureId().get()));
        if (productFeatures.isEmpty()) {
            feature = psBillingProductsFactory.createFeature(FeatureType.ADDITIVE);
            psBillingProductsFactory.createProductFeature(groupProduct.getUserProduct().getId(), feature);
        } else {
            feature = productFeatures.first();
        }

        GroupService groupService = psBillingGroupsFactory.createActualGroupService(group, groupProduct);
        groupServiceDao.updateSkipTransactionsExport(groupService.getId(), service.skipTransactionsExport);
        if (service.disabledAt.isPresent()) {
            DateUtils.freezeTime(service.disabledAt.get());
            groupServicesManager.disableGroupService(groupService);
            groupServiceDao.setStatusActual(Cf.list(groupService.getId()), Target.DISABLED);
        }
        synchronize(group, feature.getId(), userCount);

        DateUtils.freezeTime(oldTime);
        return groupService;
    }

    public ListF<GroupServiceTransactionsDao.ExportRow> calculateTransactions(LocalDate date) {
        return calculateTransactions(date, date);
    }

    public ListF<GroupServiceTransactionsDao.ExportRow> calculateMonthTransactions(LocalDate month) {
        return calculateTransactions(month.withDayOfMonth(1), month.plusDays(1).minusDays(1));
    }

    public ListF<GroupServiceTransactionsDao.ExportRow> calculateTransactions(LocalDate from, LocalDate to) {
        ListF<GroupServiceTransactionsDao.ExportRow> transactions = Cf.arrayList();
        while (from.isBefore(to.plusDays(1))) {
            groupServiceTransactionsCalculationService.clearExistingTransactionsOnDay(from);
            groupServiceTransactionsCalculationService.performSyncCalculation(from, true);
            transactions.addAll(groupServiceTransactionsDao.findTransactions(from, Option.empty(), 100000));
            from = from.plusDays(1);
        }
        return transactions;
    }

    public static class Service {
        String productCode;
        Instant createdAt;
        Option<Instant> disabledAt;
        boolean skipTransactionsExport;

        public Service(String productCode) {
            this.productCode = productCode;
            createdAt = Instant.now();
            disabledAt = Option.empty();
            skipTransactionsExport = false;
        }

        public Service createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Service createdAt(LocalDate createdAt) {
            this.createdAt = new Instant(createdAt.toDate());
            return this;
        }

        public Service disabledAt(Instant disabledAt) {
            this.disabledAt = Option.of(disabledAt);
            return this;
        }

        public Service disabledAt(LocalDate disabledAt) {
            this.disabledAt = Option.of(new Instant(disabledAt.toDate()));
            return this;
        }

        public Service notDisabled() {
            this.disabledAt = Option.empty();
            return this;
        }

        public Service skipTransactionsExport(boolean skipTransactionsExport) {
            this.skipTransactionsExport = skipTransactionsExport;
            return this;
        }
    }

    @SneakyThrows
    public void disable(GroupService groupService) {
        groupServicesManager.disableGroupService(groupService);
        UUID featureId = groupProductManager.findById(groupService.getGroupProductId())
                .getUserProduct().getFeatures().first().getFeatureId().get();
        synchronize(groupDao.findById(groupService.getGroupId()), featureId);
    }

    @SneakyThrows
    private void synchronize(Group group, UUID featureId, int userCount) {
        ListF<Long> uids = Cf.range(0, userCount).map(x -> (long) x + 1);
        Mockito.when(directoryClient.usersInOrganization(Mockito.eq(group.getExternalId()),
                        Mockito.anyBoolean(), Mockito.anyInt(),
                        Mockito.any()))
                .thenReturn(uids.toLinkedHashSet());
        synchronize(group, featureId);
    }

    private void synchronize(Group group, UUID featureId) {
        groupServicesActualizationService.actualize(group.getId());
        groupServiceMemberDao.findRecordsInInitStatus()
                .forEach(groupServiceMemberActualizationService::actualizeGroupServiceMember);
        userServiceDao.findRecordsInInitStatus().forEach(userServiceActualizationService::actualizeUserService);

        userServiceFeatureDao.findAll().forEach(userServiceFeature ->
                userFeaturesActualizationService.synchronize(userServiceFeature.getUid(), featureId)
        );
        groupServiceMemberActualizationService.updateGroupServiceMembersSyncState();
        userServiceActualizationService.updateUserServicesSyncState();
        groupServicesActualizationService.updateGroupServicesSyncState();
    }
}
