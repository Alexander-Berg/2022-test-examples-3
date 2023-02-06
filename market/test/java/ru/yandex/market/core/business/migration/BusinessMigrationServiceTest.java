package ru.yandex.market.core.business.migration;

import java.util.List;

import Market.DataCamp.API.Migration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.business.BusinessService;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.ParamType;
import ru.yandex.market.core.param.model.PushPartnerStatus;
import ru.yandex.market.core.param.model.StringParamValue;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.saas.SaasService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(before = "DatacampBusinessMigrationServiceTest.before.csv")
public class BusinessMigrationServiceTest extends FunctionalTest {
    private static final long ACTION_ID = 1L;
    private static final long SHOP_ID = 776L;
    private static final long FULL_BPMN_SUPPLIER_ID = 779L;
    private static final long OLD_BUSINESS_ID = 2000L;
    private static final long NEW_BUSINESS_ID = 2001L;

    @Autowired
    private BusinessMigrationServiceImpl migrationService;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @Autowired
    private BusinessService businessService;

    @Autowired
    private ParamService paramService;

    @Autowired
    private SaasService saasService;

    @BeforeEach
    void init() {
        doReturn(SearchBusinessOffersResult.builder()
                .setTotalCount(1)
                .build())
                .when(saasService).searchAndConvertBusinessOffers(any());
    }

    @Test
    @DisplayName("Миграция белого непуша только меняет сервис линк")
    @DbUnitDataSet(before = "DatacampBusinessMigrationServiceTest.startMigration.before.csv")
    public void testStartMigrationWhite() {
        //given
        willAnswer(invocation -> Migration.MigrationStatus.newBuilder()
                .setStatus(Migration.MigrationStatus.EStatus.STARTED)
                .build()).given(dataCampShopClient).startMigration(SHOP_ID, OLD_BUSINESS_ID, NEW_BUSINESS_ID);
        //when
        migrationService.doMigrationAndLinkService(SHOP_ID, NEW_BUSINESS_ID, ACTION_ID);
        //then
        then(dataCampShopClient).shouldHaveZeroInteractions();
        //Проверяем, что бизнес изменен
        assertEquals(NEW_BUSINESS_ID, businessService.getBusinessIdByPartner(SHOP_ID));
    }

    @Test
    @DisplayName("Миграция белого пуша запрещена")
    @DbUnitDataSet(before = "DatacampBusinessMigrationServiceTest.startMigration.before.csv")
    public void testStartMigrationWhiteError() {
        //given
        paramService.setParam(
                new StringParamValue(ParamType.IS_PUSH_PARTNER, SHOP_ID, PushPartnerStatus.REAL.name()),
                ACTION_ID);
        assertThrows(RuntimeException.class, () ->
                migrationService.doMigrationAndLinkService(SHOP_ID, NEW_BUSINESS_ID, ACTION_ID));

        //then
        then(dataCampShopClient).shouldHaveZeroInteractions();
        //Проверяем, что бизнес не изменен
        assertEquals(OLD_BUSINESS_ID, businessService.getBusinessIdByPartner(SHOP_ID));
    }


    @Test
    @DisplayName("Проверяет, что миграция началась. БПМН Полная схема для Еката")
    @DbUnitDataSet(before = "DatacampBusinessMigrationServiceTest.startMigration.before.csv")
    public void testStartMigrationBpmnFull() {
        //given
        ProcessStartResponse response = new ProcessStartResponse();
        ProcessStartInstance processInstance = new ProcessStartInstance();
        processInstance.setStatus(ProcessStatus.ACTIVE);
        processInstance.processInstanceId("EXT");
        response.setRecords(List.of(processInstance));
        willAnswer(invocation -> response)
                .given(mbiBpmnClient).postProcess(any());
        //when
        migrationService.doMigrationAndLinkService(FULL_BPMN_SUPPLIER_ID, NEW_BUSINESS_ID, ACTION_ID);
        //then
        ArgumentCaptor<ProcessInstanceRequest> captor = ArgumentCaptor.forClass(ProcessInstanceRequest.class);
        then(mbiBpmnClient).should().postProcess(captor.capture());
        //проверяем, что вызвалась именно полная схема
        Assertions.assertEquals(ProcessType.MIGRATION_FULL, captor.getValue().getProcessType());
        //Проверяем, что бизнес не изменен
        assertEquals(OLD_BUSINESS_ID, businessService.getBusinessIdByPartner(FULL_BPMN_SUPPLIER_ID));
    }
}
