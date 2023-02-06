package ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.models.DbCreative;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.models.ParameterValue;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.services.CreativeService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.infrastructure.RtbClientService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.infrastructure.RtbIntegrationHealthService;
import ru.yandex.bannerstorage.harvester.queues.rtbintegration.infrastructure.impl.BsApiClientService;
import ru.yandex.direct.bs.dspcreative.service.DspCreativeYtExporter;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.yandex.bannerstorage.harvester.queues.rtbintegration.creativechanged.CreativeChangedQueueObserver.SMART_FORMAT_NAME;

public class CreativeChangedQueueObserverTest {
    @Mock
    CreativeService creativeService;
    @Mock
    RtbClientService rtbClientService;
    @Mock
    RtbIntegrationHealthService healthService;
    @Mock
    BsApiClientService bsApiClientService;
    @Mock
    DspCreativeYtExporter dspCreativeYtExporter;

    @InjectMocks
    CreativeChangedQueueObserver queueObserver;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testAwapsToRtbMacroHack() {
        String data = "%AWAPS_AD_CLICK_URL% blah-blah %AWAPS_AD_EVENT_URL(59, 1)% %AWAPS_AD_EVENT_URL(52, 1)% %AWAPS_AD_EVENT_URL(1, 2)%";
        String actual = CreativeChangedQueueObserver.replaceAwapsMacroWithRtbAnalog(data);
        assertEquals("${VAST_CLICK_URL} blah-blah  ${TRACKING_URL_PREFIX}?action-id=0 ${VAST_CLICK_URL}", actual);
    }

    @Test
    public void testSmartFormatName_NoParameter() {
        DbCreative dbCreative = new DbCreative();
        dbCreative.setCodeId(811);
        assertEquals("smart-banner_theme_mosaic", queueObserver.getSmartFormatName(dbCreative, emptyMap()));
    }

    @Test
    public void testSmartFormatName_NoParameterSmartTGO() {
        DbCreative dbCreative = new DbCreative();
        dbCreative.setCodeId(882);  // один из кодов для смарт-ТГО
        assertNull(queueObserver.getSmartFormatName(dbCreative, emptyMap()));
    }

    @Test
    public void testSmartFormatName_HasParameter() {
        DbCreative dbCreative = new DbCreative();
        dbCreative.setCodeId(811);
        Map<String, ParameterValue> parameterValues = singletonMap(
                SMART_FORMAT_NAME,
                new ParameterValue(SMART_FORMAT_NAME, singletonList("someFormatName"), 1, "string")
        );
        assertEquals("someFormatName", queueObserver.getSmartFormatName(dbCreative, parameterValues));
    }

    @Test
    public void testSmartFormatName_HasParameterEmptyString() {
        DbCreative dbCreative = new DbCreative();
        dbCreative.setCodeId(811);
        Map<String, ParameterValue> parameterValues = singletonMap(
                SMART_FORMAT_NAME,
                new ParameterValue(SMART_FORMAT_NAME, singletonList(""), 1, "string")
        );
        assertNull(queueObserver.getSmartFormatName(dbCreative, parameterValues));
    }

    @Test(expected = RuntimeException.class)
    public void testSmartFormatName_NoParameterUnknownCode() {
        DbCreative dbCreative = new DbCreative();
        dbCreative.setCodeId(9999);  // неизвестный код
        queueObserver.getSmartFormatName(dbCreative, emptyMap());
    }
}
