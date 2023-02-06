package ru.yandex.direct.jobs.bannersystem.dataimport;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.direct.jobs.configuration.GenocideLogTransferParameter;
import ru.yandex.direct.jobs.configuration.GenocideLogsTransferParametersSource;
import ru.yandex.direct.ytwrapper.client.YtClusterConfig;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtOperator;
import ru.yandex.direct.ytwrapper.model.YtTable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

class TransferGenocideLogUtilsTest {

    private static final String FRESHNESS_TIME_ATTRIBUTE = "freshness_time";

    private TransferGenocideLogUtils utils;

    @Mock
    private YtOperator mockedYtOperator;

    @Mock
    private YtClusterConfig mockedYtClusterConfig;

    @Mock
    private YtProvider mockedYtProvider;

    @Mock
    private Consumer<GenocideLogTransferParameter> copyFunction;

    private GenocideLogTransferParameter parameter;
    private YtTable sourceTable;
    private YtTable sourceTable0;
    private YtTable sourceTable1;
    private YtTable destinationTable;

    @BeforeEach
    void init() {

        MockitoAnnotations.initMocks(this);

        List<GenocideLogTransferParameter> allParams = new GenocideLogsTransferParametersSource().getAllParamValues();
        parameter = allParams.get(new Random().nextInt(allParams.size()));

        when(mockedYtClusterConfig.getHome()).thenReturn("/home");
        when(mockedYtProvider.getClusterConfig(any())).thenReturn(mockedYtClusterConfig);
        when(mockedYtProvider.getOperator(any())).thenReturn(mockedYtOperator);

        utils = new TransferGenocideLogUtils(mockedYtProvider);

        sourceTable = utils.getSourceTable(parameter.getYabsCsKey());
        sourceTable0 = utils.getDividedSourceTables(parameter.getYabsCsKey()).get(0);
        sourceTable1 = utils.getDividedSourceTables(parameter.getYabsCsKey()).get(1);
        destinationTable = utils.getDestinationTable(parameter);
    }

    @Test
    void noDestinationTableTest() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(false);

        utils.process(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void divided_noDestinationTable_copy() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(false);
        when(mockedYtOperator.readTableModificationTime(sourceTable0)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableModificationTime(sourceTable1)).thenReturn("2019-03-26T12:09:26.622289Z");

        utils.processDividedTables(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void noFreshnessDestinationTableTest() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE)).thenReturn(null);

        utils.process(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void divided_noFreshnessDestinationTable_copy() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable0)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableModificationTime(sourceTable1)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE)).thenReturn(null);

        utils.processDividedTables(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void olderDestinationTableTest() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable))
                .thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE))
                .thenReturn("2019-03-26T10:09:26.622289Z");

        utils.process(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void divided_destinationTableIsOld_copy() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable0)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableModificationTime(sourceTable1)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE))
                .thenReturn("2019-03-26T11:09:26.622Z");

        utils.processDividedTables(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void divided_destinationTableIsOlderThanAverage_copy() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable0)).thenReturn("2019-03-26T12:12:26.622289Z");
        when(mockedYtOperator.readTableModificationTime(sourceTable1)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE))
                .thenReturn("2019-03-26T12:10:26.622Z");

        utils.processDividedTables(parameter, copyFunction);

        verify(copyFunction).accept(parameter);
    }

    @Test
    void divided_destinationTableIsAsOldAsAverage_dontCopy() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable0)).thenReturn("2019-03-26T12:11:26.622289Z");
        when(mockedYtOperator.readTableModificationTime(sourceTable1)).thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE))
                .thenReturn("2019-03-26T12:10:26.622Z");

        utils.processDividedTables(parameter, copyFunction);

        verifyZeroInteractions(copyFunction);
    }

    @Test
    void actualDestinationTableTest() {
        when(mockedYtOperator.exists(destinationTable)).thenReturn(true);
        when(mockedYtOperator.readTableModificationTime(sourceTable))
                .thenReturn("2019-03-26T12:09:26.622289Z");
        when(mockedYtOperator.readTableStringAttribute(destinationTable, FRESHNESS_TIME_ATTRIBUTE))
                .thenReturn("2019-03-26T12:09:26.622289Z");

        utils.process(parameter, copyFunction);

        verifyZeroInteractions(copyFunction);
    }
}
