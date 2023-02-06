package ru.yandex.direct.internaltools.configuration;

import com.yandex.ydb.table.TableClient;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.canvas.tools_client.CanvasToolsClient;
import ru.yandex.direct.core.testing.configuration.CoreTestingConfiguration;
import ru.yandex.direct.oneshot.core.entity.oneshot.service.OneshotStartrekService;

import static ru.yandex.direct.configuration.YdbConfiguration.HOURGLASS_YDB_TABLE_CLIENT_BEAN;

@Configuration
@Import({InternalToolsConfiguration.class, CoreTestingConfiguration.class})
public class InternalToolsTestingConfiguration {
    @MockBean
    CanvasToolsClient canvasToolsClient;

    @MockBean
    OneshotStartrekService oneshotStartrekService;

    @MockBean(name = HOURGLASS_YDB_TABLE_CLIENT_BEAN)
    TableClient tableClient;

}
