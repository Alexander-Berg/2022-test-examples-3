package ru.yandex.direct.grid.processing.service.jsonsettings;

import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.direct.grid.core.frontdb.jsonsettings.service.JsonSettingsService;
import ru.yandex.direct.grid.core.frontdb.repository.JsonSettingsRepository;
import ru.yandex.direct.grid.model.jsonsettings.GdGetJsonSettings;
import ru.yandex.direct.grid.model.jsonsettings.GdSetJsonSettings;
import ru.yandex.direct.grid.model.jsonsettings.GdUpdateJsonSettingsUnion;
import ru.yandex.direct.grid.model.jsonsettings.IdType;
import ru.yandex.direct.grid.processing.configuration.GridProcessingTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@GridProcessingTest
@RunWith(MockitoJUnitRunner.class)
public class JsonSettingsServiceTest {

    private Long uid;
    private Long clientId;

    @Mock
    private JsonSettingsRepository jsonSettingsRepository;

    @InjectMocks
    private JsonSettingsService jsonSettingsService;

    @Before
    public void initTestData() {
        uid = RandomNumberUtils.nextPositiveLong();
        clientId = RandomNumberUtils.nextPositiveLong();
        jsonSettingsService = new JsonSettingsService(jsonSettingsRepository);
    }

    @Test
    public void setRootPath_Success() {
        String json = "{\"key\":123}";
        doReturn(List.of(json)).when(jsonSettingsRepository).getJsonSettings(eq(clientId), eq(uid), anyList());

        GdSetJsonSettings input = new GdSetJsonSettings()
                .withIdType(IdType.UID_AND_CLIENT_ID)
                .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion().withJsonPath("$").withNewValue(Map.of("k",
                        123)),
                        new GdUpdateJsonSettingsUnion().withJsonPath("$").withNewValue(Map.of("newKey", 0))));

        String result = jsonSettingsService.setJsonSettings(clientId, uid, input);

        String expected = "{\"newKey\":0}";

        assertThat(result)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void set_Success() {
        String json = "{\"key\":123}";
        doReturn(List.of(json)).when(jsonSettingsRepository).getJsonSettings(eq(clientId), eq(uid), anyList());

        GdSetJsonSettings input = new GdSetJsonSettings()
                .withIdType(IdType.UID_AND_CLIENT_ID)
                .withUpdateItems(List.of(new GdUpdateJsonSettingsUnion().withJsonPath("$.key").withNewValue(Map.of()),
                        new GdUpdateJsonSettingsUnion().withJsonPath("$.key3").withNewValue(Map.of("newKey", 0))));

        String result = jsonSettingsService.setJsonSettings(clientId, uid, input);

        String expected = "{\"key\":{},\"key3\":{\"newKey\":0}}";

        assertThat(result)
                .is(matchedBy(beanDiffer(expected)));
    }

    @Test
    public void get_Success() {
        String json = "{\"key\":{},\"key3\":{\"newKey\":0}}";
        doReturn(List.of(json)).when(jsonSettingsRepository).getJsonSettings(eq(clientId), isNull(), anyList());

        GdGetJsonSettings input = new GdGetJsonSettings()
                .withIdType(IdType.CLIENT_ID)
                .withJsonPath(List.of("$.key"));

        var result = jsonSettingsService.getJsonSettings(clientId, uid, input);

        var expected = List.of("{\"key\":{},\"key3\":{\"newKey\":0}}");

        assertThat(result)
                .is(matchedBy(beanDiffer(expected)));
    }
}
