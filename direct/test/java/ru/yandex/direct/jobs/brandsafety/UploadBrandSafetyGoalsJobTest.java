package ru.yandex.direct.jobs.brandsafety;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.crypta.repository.CryptaSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.testing.repository.TestCryptaSegmentRepository;
import ru.yandex.direct.core.util.ReflectionTranslator;
import ru.yandex.direct.jobs.configuration.DirectExportYtClustersParametersSource;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.ytwrapper.YtPathUtil;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.yql.YqlConnection;
import ru.yandex.yql.YqlDataSource;
import ru.yandex.yql.YqlPreparedStatement;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.direct.jobs.util.yt.YtEnvPath.relativePart;

@JobsTest
@ExtendWith(SpringExtension.class)
class UploadBrandSafetyGoalsJobTest {

    @Autowired
    private CryptaSegmentRepository cryptaSegmentRepository;

    @Autowired
    private ReflectionTranslator reflectionTranslator;

    @Autowired
    private TestCryptaSegmentRepository testCryptaSegmentRepository;

    private UploadBrandSafetyGoalsJob job;

    private YqlConnection connection;


    @BeforeEach
    void setUp() throws SQLException {
        var ytProvider = mock(YtProvider.class);
        var parametersSource = new DirectExportYtClustersParametersSource(Collections.singletonList(YtCluster.HAHN));

        job = new UploadBrandSafetyGoalsJob(
                ytProvider,
                cryptaSegmentRepository,
                reflectionTranslator,
                parametersSource
        ) {
            @Override
            public String getParam() {
                return YtCluster.HAHN.name();
            }
        };

        var goal = new Goal()
                .withId(4_294_967_298L)
                .withParentId(4_294_967_299L)
                .withType(GoalType.BRANDSAFETY)
                .withName("Test")
                .withTankerNameKey("brandsafety_adult_name")
                .withTankerDescriptionKey("brandsafety_adult_description");

        var featureGoal = new Goal()
                .withId(4_294_967_302L)
                .withParentId(null)
                .withType(GoalType.BRANDSAFETY)
                .withName("Under feature")
                .withTankerNameKey("brandsafety_politics_name")
                .withTankerDescriptionKey("brandsafety_politics_description");

        var contentCategoryGoal = new Goal()
                .withId(4_294_968_296L)
                .withParentId(4_294_967_299L)
                .withParentId(0L)
                .withType(GoalType.CONTENT_CATEGORY)
                .withName("Авто")
                .withTankerNameKey("content_genre_auto_name")
                .withTankerDescriptionKey("content_genre_auto_description");

        var contentGenreGoal = new Goal()
                .withId(4_294_970_296L)
                .withParentId(null)
                .withType(GoalType.CONTENT_GENRE)
                .withName("Советское кино")
                .withTankerNameKey("content_genre_soviet_cinema_name")
                .withTankerDescriptionKey("content_genre_soviet_cinema_description");

        testCryptaSegmentRepository.addAll(Set.of(
                (Goal) goal,
                (Goal) featureGoal,
                (Goal) contentCategoryGoal,
                (Goal) contentGenreGoal)
        );

        var yqlDataSource = mock(YqlDataSource.class);
        var statement = mock(YqlPreparedStatement.class);

        connection = mock(YqlConnection.class);
        var ytClusterConfig = mock(YtClusterConfig.class);

        given(ytProvider.getYql(any(), any())).willReturn(yqlDataSource);
        given(yqlDataSource.getConnection()).willReturn(connection);
        given(connection.prepareStatement(any())).willReturn(statement);
        given(ytProvider.getClusterConfig(any())).willReturn(ytClusterConfig);
        given(ytClusterConfig.getHome()).willReturn("//home/direct");
    }

    @Test
    void shouldGatherYqlQuery() throws SQLException {
        var home = "//home/direct";
        var ytPath = YtPathUtil.generatePath(home, relativePart(), "export/brandsafety_categories");
        var expectedQuery = String.format(
                "INSERT INTO `%s` WITH TRUNCATE (id, parent_id, name, crypta_goal_type, bb_keyword, bb_keyword_value) VALUES (?,?,?,?,?,?),(?,?,?,?,?,?),(?,?,?,?,?,?),(?,?,?,?,?,?)",
                ytPath
        );

        job.execute();

        verify(connection, times(1)).prepareStatement(eq(expectedQuery));
    }

    @Test
    void shouldNotFailIfThereAreNoDataInCryptaSegments() throws SQLException {
        testCryptaSegmentRepository.clean();

        job.execute();

        verify(connection, never()).prepareStatement(any());
    }
}
