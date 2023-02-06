package ru.yandex.market.mbo.synchronizer.export.navigation;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.mbo.core.navigation.NavigationNodeRedirect;
import ru.yandex.market.mbo.db.navigation.NavigationTreeNodeRedirectService;
import ru.yandex.market.mbo.db.navigation.NavigationTreeService;
import ru.yandex.market.mbo.db.navigation.stubs.NavigationTreeNodeRedirectServiceH2MemImpl;
import ru.yandex.market.mbo.gwt.models.navigation.NavigationTree;
import ru.yandex.market.mbo.gwt.models.visual.TreeNode;
import ru.yandex.market.mbo.synchronizer.export.BaseExtractor;
import ru.yandex.market.mbo.synchronizer.export.ExtractorBaseTestClass;
import ru.yandex.market.mbo.synchronizer.export.ExtractorWriterService;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author york
 * @since 26.09.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class NavigationRedirectsExtractorTest extends ExtractorBaseTestClass {

    private NavigationRedirectsExtractor extractor;

    @Before
    @Override
    public void setUp() throws Exception {
        extractor = new NavigationRedirectsExtractor();
        NavigationTreeNodeRedirectService service = new NavigationTreeNodeRedirectServiceH2MemImpl();
        service.setNavigationTreeService(Mockito.mock(NavigationTreeService.class));
        service.addRedirect(new NavigationNodeRedirect("blue", 1L, 2L));
        service.addRedirect(new NavigationNodeRedirect("green", 3L, 4L));
        service.addRedirect(new NavigationNodeRedirect("green", 1L, 3L));
        NavigationTree blueTree = new NavigationTree();
        blueTree.setCode("blue");
        blueTree.setRoot(new TreeNode<>());

        NavigationTree greenTree = new NavigationTree();
        greenTree.setCode("green");
        greenTree.setRoot(new TreeNode<>());

        NavigationTreeProvider provider = Mockito.mock(NavigationTreeProvider.class);
        when(provider.getNavigationNodeRedirects(any()))
            .thenReturn(service.getCollapsedRedirects(Arrays.asList(greenTree, blueTree)));
        extractor.setNavigationTreeProvider(provider);
        extractor.setWhiteInsteadOfBlue(false);
        ExtractorWriterService extractorWriterService = new ExtractorWriterService();
        extractor.setExtractorWriterService(extractorWriterService);
        super.setUp();
    }

    @Override
    protected BaseExtractor createExtractor() {
        return extractor;
    }

    @Test
    public void testExtract() {
        String resultXml = new String(performAndGetExtractContent(), StandardCharsets.UTF_8);
        System.out.println(resultXml);
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation-redirects>\n" +
            " <redirects tree-code=\"blue\">\n" +
            "  <redirect from=\"1\" to=\"2\"/>\n" +
            " </redirects>\n" +
            " <redirects tree-code=\"green\">\n" +
            "  <redirect from=\"1\" to=\"4\"/>\n" +
            "  <redirect from=\"3\" to=\"4\"/>\n" +
            " </redirects>\n" +
            "</navigation-redirects>\n", resultXml);
    }

    @Test
    public void testWhiteInsteadOfBlueFlag() {
        extractor.setWhiteInsteadOfBlue(true);
        String resultXml = new String(performAndGetExtractContent(), StandardCharsets.UTF_8);
        String whiteRedirects = "  <redirect from=\"1\" to=\"4\"/>\n"
            + "  <redirect from=\"3\" to=\"4\"/>\n";
        String blueRedirects = "  <redirect from=\"1\" to=\"2\"/>\n";
        // Check that white (green) redirects are under blue tree code
        Assert.assertEquals("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
            "<navigation-redirects>\n" +
            " <redirects tree-code=\"blue\">\n" +
                blueRedirects +
                // Takes also white
                whiteRedirects +
            " </redirects>\n" +
            " <redirects tree-code=\"green\">\n" +
                whiteRedirects +
                // Takes also blue
                blueRedirects +
            " </redirects>\n" +
            "</navigation-redirects>\n", resultXml);
    }
}
