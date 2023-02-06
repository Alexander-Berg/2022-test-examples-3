package ru.yandex.market.mbo.mdm.common.masterdata.services.business;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmBusinessStage;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplier;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierIdGroup;
import ru.yandex.market.mbo.mdm.common.masterdata.model.supplier.MdmSupplierType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmSupplierRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.queue.MdmQueuesManager;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class MdmBusinessStageSwitcherTest extends MdmBaseDbTestClass {

    private MdmBusinessStageSwitcher switcher;

    @Autowired
    private MdmSupplierRepository mdmSupplierRepository;
    @Autowired
    private MdmQueuesManager mdmQueuesManager;
    private StorageKeyValueServiceMock storageKeyValueService;
    private MdmSupplierCachingServiceImpl supplierCachingService;

    @Before
    public void setup() {
        storageKeyValueService = new StorageKeyValueServiceMock();
        switcher = new MdmBusinessStageSwitcherImpl(mdmSupplierRepository, mdmQueuesManager, storageKeyValueService);
        supplierCachingService = new MdmSupplierCachingServiceImpl(mdmSupplierRepository, storageKeyValueService);
        storageKeyValueService.putValue(MdmProperties.ENABLE_BUSINESS_STATE_UPDATE_IN_MDM_SUPPLIER_CACHE, true);
        supplierCachingService.refresh();
    }

    @Test
    public void whenNoChangesShouldDoNothing() {
        switcher.applySupplierBusinessChanges(List.of());
        Assertions.assertThat(mdmSupplierRepository.findAll()).isEmpty();
    }

    @Test
    public void testStage2to3() {
        MdmSupplier business = business(1);
        MdmSupplier service1 = service(11, business.getId(), false);
        MdmSupplier orphan2 = service(12, business.getId(), false); // этого не будем переключать
        MdmSupplier service3 = service(13, business.getId(), false);
        MdmSupplier service4 = service(14, business.getId(), true); // уже стейт 3
        MdmSupplier service5 = service(15, business.getId(), true); // уже стейт 3
        mdmSupplierRepository.insertBatch(service1, orphan2, service3, service4, service5, business);

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service3.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service4.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER) // ожидается no op
        ));

        MdmSupplierIdGroup stage3GroupByService1 = supplierCachingService.getSupplierIdGroupFor(service1.getId()).get();
        MdmSupplierIdGroup orphanGroupByService2 = supplierCachingService.getSupplierIdGroupFor(orphan2.getId()).get();
        MdmSupplierIdGroup stage2GroupByService2 = supplierCachingService.getFlatRelationsFor(orphan2.getId()).get();
        MdmSupplierIdGroup stage3GroupByService3 = supplierCachingService.getSupplierIdGroupFor(service3.getId()).get();
        MdmSupplierIdGroup stage3GroupByService4 = supplierCachingService.getSupplierIdGroupFor(service4.getId()).get();
        MdmSupplierIdGroup stage3GroupByService5 = supplierCachingService.getSupplierIdGroupFor(service5.getId()).get();
        MdmSupplierIdGroup stage3GroupByBusiness = supplierCachingService.getSupplierIdGroupFor(business.getId()).get();

        MdmSupplierIdGroup expectedStage3Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service1.getId(), service3.getId(), service4.getId(), service5.getId())
        );
        MdmSupplierIdGroup expectedStage2Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service1.getId(), orphan2.getId(), service3.getId(), service4.getId(), service5.getId())
        );
        MdmSupplierIdGroup expectedOrphanGroup = MdmSupplierIdGroup.createNoBusinessGroup(orphan2.getId());

        Assertions.assertThat(expectedStage3Group)
            .isEqualTo(stage3GroupByService1)
            .isEqualTo(stage3GroupByService3)
            .isEqualTo(stage3GroupByService4)
            .isEqualTo(stage3GroupByService5)
            .isEqualTo(stage3GroupByBusiness);
        Assertions.assertThat(stage2GroupByService2).isEqualTo(expectedStage2Group);
        Assertions.assertThat(orphanGroupByService2).isEqualTo(expectedOrphanGroup);
    }

    @Test
    public void testStage3to2() {
        MdmSupplier business = business(1);
        MdmSupplier service1 = service(11, business.getId(), true);
        MdmSupplier service2 = service(12, business.getId(), true); // этого не будем переключать
        MdmSupplier service3 = service(13, business.getId(), true);
        MdmSupplier service4 = service(14, business.getId(), false); // уже стейт 2
        MdmSupplier service5 = service(15, business.getId(), false); // уже стейт 2
        mdmSupplierRepository.insertBatch(service1, service2, service3, service4, service5, business);

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_DISABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service3.getId(), MdmBusinessStage.BUSINESS_DISABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service4.getId(), MdmBusinessStage.BUSINESS_DISABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER) // ожидается no op
        ));

        MdmSupplierIdGroup stage2GroupByService1 = supplierCachingService.getFlatRelationsFor(service1.getId()).get();
        MdmSupplierIdGroup orphanGroupByService1 = supplierCachingService.getSupplierIdGroupFor(service1.getId()).get();
        MdmSupplierIdGroup stage3GroupByService2 = supplierCachingService.getSupplierIdGroupFor(service2.getId()).get();
        MdmSupplierIdGroup stage2GroupByService3 = supplierCachingService.getFlatRelationsFor(service3.getId()).get();
        MdmSupplierIdGroup orphanGroupByService3 = supplierCachingService.getSupplierIdGroupFor(service3.getId()).get();
        MdmSupplierIdGroup stage2GroupByService4 = supplierCachingService.getFlatRelationsFor(service4.getId()).get();
        MdmSupplierIdGroup orphanGroupByService4 = supplierCachingService.getSupplierIdGroupFor(service4.getId()).get();
        MdmSupplierIdGroup stage2GroupByService5 = supplierCachingService.getFlatRelationsFor(service5.getId()).get();
        MdmSupplierIdGroup orphanGroupByService5 = supplierCachingService.getSupplierIdGroupFor(service5.getId()).get();
        MdmSupplierIdGroup stage3GroupByBusiness = supplierCachingService.getSupplierIdGroupFor(business.getId()).get();

        MdmSupplierIdGroup expectedStage2Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service1.getId(), service2.getId(), service3.getId(), service4.getId(), service5.getId())
        );
        MdmSupplierIdGroup expectedStage3Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service2.getId())
        );
        MdmSupplierIdGroup expectedOrphan1Group = MdmSupplierIdGroup.createNoBusinessGroup(service1.getId());
        MdmSupplierIdGroup expectedOrphan3Group = MdmSupplierIdGroup.createNoBusinessGroup(service3.getId());
        MdmSupplierIdGroup expectedOrphan4Group = MdmSupplierIdGroup.createNoBusinessGroup(service4.getId());
        MdmSupplierIdGroup expectedOrphan5Group = MdmSupplierIdGroup.createNoBusinessGroup(service5.getId());

        Assertions.assertThat(expectedStage2Group)
            .isEqualTo(stage2GroupByService1)
            .isEqualTo(stage2GroupByService3)
            .isEqualTo(stage2GroupByService4)
            .isEqualTo(stage2GroupByService5);
        Assertions.assertThat(expectedStage3Group)
            .isEqualTo(stage3GroupByService2)
            .isEqualTo(stage3GroupByBusiness);
        Assertions.assertThat(orphanGroupByService1).isEqualTo(expectedOrphan1Group);
        Assertions.assertThat(orphanGroupByService3).isEqualTo(expectedOrphan3Group);
        Assertions.assertThat(orphanGroupByService4).isEqualTo(expectedOrphan4Group);
        Assertions.assertThat(orphanGroupByService5).isEqualTo(expectedOrphan5Group);
    }

    @Test
    public void testBothDirectionsStageSwitchInOneRun() {
        MdmSupplier business = business(1);
        MdmSupplier service1 = service(11, business.getId(), true);
        MdmSupplier service2 = service(12, business.getId(), false);
        mdmSupplierRepository.insertBatch(service1, service2, business);

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_DISABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service2.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER)
        ));

        MdmSupplierIdGroup stage2GroupByService1 = supplierCachingService.getFlatRelationsFor(service1.getId()).get();
        MdmSupplierIdGroup orphanGroupByService1 = supplierCachingService.getSupplierIdGroupFor(service1.getId()).get();
        MdmSupplierIdGroup stage3GroupByService2 = supplierCachingService.getSupplierIdGroupFor(service2.getId()).get();
        MdmSupplierIdGroup stage3GroupByBusiness = supplierCachingService.getSupplierIdGroupFor(business.getId()).get();

        MdmSupplierIdGroup expectedStage2Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service1.getId(), service2.getId())
        );
        MdmSupplierIdGroup expectedStage3Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service2.getId())
        );
        MdmSupplierIdGroup expectedOrphan1Group = MdmSupplierIdGroup.createNoBusinessGroup(service1.getId());

        Assertions.assertThat(stage2GroupByService1).isEqualTo(expectedStage2Group);
        Assertions.assertThat(expectedStage3Group)
            .isEqualTo(stage3GroupByService2)
            .isEqualTo(stage3GroupByBusiness);
        Assertions.assertThat(orphanGroupByService1).isEqualTo(expectedOrphan1Group);
    }

    @Test
    public void testBusinessChange() {
        // Кейс с переездами трудно читать с голыми цифрами типа serviceX, поэтому построим ассоциативный ряд:
        // пусть у Черчилля и Ленина пригреты под боком сервисы-шпионы противоположных идеологий. Задача переезда -
        // вернуть шпионов домой.
        MdmSupplier churchill = business(1);
        MdmSupplier lenin = business(2);
        MdmSupplier communistSpy1 = service(11, churchill.getId(), false);
        MdmSupplier communistSpy2 = service(12, churchill.getId(), false);
        MdmSupplier capitalistSpy1 = service(13, lenin.getId(), false);
        MdmSupplier capitalistSpy2 = service(14, lenin.getId(), false);
        mdmSupplierRepository.insertBatch(lenin, churchill, communistSpy1, communistSpy2,
            capitalistSpy1, capitalistSpy2);

        switcher.applySupplierBusinessChanges(List.of( // возвращаем шпионов на родину к лидерам родных им идеологий
            BusinessSwitchInfo.businessChange(communistSpy1.getId(), lenin.getId(), 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.businessChange(communistSpy2.getId(), lenin.getId(), 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.businessChange(capitalistSpy1.getId(), churchill.getId(), 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.businessChange(capitalistSpy2.getId(), churchill.getId(), 0,
                BusinessSwitchTransport.MBI_LOGBROKER)
        ));

        MdmSupplierIdGroup actualCapitalists = supplierCachingService.getFlatRelationsFor(churchill.getId()).get();
        MdmSupplierIdGroup actualCommunists = supplierCachingService.getFlatRelationsFor(lenin.getId()).get();

        MdmSupplierIdGroup expectedCapitalistParty = MdmSupplierIdGroup.createBusinessGroup(
            churchill.getId(),
            List.of(capitalistSpy1.getId(), capitalistSpy2.getId())
        );
        MdmSupplierIdGroup expectedCommunistParty = MdmSupplierIdGroup.createBusinessGroup(
            lenin.getId(), // и пусть вас не смущает "бизнес" и коммунизм в одном месте. Нэпманы тоже люди!
            List.of(communistSpy1.getId(), communistSpy2.getId())
        );

        Assertions.assertThat(actualCapitalists).isEqualTo(expectedCapitalistParty);
        Assertions.assertThat(actualCommunists).isEqualTo(expectedCommunistParty);
    }

    @Test
    public void whenSeveralActionsScheduledShouldPickMostRecent() {
        MdmSupplier business = business(1);
        MdmSupplier service1 = service(11, business.getId(), true);
        MdmSupplier service2 = service(12, business.getId(), false);
        mdmSupplierRepository.insertBatch(service1, service2, business);

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 3,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_DISABLED, 10,
                BusinessSwitchTransport.MBI_LOGBROKER), // выберется эта
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, -100,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 5,
                BusinessSwitchTransport.MBI_LOGBROKER),

            BusinessSwitchInfo.stageSwitch(service2.getId(), MdmBusinessStage.BUSINESS_DISABLED, 2,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service2.getId(), MdmBusinessStage.BUSINESS_DISABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service2.getId(), MdmBusinessStage.BUSINESS_ENABLED, 10,
                BusinessSwitchTransport.MBI_LOGBROKER), // повторяется
            BusinessSwitchInfo.stageSwitch(service2.getId(), MdmBusinessStage.BUSINESS_DISABLED, -3,
                BusinessSwitchTransport.MBI_LOGBROKER),
            BusinessSwitchInfo.stageSwitch(service2.getId(), MdmBusinessStage.BUSINESS_ENABLED, 10,
                BusinessSwitchTransport.MBI_LOGBROKER) // схлопнется в одну
        ));

        MdmSupplierIdGroup stage2GroupByService1 = supplierCachingService.getFlatRelationsFor(service1.getId()).get();
        MdmSupplierIdGroup orphanGroupByService1 = supplierCachingService.getSupplierIdGroupFor(service1.getId()).get();
        MdmSupplierIdGroup stage3GroupByService2 = supplierCachingService.getSupplierIdGroupFor(service2.getId()).get();
        MdmSupplierIdGroup stage3GroupByBusiness = supplierCachingService.getSupplierIdGroupFor(business.getId()).get();

        MdmSupplierIdGroup expectedStage2Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service1.getId(), service2.getId())
        );
        MdmSupplierIdGroup expectedStage3Group = MdmSupplierIdGroup.createBusinessGroup(
            business.getId(),
            List.of(service2.getId())
        );
        MdmSupplierIdGroup expectedOrphan1Group = MdmSupplierIdGroup.createNoBusinessGroup(service1.getId());

        Assertions.assertThat(stage2GroupByService1).isEqualTo(expectedStage2Group);
        Assertions.assertThat(expectedStage3Group)
            .isEqualTo(stage3GroupByService2)
            .isEqualTo(stage3GroupByBusiness);
        Assertions.assertThat(orphanGroupByService1).isEqualTo(expectedOrphan1Group);
    }

    @Test
    public void testBusinessChangesFromMultipleSources() {
        MdmSupplier business = business(1);
        MdmSupplier service1 = service(11, business.getId(), true);
        MdmSupplier service2 = service(12, business.getId(), false);
        mdmSupplierRepository.insertBatch(service1, service2, business);

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_LOGBROKER)
        ));

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_YT)
        ));

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MBI_YT)
        ));

        switcher.applySupplierBusinessChanges(List.of(
            BusinessSwitchInfo.stageSwitch(service1.getId(), MdmBusinessStage.BUSINESS_ENABLED, 0,
                BusinessSwitchTransport.MIGRATION_LOCK_HANDLE)
        ));

        MdmSupplier supplier1 = mdmSupplierRepository.findById(service1.getId());
        Assertions.assertThat(supplier1.getBusinessSwitchTransports().keySet()).containsExactlyInAnyOrder(
            BusinessSwitchTransport.MBI_LOGBROKER,
            BusinessSwitchTransport.MBI_YT,
            BusinessSwitchTransport.MIGRATION_LOCK_HANDLE
        );
    }

    private MdmSupplier supplier(int id,
                                 boolean isBusiness,
                                 Integer businessId,
                                 boolean businessFlag) {
        MdmSupplier supplier = new MdmSupplier().setId(id);
        if (isBusiness) {
            supplier.setType(MdmSupplierType.BUSINESS);
        } else {
            supplier.setType(MdmSupplierType.THIRD_PARTY).setBusinessId(businessId);
        }
        supplier.setBusinessEnabled(businessFlag);
        return supplier;
    }

    private MdmSupplier business(int id) {
        return supplier(id, true, null, false);
    }

    private MdmSupplier service(int id, Integer businessId, boolean businessFlag) {
        return supplier(id, false, businessId, businessFlag);
    }
}
