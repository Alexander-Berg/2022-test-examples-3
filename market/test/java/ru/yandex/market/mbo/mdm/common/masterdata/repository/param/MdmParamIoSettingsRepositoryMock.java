package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import ru.yandex.market.mbo.lightmapper.test.IntGenericMapperRepositoryMock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoSettings;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;

/**
 * @author amaslak
 */
public class MdmParamIoSettingsRepositoryMock extends IntGenericMapperRepositoryMock<MdmParamIoSettings>
    implements MdmParamIoSettingsRepository {

    public MdmParamIoSettingsRepositoryMock() {
        super(MdmParamIoSettings::setId, MdmParamIoSettings::getId);
    }

    @Override
    public List<MdmParamIoSettings> findMdmParamsForIoType(@Nonnull MdmParamIoType type) {
        return findAll().stream().filter(s -> s.getIoType() == type).collect(Collectors.toList());
    }
}
