package ru.yandex.market.logistics.lms.client.controller.yt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;
import ru.yandex.market.logistics.lom.utils.YtLmsVersionsUtils;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

public abstract class LmsLomYtControllerAbstractTest extends AbstractContextualTest {

    protected static final String YT_ACTUAL_VERSION = "2022-03-02T08:05:24Z";

    @Autowired
    protected LmsYtProperties lmsYtProperties;

    @Autowired
    protected Yt hahnYt;

    @Autowired
    protected YtTables ytTables;

    @BeforeEach
    void setUp() {
        doReturn(ytTables)
            .when(hahnYt).tables();

        YtLmsVersionsUtils.mockYtVersionTable(ytTables, lmsYtProperties, YT_ACTUAL_VERSION);
    }

    @AfterEach
    void tearDown() {
        verify(hahnYt, times(2)).tables();

        verifyNoMoreInteractions(ytTables, hahnYt);
    }
}
