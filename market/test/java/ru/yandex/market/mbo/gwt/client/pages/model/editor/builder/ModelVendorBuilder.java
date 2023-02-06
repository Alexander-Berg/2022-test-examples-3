package ru.yandex.market.mbo.gwt.client.pages.model.editor.builder;

import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.models.vendor.VendorSourceInfo;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author gilmulla
 */
public class ModelVendorBuilder {

    ModelDataBuilder parent;
    GlobalVendor vendor = new GlobalVendor();
    List<VendorSourceInfo> sources = new ArrayList<>();

    ModelVendorBuilder(ModelDataBuilder parent) {
        this.parent = parent;
    }

    public ModelVendorBuilder site(String url) {
        vendor.setSite(url);
        return this;
    }

    public ModelVendorBuilder modificationDate(Date modificationDate) {
        vendor.setModificationDate(modificationDate);
        return this;
    }

    public ModelVendorBuilder source(String url, String lang, Date date) {
        VendorSourceInfo inf = new VendorSourceInfo();
        inf.setUrl(url);
        inf.setLang(lang);
        inf.setModifiedDate(date);
        sources.add(inf);
        return this;
    }

    public ModelDataBuilder endVendor() {
        parent.modelData.setVendor(vendor);
        parent.modelData.setVendorSources(sources);
        return parent;
    }

}
