package ru.yandex.market.pers.qa.tms.toloka;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.cataloger.CatalogerClient;
import ru.yandex.market.cataloger.model.GetTreeResponseWrapper;
import ru.yandex.market.pers.qa.PersQaTmsTest;
import ru.yandex.market.pers.qa.client.model.QuestionType;
import ru.yandex.market.pers.qa.service.toloka.TolokaServiceHelper;
import ru.yandex.market.pers.qa.service.toloka.model.TolokaEntity;
import ru.yandex.market.pers.yt.YtClient;
import ru.yandex.market.pers.yt.YtClientProvider;
import ru.yandex.market.pers.yt.YtClusterType;
import ru.yandex.market.util.FormatUtils;
import ru.yandex.market.util.db.ConfigurationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.01.2019
 */
public class TolokaTestsBase extends PersQaTmsTest {
    @Autowired
    protected YtClientProvider ytClientProvider;

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("tolokaConfigurationService")
    protected ConfigurationService configurationService;

    @Autowired
    protected CatalogerClient catalogerClient;

    @Override
    protected void resetMocks() {
        super.resetMocks();
        configurationService.deleteValue(TolokaServiceHelper.CFG_YT_CLUSTER_ID);
    }

    protected YtClient getYtClient(YtClusterType clusterType) {
        return ytClientProvider.getClient(clusterType);
    }

    protected void checkYtWasNotCalled() {
        for (YtClusterType clusterType : YtClusterType.values()) {
            checkYtWasNotCalled(clusterType);
        }
    }

    protected void checkYtWasNotCalled(YtClusterType clusterType) {
        verifyZeroInteractions(getYtClient(clusterType));
    }

    protected void checkYtWasOnlyTriedToCall(YtClusterType clusterType) {
        YtClient ytClient = getYtClient(clusterType);
        verify(ytClient, times(1)).doInTransaction(any());
        verifyNoMoreInteractions(ytClient);
    }

    protected void imitateYtFailed() {
        for (YtClusterType clusterType : YtClusterType.values()) {
            imitateYtFailed(clusterType);
        }
    }

    protected void imitateYtFailed(YtClusterType clusterType) {
        doThrow(new YtDisabledException("YT Cluster disabled: " + clusterType.getCode()))
            .when(getYtClient(clusterType))
            .doInTransaction(any());

        doThrow(new YtDisabledException("YT Cluster disabled: " + clusterType.getCode()))
            .when(getYtClient(clusterType))
            .exists(any());
    }

    protected void changeUploadYtCluster(YtClusterType clusterType) {
        configurationService.mergeValue(TolokaServiceHelper.CFG_YT_CLUSTER_ID, clusterType.getId());
    }

    public static final class YtDisabledException extends RuntimeException {
        public YtDisabledException(String message) {
            super(message);
        }
    }

    protected void checkEntity(TolokaEntity tolokaEntity,
                               QuestionType entityType,
                               long entityId,
                               String text,
                               String url) {
        assertNotNull(tolokaEntity);
        assertEquals(entityType, tolokaEntity.getEntityTypeEnum());
        assertEquals(entityId, tolokaEntity.getEntityIdLong());
        assertEquals(text, tolokaEntity.getText());
        assertEquals(url, tolokaEntity.getImage());
    }

    @NotNull
    protected GetTreeResponseWrapper mockCategory(long hid, String name) {
        return FormatUtils.fromJson(
            String.format("{\"result\":{\"id\":%d,\"name\":\"%s\",\"fullName\":\"%s\"}}", hid, name, name),
            GetTreeResponseWrapper.class);
    }
}
