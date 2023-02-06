package ru.yandex.market.mbo.cms.core.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CmsHostLinksHelperTest {
    @Test
    public void makeUrlTest() {
        Map<String, Set<String>> exportParams;
        String docType = "";
        String urlPrefix = "/{brand_id}/{semantic_id}";

        exportParams = ImmutableMap.<String, Set<String>>builder().
                put("semantic_id", Set.of("semantic")).
                put("brand_id", Set.of("brand")).
                build();
        assertEquals("/brand/semantic",
            CmsHostLinksHelper.makeUrl(docType, exportParams, List.of("semantic_id", "brand_id"), urlPrefix));

        exportParams = ImmutableMap.<String, Set<String>>builder().
                put("brand_id", Set.of("brand")).
                build();
        assertEquals("/brand",
            CmsHostLinksHelper.makeUrl(docType, exportParams, List.of("brand_id"), urlPrefix));

        exportParams = ImmutableMap.<String, Set<String>>builder().
                put("semantic_id", Set.of("semantic")).
                build();
        assertEquals("/semantic",
            CmsHostLinksHelper.makeUrl(docType, exportParams, List.of("semantic_id"), urlPrefix));

        exportParams = ImmutableMap.<String, Set<String>>builder().
                build();
        assertEquals(null,
            CmsHostLinksHelper.makeUrl(docType, exportParams, Collections.emptyList(), urlPrefix));
    }
}
