package ru.yandex.mbo.tool.jira.MBO37345;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.mbo.db.modelstorage.ModelStorageService;
import ru.yandex.market.mbo.db.modelstorage.data.group.ModelSaveGroup;
import ru.yandex.market.mbo.gwt.models.modelstorage.CommonModel;
import ru.yandex.market.mbo.gwt.models.rules.CommonModelBuilder;
import ru.yandex.market.mbo.user.AutoUser;
import ru.yandex.mbo.tool.jira.utils.CommandLineTool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static ru.yandex.mbo.tool.jira.MBO37345.UpForceSyncVersionTool.SYNC_VERSION_PARAM_ID;
import static ru.yandex.mbo.tool.jira.MBO37345.UpForceSyncVersionTool.SYNC_VERSION_XSL_NAME;

@SuppressWarnings("checkstyle:MagicNumber")
@RunWith(MockitoJUnitRunner.class)
public class UpForceSyncVersionToolTest {

    @Mock
    private AutoUser autoUser;
    @Mock
    private ModelStorageService storage;

    private CommandLineTool tool;

    @Before
    public void setUp() {
        tool = new UpForceSyncVersionTool(autoUser, storage);
        given(autoUser.getId()).willReturn(1L);
    }

    @Test
    public void shouldUpSyncVersionForMsku() throws Exception {
        // given
        var initialModel = CommonModelBuilder.newBuilder(2L, 10L)
            .parameterValues(SYNC_VERSION_PARAM_ID, SYNC_VERSION_XSL_NAME, BigDecimal.valueOf(10L))
            .currentType(CommonModel.Source.SKU)
            .endModel();
        URI mskuIdsFile = getClass().getClassLoader().getResource("MBO37345/test.csv").toURI();
        given(storage.searchByIds(List.of(2L))).willReturn(List.of(initialModel));

        // when
        tool.start(new String[]{"-i", mskuIdsFile.getPath(), "--dry-run", "false"});

        // then
        ArgumentCaptor<ModelSaveGroup> toSaveCaptor = ArgumentCaptor.forClass(ModelSaveGroup.class);
        verify(storage).saveModels(toSaveCaptor.capture(), any());
        ModelSaveGroup toSaveGroup = toSaveCaptor.getValue();

        assertThat(toSaveGroup.getModels()).hasSize(1);
        assertThat(toSaveGroup.getById(2L).getParameterValues(SYNC_VERSION_PARAM_ID).getNumericValues())
            .containsExactly(BigDecimal.valueOf(11L));
    }

    @Test
    public void shouldCreateNewSyncVersionForMskuWhenNotSet() throws Exception {
        // given
        var initialModel = CommonModelBuilder.newBuilder(2L, 10L)
            .currentType(CommonModel.Source.SKU)
            .endModel();
        URI mskuIdsFile = getClass().getClassLoader().getResource("MBO37345/test.csv").toURI();
        given(storage.searchByIds(List.of(2L))).willReturn(List.of(initialModel));

        // when
        tool.start(new String[]{"-i", mskuIdsFile.getPath(), "--dry-run", "false"});

        // then
        ArgumentCaptor<ModelSaveGroup> toSaveCaptor = ArgumentCaptor.forClass(ModelSaveGroup.class);
        verify(storage).saveModels(toSaveCaptor.capture(), any());
        ModelSaveGroup toSaveGroup = toSaveCaptor.getValue();

        assertThat(toSaveGroup.getModels()).hasSize(1);
        assertThat(toSaveGroup.getById(2L).getParameterValues(SYNC_VERSION_PARAM_ID).getNumericValues())
            .containsExactly(BigDecimal.valueOf(1L));
    }
}
