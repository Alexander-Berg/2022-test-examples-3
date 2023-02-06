package ru.yandex.market.core.stats;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.common.util.IOUtils;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.billing.pp.PpDictionary;
import ru.yandex.market.core.report.model.Field;
import ru.yandex.market.core.report.model.NodeField;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

/**
 * Тесты для {@link PlacementPPGroupRegistry}.
 *
 * @author vbudnev
 */

public class PlacementPPGroupRegistryTest extends FunctionalTest {

    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    private PlacementPPGroupRegistry registryV0;
    private PlacementPPGroupRegistry registryV1;
    private PlacementPPGroupRegistry registryV2;

    @BeforeEach
    public void before() {
        //чтобы между тестами не было кэшированных данных, и не дергать .refresh явно
        registryV0 = new PlacementPPGroupRegistry(namedParameterJdbcTemplate, 0);
        registryV1 = new PlacementPPGroupRegistry(namedParameterJdbcTemplate, 1);
        registryV2 = new PlacementPPGroupRegistry(namedParameterJdbcTemplate, 2);
    }

    /**
     * Проверяем, что существует кэш и пока TTL не исчерпан, нет обращений в базу и данные вовзаращаются те же.
     * Если повторное обращение обратится к базе, то получим npe.
     */
    @DbUnitDataSet(before = {"db/PlacementPpData.before.csv"})
    @Test
    public void test_getMethods_when_calledBeforeTllExpires_should_returnCachedData() {
        List<Long> groups = registryV2.getPpGroups();
        Map<Long, Long> groupsByPp = registryV2.getPpGroupByPpCode();

        List<Long> cachedGroups = registryV2.getPpGroups();
        Map<Long, Long> cachedGroupsByPp = registryV2.getPpGroupByPpCode();

        registryV2.setNamedParameterJdbcTemplate(null);

        assertThat(groups, equalTo(cachedGroups));
        assertThat(groupsByPp, equalTo(cachedGroupsByPp));
    }

    /**
     * Проверяем структуру возвращаемых данных о группах и шапку для отчета.
     * Группа 4 (карточка модели) в выдаче отчета версии 1 отсутствует.
     * Порядок групп важен.
     */
    @DbUnitDataSet(before = {"db/PlacementPpData.before.csv"})
    @Test
    public void test_fetchData() {
        List<Long> groups = registryV1.getPpGroups();
        Map<Long, Long> groupsByPp = registryV1.getPpGroupByPpCode();

        assertThat(groups, contains(3L, 6L, 5L, 7L, 8L, 9L, 10L));
        assertThat(
                groupsByPp,
                equalTo(
                        ImmutableMap.<Long, Long>builder()
                                .put(611L, 3L)
                                .put(612L, 3L)
                                .put(614L, 5L)
                                .put(615L, 6L)
                                .put(616L, 7L)
                                .put(617L, 8L)
                                .put(618L, 9L)
                                .put(619L, 10L)
                                .put(1002L, 11L)
                                .build()
                )
        );
    }

    /**
     * Проверяем структуру возвращаемых данных о группах и шапку для отчета в случае если в базе для словаря
     * мест размещений групп больше, чем поддерживает отчет.
     * Так как фронт динамически не ориентируется, то важно поддерживать структуру такую, на которую смотрит фронт,
     * и не разлетаться при добавлении новых групп, еще не протянутых во фронт.
     * В выдаче для версии 2 добавлена инфа оп группе 11.
     * Порядок групп важен.
     */
    @DbUnitDataSet(before = {"db/PlacementPpGroupsData_SomeUnknown.before.csv"})
    @Test
    public void test_fetchData_when_ppForIgnoredGroups() {
        List<Long> groups = registryV2.getPpGroups();
        Map<Long, Long> groupsByPp = registryV2.getPpGroupByPpCode();

        assertThat(groups, contains(3L, 6L, 5L, 7L, 8L, 9L, 10L, 11L));
        assertThat(
                groupsByPp,
                equalTo(
                        ImmutableMap.<Long, Long>builder()
                                .put(612L, 3L)
                                .put(614L, 5L)
                                .put(615L, 6L)
                                .put(616L, 7L)
                                .put(617L, 8L)
                                .put(618L, 9L)
                                .put(619L, 10L)
                                .put(1002L, 11L)
                                .put(621L, 12L)
                                .put(622L, 13L)
                                .build()
                )
        );
    }

    /**
     * Проверяем структуру мета информации для отчета по кликам.
     * <p>
     * Проверка структуры мета данных сделана через json сериализацию.
     * Попытка сделать проверку через зоопарк матчеров для Field и наследников или relfectinons объектов оказалась
     * нечитабельна совершенно.
     * Да, есть кейсы в которых это не сработает, но для наглядности это позволяет легко увидеть и проверить структуру.
     */
    @DbUnitDataSet(before = {"db/PlacementPpData.before.csv"})
    @Test
    public void test_clicksMetaStructure() throws IOException, JSONException {
        registryV2.getPpGroups();
        NodeField actualRoot = registryV2.getMetaData().getRoot();

        String expectedOutput = IOUtils.readInputStream(
                getClass().getResourceAsStream("PlacementPPGroupRegistryTest_clicksMetaStructure.json")
        );

        String actualResult = reportObjectAsJson(actualRoot);

        JSONAssert.assertEquals(expectedOutput, actualResult, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Проверяем структуру мета информации для отчета по показам.
     * Почему json - см {@link #test_clicksMetaStructure}.
     */
    @DbUnitDataSet(before = {"db/PlacementPpData.before.csv"})
    @Test
    public void test_showsMetaStructure() throws IOException, JSONException {
        registryV2.getPpGroups();
        NodeField actualRoot = registryV2.getShowsMetaData().getRoot();

        String expectedOutput = IOUtils.readInputStream(
                getClass().getResourceAsStream("PlacementPPGroupRegistryTest_showsMetaStructure.json")
        );

        String actualResult = reportObjectAsJson(actualRoot);

        JSONAssert.assertEquals(expectedOutput, actualResult, JSONCompareMode.NON_EXTENSIBLE);
    }

    /**
     * Проверяем структуру возвращаемых данных о группах и шапку для отчета старой версии.
     * Для словаря {@link PpDictionary#PLACEMENT} учитываются только группы 3,4,5,6.
     * Порядок групп важен.
     */
    @DbUnitDataSet(before = {"db/PlacementPpData.before.csv"})
    @Test
    public void test_fetchData_when_oldReportVersionRequested() {
        List<Long> groups = registryV0.getPpGroups();
        Map<Long, Long> groupsByPp = registryV0.getPpGroupByPpCode();

        assertThat(groups, contains(3L, 4L, 5L, 6L));
        assertThat(
                groupsByPp,
                equalTo(
                        ImmutableMap.<Long, Long>builder()
                                .put(611L, 3L)
                                .put(612L, 3L)
                                .put(617L, 4L)
                                .put(614L, 5L)
                                .put(615L, 6L)
                                .build()
                )
        );
    }

    /**
     * Вспомогтаельный метод, переводящий объекты, в которых пресдставлен отчет в json, для проверки структуры.
     */
    private String reportObjectAsJson(Field targetRootField) throws IOException {
        StringWriter writer = new StringWriter();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.writeValue(writer, targetRootField);
        return writer.toString();
    }

}
