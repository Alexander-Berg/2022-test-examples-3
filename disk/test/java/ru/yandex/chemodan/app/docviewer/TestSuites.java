package ru.yandex.chemodan.app.docviewer;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestSuites {

    public static Collection<URL> MICROSOFT_WORD_97;

    public static Collection<Object[]> PDF_ARRAY;
    public static Map<URL, String> PDF_MAP;

    public static Collection<Object[]> TEXTS_ARRAY;
    public static Map<URL, String> TEXTS_MAP;

    static {
        List<URL> word97 = new ArrayList<>();
        word97.add(TestResources.Microsoft_Word_97_001p);
        word97.add(TestResources.Microsoft_Word_97_002p);
        word97.add(TestResources.Microsoft_Word_97_025p);
        word97.add(TestResources.Microsoft_Word_97_046p);
        word97.add(TestResources.Microsoft_Word_97_050p);
        word97.add(TestResources.Microsoft_Word_97_102p);
        MICROSOFT_WORD_97 = word97;
    }

    static {
        Map<URL, String> texts = new LinkedHashMap<>();
        texts.put(TestResources.Adobe_Acrobat_1_4_001p, "6108283e-a09e-4f78-84cf-c06e6193264a");
        texts.put(TestResources.Microsoft_Excel_97_001p, "e1092474-736b-430d-bbf4-2e923d0928cb");
        texts.put(TestResources.Microsoft_RTF, "e8bd2361-27f9-48af-aec5-b0a88205b544");
        texts.put(TestResources.Microsoft_Word_12_001p, "67a43feb-1c53-4b31-887b-c7f0471708d8");
        texts.put(TestResources.Microsoft_Word_97_001p, "3db63cb0-92ee-4853-9f7d-1832a0da125d");
        TEXTS_MAP = Collections.unmodifiableMap(new LinkedHashMap<>(texts));
        TEXTS_ARRAY = toData(texts);
    }

    static {
        Map<URL, String> pdfs = new LinkedHashMap<>();
        pdfs.put(TestResources.Adobe_Acrobat_1_3_001p, "unknown");
        pdfs.put(TestResources.Adobe_Acrobat_1_3_001p_2columns, "unknown");
        pdfs.put(TestResources.Adobe_Acrobat_1_4_001p, "6108283e-a09e-4f78-84cf-c06e6193264a");
        pdfs.put(TestResources.Adobe_Acrobat_1_4_001p_limited, "unknown");
        pdfs.put(TestResources.Adobe_Acrobat_1_4_004p, "unknown");
        pdfs.put(TestResources.Adobe_Acrobat_1_5_114p, "unknown");
        PDF_MAP = Collections.unmodifiableMap(new LinkedHashMap<>(pdfs));
        PDF_ARRAY = toData(pdfs);
    }

    private static Collection<Object[]> toData(Map<?, ?> map) {
        List<Object[]> result = new ArrayList<>(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.add(new Object[] { entry.getKey(), entry.getValue() });
        }
        return Collections.unmodifiableList(result);
    }
}
