package ru.yandex.market.mbo.export;

import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.common.util.collections.MultiMap;
import ru.yandex.market.mbo.gwt.models.gurulight.Tag;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.ConfidentFormalization;
import ru.yandex.market.mbo.gwt.models.params.FormalizationScope;
import ru.yandex.market.mbo.gwt.models.params.Measure;
import ru.yandex.market.mbo.gwt.models.params.Param;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.FormalizerClient;
import ru.yandex.market.mbo.gwt.models.visual.VisualCategory;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VisualCategoryForFormalizerExporterTest {

    @Mock
    private Writer ww;

    @Mock
    private VisualCategory vc;

    @Mock
    private VisualCategoryForFormalizerExtendedInfo extendedInfo;

    private VisualCategoryForFormalizerExporter visualCategoryForFormalizerExporter;

    private XmlStringWriter xmlWriter;

    private Map<Long, Measure> measuresMap;

    private Map<Integer, Tag> formalizerTagMap;

    private Set<Long> formalizedOptions;

    private String result = "<formalizer> <rule class=\"ru.yandex.market.ir.decision.BooleanRule\"" +
        " type=\"boolean\"/>\n" +
        " <rule class=\"ru.yandex.market.ir.decision.StringDictionaryRule\" type=\"stringDictionary\"/>\n" +
        " <rule class=\"ru.yandex.market.ir.decision.NumberRule\" type=\"number\"/>\n" +
        " <rule class=\"ru.yandex.market.ir.decision.NumberRule\" type=\"numberDictionary\"/>\n" +
        " <tags/>\n" +
        " <type baseType=\"stringDictionary\" id=\"0\" name=\"0:enum:testName\">\n" +
        "  <param name=\"formalize_from_parameters\" value=\"true\"/>\n" +
        "  <param name=\"formalize_from_title\" value=\"true\"/>\n" +
        "  <param name=\"confident_in_parameters\" value=\"true\"/>\n" +
        "  <param name=\"confident_in_title\" value=\"true\"/>\n" +
        "  <param name=\"do_not_formalize_patterns\" value=\"true\"/>\n" +
        "</type>\n" +
        " <category id=\"0\" name=\"\" use_title_maker=\"false\">\n" +
        "  <client value=\"REPORT\"/>\n" +
        "  <param id=\"0\" name=\"\" xslname=\"testName\" type=\"0:enum:testName\"" +
        " level=\"model\" required_for_index=\"false\" notify_stores=\"false\"" +
        " published=\"true\" model_filter_index=\"1\"/>\n" +
        " </category>\n" +
        " <links/>\n" +
        " <conflict-rules/>\n" +
        "</formalizer>\n";

    @Before
    public void setUp() throws Exception {
        measuresMap = new HashMap<>();
        formalizedOptions = new HashSet<>();
        formalizerTagMap = new HashMap<>();
        when(vc.getFormalizerClients()).thenReturn(Collections.singletonList(FormalizerClient.REPORT));
        visualCategoryForFormalizerExporter = new VisualCategoryForFormalizerExporter(
            ww, vc, measuresMap, formalizerTagMap, extendedInfo, formalizedOptions);
        xmlWriter = new XmlStringWriter();
        FieldSetter.setField(visualCategoryForFormalizerExporter,
            visualCategoryForFormalizerExporter.getClass().getDeclaredField("w"),
            xmlWriter);
    }

    @Test
    public void testExport() throws IOException {
        CategoryParam categoryParam = new Parameter();
        categoryParam.setFormalizationScope(FormalizationScope.YML_AND_TITLE);
        categoryParam.setDoNotFormalizePatterns(true);
        categoryParam.setDontUseAsAlias(true);
        categoryParam.setType(Param.Type.ENUM);
        categoryParam.setXslName("testName");
        categoryParam.setPublished(true);
        categoryParam.setModelFilterIndex(1);
        categoryParam.setConfidentFormalization(ConfidentFormalization.YML_AND_TITLE);

        when(extendedInfo.getParameters()).thenReturn(Collections.singleton(categoryParam));
        when(extendedInfo.getGeneratedTagsMap()).thenReturn(new MultiMap<>());

        visualCategoryForFormalizerExporter.export();

        assertEquals(result, xmlWriter.getStringXml());
    }
}
