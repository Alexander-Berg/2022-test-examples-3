package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import ru.yandex.market.mbo.mdm.common.infrastructure.MdmMonitoringResult;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.CommonParamViewSettingProjectionRepositoryImpl;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MdmEntityTypeProjectionRepositoryImp;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.CachedMetadataProvider;
import ru.yandex.market.mbo.mdm.common.service.bmdm.converter.param.BmdmEntityTypeToParamsConverterImpl;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.tms.quartz2.model.MonitoringStatus;


@Import({
    MdmEntityTypeProjectionRepositoryImp.class,
    CommonParamViewSettingProjectionRepositoryImpl.class,
    CachedMetadataProvider.class,
    BmdmEntityTypeToParamsConverterImpl.class,
    BmdmMetadataMonitoringServiceImpl.class,
    ParamIdsForUIProviderImplBmdmCache.class,
    ParamIdsForUIProviderImplMdmRepo.class
})
public class BmdmMetadataMonitoringServiceImplTest extends MdmBaseDbTestClass {
    @Autowired
    private BmdmMetadataMonitoringServiceImpl service;

    @Test
    public void shouldPassAllParamsCheckOnTestData() {
        // when
        MdmMonitoringResult result = service.checkBmdmMetadataCorrectness();

        // then
        Assertions.assertThat(result.getStatus()).isEqualTo(MonitoringStatus.OK);
    }
}
