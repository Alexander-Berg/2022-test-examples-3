package ru.yandex.chemodan.app.docviewer.web.framework;

import org.junit.Test;

import ru.yandex.chemodan.app.docviewer.web.backend.HtmlWithImagesPageInfoAction;
import ru.yandex.chemodan.app.docviewer.web.backend.PageRequest;
import ru.yandex.chemodan.app.docviewer.web.backend.StartAction;
import ru.yandex.chemodan.app.docviewer.web.backend.StartRequest;
import ru.yandex.misc.test.Assert;

/**
 * Test cases for {@link AbstractActionServlet}, but "Abstract" prefix were
 * removed to ensure it is called from build.xml
 *
 * @author vlsergey
 */
public class ActionServletTest {

    @Test
    public void testGetRequestClass_Complex() {
        Assert.A.equals(PageRequest.class, new HtmlWithImagesPageInfoAction().getRequestClass());
    }

    @Test
    public void testGetRequestClass_Simple() {
        Assert.A.equals(StartRequest.class, new StartAction().getRequestClass());
    }

    @Test
    public void testInit_Complex() {
        new HtmlWithImagesPageInfoAction().initRequestConstructor();
    }

    @Test
    public void testInit_Simple() {
        new StartAction().initRequestConstructor();
    }
}
