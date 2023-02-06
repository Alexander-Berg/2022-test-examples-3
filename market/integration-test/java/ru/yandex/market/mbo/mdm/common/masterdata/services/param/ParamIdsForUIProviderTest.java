package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoSettings;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.MdmParamIoSettingsRepository;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MdmBaseIntegrationTestClass;
import ru.yandex.market.mboc.common.utils.MdmProperties;

public class ParamIdsForUIProviderTest extends MdmBaseIntegrationTestClass {

    @Autowired
    private MdmParamProvider mdmParamProvider;
    @Autowired
    private MdmParamIoSettingsRepository ioSettingsRepository;
    @Autowired
    private StorageKeyValueService keyValueService;

    @Test
    @Ignore
    public void getParamIdsForIoType() {
        keyValueService.putValue(MdmProperties.USE_BMDM_FOR_ALL_UI_ATTRIBUTES, true);
        var ioSettings = ioSettingsRepository.findMdmParamsForIoType(MdmParamIoType.SSKU_UI_VIEW)
                .stream().filter(MdmParamIoSettings::isEnabled).collect(Collectors.toList());

        var paramIdsFromBmdm = mdmParamProvider.getParamIdsForIoType(MdmParamIoType.SSKU_UI_VIEW);

        Assertions.assertThat(paramIdsFromBmdm).containsExactlyInAnyOrderElementsOf(
                ioSettings.stream().map(MdmParamIoSettings::getMdmParamId).collect(Collectors.toList()));
    }
}
