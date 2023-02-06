package ru.yandex.chemodan.app.djfs.core.legacy.formatting.office.discovery;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class XmlDiscoveryParsingTest {

    private String xml;

    @Before
    public void init() throws IOException {
        xml = IOUtils.toString(XmlDiscoveryParsingTest.class.getResourceAsStream("discovery-microsoft.xml"), StandardCharsets.UTF_8);
    }

    @Test
    public void testParsing() {
        XmlWopiDiscovery discovery = XmlWopiDiscovery.parser.parseXml(xml);
        assertNotNull(discovery);
        assertNotNull(discovery.getNetZones());
        assertFalse(discovery.getNetZones().isEmpty());
    }


    @Test
    public void testMapping() {
        XmlWopiDiscovery discovery = XmlWopiDiscovery.parser.parseXml(xml);
        ListF<MicrosoftApp> apps = MicrosoftApp.fromXml(discovery, Cf.list("Word", "Excel", "PowerPoint", "WopiTest"),
                Cf.list("view", "edit", "editnew", "getinfo"));
        assertNotNull(apps);
        assertFalse(apps.isEmpty());
        assertFalse(apps.map(MicrosoftApp::getApp).containsTs("OneNote"));
        assertFalse(apps.flatMap(MicrosoftApp::getActions).map(Action::getAction).containsTs("embedview"));
        assertTrue(apps.map(MicrosoftApp::getApp).containsTs("Word"));
        assertTrue(apps.flatMap(MicrosoftApp::getActions).map(Action::getAction).containsTs("edit"));
    }

}
