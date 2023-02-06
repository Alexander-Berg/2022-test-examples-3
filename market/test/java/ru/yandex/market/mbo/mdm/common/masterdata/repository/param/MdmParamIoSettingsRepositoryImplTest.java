package ru.yandex.market.mbo.mdm.common.masterdata.repository.param;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamExternals;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoSettings;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.masterdata.TestDataUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author amaslak
 */
@SuppressWarnings("checkstyle:MagicNumber")
public class MdmParamIoSettingsRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmParamIoSettingsRepository repository;

    @Autowired
    private MdmParamRepository mdmParamRepository;

    private EnhancedRandom random;

    @Before
    public void setup() {
        random = TestDataUtils.defaultRandom(4);
        repository.deleteAll();
        mdmParamRepository.deleteAll();
    }

    @Test
    public void whenInserShouldFindById() {
        MdmParam param = param();
        mdmParamRepository.insert(param);

        MdmParamIoSettings meta = paramSettings(param.getId());
        repository.insert(meta);

        MdmParamIoSettings found = repository.findById(meta.getId());
        assertThat(meta).isEqualTo(found);
    }

    @Test
    public void whenInsertShouldAutoCreateNewId() {
        MdmParam param = param();
        mdmParamRepository.insert(param);

        MdmParamIoSettings meta = paramSettings(param.getId()).setId(null);
        MdmParamIoSettings inserted = repository.insert(meta);
        assertThat(inserted.getId()).isNotNull();

        MdmParamIoSettings found = repository.findById(inserted.getId());
        assertThat(inserted).isEqualTo(found);
    }

    @Test
    public void whenManyIoTypesShouldFilterByIoType() {
        MdmParam param = param();
        mdmParamRepository.insert(param);

        MdmParamIoSettings categoryParam = paramSettings(param.getId()).setIoType(MdmParamIoType.CATEGORY_EXCEL_EXPORT);
        repository.insert(categoryParam);
        MdmParamIoSettings mskuParam = paramSettings(param.getId()).setIoType(MdmParamIoType.MSKU_UI_VIEW);
        repository.insert(mskuParam);

        var mdmParamsForCategory = repository.findMdmParamsForIoType(MdmParamIoType.CATEGORY_EXCEL_EXPORT);
        assertThat(mdmParamsForCategory).containsExactly(categoryParam);
    }

    private MdmParamIoSettings paramSettings(long mdmParamId) {
        return random.nextObject(MdmParamIoSettings.class).setMdmParamId(mdmParamId);
    }

    private MdmParam param() {
        MdmParamExternals externals = random.nextObject(MdmParamExternals.class, "optionRenders");
        return random.nextObject(MdmParam.class, "externals").setExternals(externals);
    }

}
