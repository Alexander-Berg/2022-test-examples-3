package ru.yandex.market.mbo.mdm.common.masterdata.repository;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.common_view.CommonParamViewSetting;
import ru.yandex.market.mdm.http.common_view.CommonViewType;

public class CommonParamViewSettingProjectionRepositoryImplTest extends MdmBaseDbTestClass {

    @Autowired
    protected NamedParameterJdbcTemplate jdbcTemplate;

    private CommonParamViewSettingProjectionRepositoryImpl commonParamViewSettingProjectionRepository;

    CommonParamViewSetting.Builder commonParamViewSettingBuilder = CommonParamViewSetting.newBuilder()
        .setMdmId(1)
        .setCommonViewTypeId(1)
        .setCommonViewType(
            CommonViewType.newBuilder()
                .build())
        .setMdmEntityId(1)
        .setUiOrder(1)
        .setIsEnabled(true)
        .setMdmUpdateMeta(MdmBase.MdmUpdateMeta.newBuilder().build());

    @Before
    public void setUp() {
        commonParamViewSettingProjectionRepository =
            new CommonParamViewSettingProjectionRepositoryImpl(jdbcTemplate, transactionTemplate);
    }

    @Test
    public void testCorrectReadAndWrite() {
        // given
        List<CommonParamViewSetting> settingsToWrite =
            List.of(commonParamViewSettingBuilder.build());

        // when
        commonParamViewSettingProjectionRepository.replaceAllByCommonParamViewSettings(settingsToWrite);
        List<CommonParamViewSetting> settingsFromRead =
            commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings();

        // then
        Assertions.assertThat(settingsToWrite.get(0)).isEqualTo(settingsFromRead.get(0));
    }

    @Test
    public void testCorrectTruncateBetweenWrite() {
        // given
        CommonParamViewSetting settingFirst = commonParamViewSettingBuilder.build(),
            settingSecond = commonParamViewSettingBuilder.setMdmId(2).build();

        // when
        commonParamViewSettingProjectionRepository.replaceAllByCommonParamViewSettings(
            List.of(settingFirst)
        );
        commonParamViewSettingProjectionRepository.replaceAllByCommonParamViewSettings(
            List.of(settingSecond)
        );
        List<CommonParamViewSetting> settingsFromRead =
            commonParamViewSettingProjectionRepository.findAllCommonParamViewSettings();

        // then
        Assertions.assertThat(settingsFromRead.size()).isEqualTo(1);
        Assertions.assertThat(settingFirst).isNotEqualTo(settingsFromRead.get(0));
        Assertions.assertThat(settingSecond).isEqualTo(settingsFromRead.get(0));
    }
}
