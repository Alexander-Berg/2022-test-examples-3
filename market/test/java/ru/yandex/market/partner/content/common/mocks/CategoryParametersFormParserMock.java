package ru.yandex.market.partner.content.common.mocks;

import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.XslInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CategoryParametersFormParserMock implements CategoryParametersFormParser {
    private final Map<Long, Map<String, XslInfo>> attrsByHid = new HashMap<>();
    private Map<String, XslInfo> defaultAttrs = Collections.emptyMap();

    @Override
    public Map<String, XslInfo> getCategoryAttr(long hid) {
        return attrsByHid.getOrDefault(hid, defaultAttrs);
    }

    public void addCategoryAttrs(long hid, Map<String, XslInfo> attrs) {
        attrsByHid.put(hid, attrs);
    }

    public void setDefaultAttrs(Map<String, XslInfo> defaultAttrs) {
        this.defaultAttrs = defaultAttrs;
    }
}
