package ru.yandex.market.delivery.rupostintegrationapp.service.component.pickuppointstariff.updater;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.PickuppointRepository;
import ru.yandex.market.delivery.rupostintegrationapp.dao.pickuppoint.RussianPostPickupPoint;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatus;
import ru.yandex.market.delivery.rupostintegrationapp.log.JobStatusLogger;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.pickuppointstariff.updater.tariffmapping.RussianpostTariffMap;
import ru.yandex.market.delivery.rupostintegrationapp.service.component.pickuppointstariff.updater.tariffmapping.RussianpostTariffRequestSettingMap;
import ru.yandex.market.delivery.russianpostapiclient.bean.gettariff.Dimension;
import ru.yandex.market.delivery.russianpostapiclient.bean.gettariff.GetTariffRequest;
import ru.yandex.market.delivery.russianpostapiclient.bean.gettariff.GetTariffResponse;
import ru.yandex.market.delivery.russianpostapiclient.client.RussianPostApiClient;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PickupPointTariffUpdaterTest extends BaseTest {

    private static final int UPDATE_LIMIT = 100;

    @Mock
    private RussianPostApiClient client;

    @Mock
    private PickupPointTariffUpdateConsumer consumer;

    @Mock
    private PickuppointRepository repository;

    @Mock
    private RussianpostTariffMap russianpostTariffMap;

    @Mock
    private JobStatusLogger jobStatusLogger;

    @Test
    void testUpdateFalse() {
        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );
        updater.update();
        verify(repository, never()).findAndUpdateTariffsPickupPoints(anyInt());
    }

    @Test
    void testUpdateTrue() {
        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(new ArrayList<>());
        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, true, UPDATE_LIMIT
        );
        updater.update();
        verify(repository, times(1)).findAndUpdateTariffsPickupPoints(anyInt());
        verify(jobStatusLogger).writeToLog(JobStatus.SUCCESS, PickupPointTariffUpdater.class,
            "\"PickupPoint Tariff update has been successfully finished\"");
    }

    @Test
    void testForcePickupPointUpdate() {
        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(new ArrayList<>());
        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );
        updater.forcePickupPointUpdate();
        verify(repository, times(1)).findAndUpdateTariffsPickupPoints(anyInt());
        verify(jobStatusLogger).writeToLog(JobStatus.SUCCESS, PickupPointTariffUpdater.class,
            "\"PickupPoint Tariff update has been successfully finished\"");
    }

    @Test
    void testUpdatePickupPointNoTariffsToCheck() throws Throwable {
        when(repository.findPickupPointByid(anyInt())).thenReturn(new RussianPostPickupPoint());
        when(russianpostTariffMap.getMapping()).thenReturn(new ArrayList<>());

        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );

        updater.updatePickupPoint(1);

        verify(consumer, times(1)).reset();
        verify(russianpostTariffMap, times(1)).getMapping();
        verify(repository, times(1)).findPickupPointByid(anyInt());
        verify(consumer, never()).consume(anyObject());
    }

    private void defaultChecks() {
        verify(consumer, times(1)).reset();
        verify(russianpostTariffMap, times(1)).getMapping();
        verify(repository, times(1)).findAndUpdateTariffsPickupPoints(anyInt());
    }

    @Test
    void testUpdatePickupPointsNoTariffsToCheck() throws Throwable {
        List<RussianPostPickupPoint> pp = Collections.singletonList(new RussianPostPickupPoint());

        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(pp);
        when(russianpostTariffMap.getMapping()).thenReturn(new ArrayList<>());

        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );

        updater.updatePickupPoints();

        defaultChecks();
        verify(consumer, never()).consume(anyObject());
    }

    @Test
    void testUpdatePickupPointsFalseResponseNoIndex() throws Throwable {
        List<RussianPostPickupPoint> pp = Collections.singletonList(new RussianPostPickupPoint());

        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(pp);

        List<RussianpostTariffRequestSettingMap> mapList = Collections.singletonList(getSettingMap("aha", "ahaha"));

        when(russianpostTariffMap.getMapping()).thenReturn(mapList);

        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );

        softly.assertThatThrownBy(updater::updatePickupPoints)
            .isInstanceOf(PickupPointTariffUpdateException.class)
            .hasMessage("Index must not be null");

        defaultChecks();
        verify(consumer, never()).consume(anyObject());
        verify(client, never()).getTariff(anyObject());
    }

    @Test
    void testUpdatePickupPointsFalseResponseNoRates() throws Throwable {
        RussianPostPickupPoint point = new RussianPostPickupPoint();
        point.setIndex("119021");
        List<RussianPostPickupPoint> pp = Collections.singletonList(point);

        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(pp);

        List<RussianpostTariffRequestSettingMap> mapList = Collections.singletonList(getSettingMap("aha", "ahaha"));

        when(russianpostTariffMap.getMapping()).thenReturn(mapList);

        when(client.getTariff(anyObject())).thenReturn(new GetTariffResponse());

        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );

        softly.assertThatThrownBy(updater::updatePickupPoints)
            .as("Asserting cathced exception type")
            .isInstanceOf(NullPointerException.class);

        defaultChecks();
        verify(consumer, never()).consume(anyObject());
        verify(client, times(1)).getTariff(anyObject());
    }


    @Test
    void testUpdatePickupPointsResponseRates() throws Throwable {
        RussianPostPickupPoint point = new RussianPostPickupPoint();
        point.setIndex("119021");
        List<RussianPostPickupPoint> pp = Collections.singletonList(point);

        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(pp);

        List<RussianpostTariffRequestSettingMap> mapList = Collections.singletonList(getSettingMap("aha", "ahaha"));

        when(russianpostTariffMap.getMapping()).thenReturn(mapList);

        GetTariffResponse response = new GetTariffResponse();
        response.setTotalRate(100);

        when(client.getTariff(anyObject())).thenReturn(response);

        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );

        updater.updatePickupPoints();

        defaultChecks();
        verify(consumer, times(1)).consume(anyObject());
        verify(client, times(1)).getTariff(anyObject());
    }


    @Test
    void testUpdatePickupPointsMultiSettingsResponseRates() throws Throwable {
        RussianPostPickupPoint point = new RussianPostPickupPoint();
        point.setIndex("119021");
        List<RussianPostPickupPoint> pp = Collections.singletonList(point);

        when(repository.findAndUpdateTariffsPickupPoints(anyInt())).thenReturn(pp);

        List<RussianpostTariffRequestSettingMap> mapList = Arrays.asList(
            getSettingMap("aha", "ahaha"),
            getSettingMap("aha2", "ahaha2")
        );

        when(russianpostTariffMap.getMapping()).thenReturn(mapList);

        GetTariffResponse response = new GetTariffResponse();
        response.setTotalRate(100);

        when(client.getTariff(anyObject())).thenReturn(response);

        PickupPointTariffUpdater updater = new PickupPointTariffUpdater(
            client, consumer, repository, russianpostTariffMap, jobStatusLogger, false, UPDATE_LIMIT
        );

        updater.updatePickupPoints();

        defaultChecks();
        verify(consumer, times(1)).consume(anyObject());
        verify(client, times(2)).getTariff(anyObject());
    }

    private RussianpostTariffRequestSettingMap getSettingMap(String name, String tag) {
        GetTariffRequest request = new GetTariffRequest();
        request.setMass(1000);
        request.setDimension(new Dimension(100, 100, 100));

        RussianpostTariffRequestSettingMap settingMap = new RussianpostTariffRequestSettingMap();
        settingMap.setRequest(request);
        settingMap.setNameTag(tag);
        settingMap.setName(name);
        return settingMap;
    }

}
