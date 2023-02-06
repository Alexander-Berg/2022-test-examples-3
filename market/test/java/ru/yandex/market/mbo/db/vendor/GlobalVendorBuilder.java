package ru.yandex.market.mbo.db.vendor;

import ru.yandex.market.mbo.gwt.models.vendor.GlobalVendor;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * @author s-ermakov
 */
public class GlobalVendorBuilder {

    private long id;
    private String name;
    private List<String> aliases;
    private boolean published;
    private String pictureUrl;
    private boolean isFakeVendor;
    private boolean isRequireGtinBarcodes;

    private GlobalVendorBuilder() {
        aliases = new ArrayList<>();
    }

    public static GlobalVendorBuilder newBuilder() {
        return new GlobalVendorBuilder();
    }

    public static GlobalVendorBuilder newBuilder(long id, String name) {
        return newBuilder()
                .setId(id)
                .setName(name);
    }


    public GlobalVendorBuilder setId(long id) {
        this.id = id;
        return this;
    }

    public GlobalVendorBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public GlobalVendorBuilder addAlias(String alias) {
        aliases.add(alias);
        return this;
    }

    public GlobalVendorBuilder setPublished(boolean published) {
        this.published = published;
        return this;
    }

    public GlobalVendorBuilder setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
        return this;
    }

    public GlobalVendorBuilder setIsFakeVendor(boolean isFakeVendor) {
        this.isFakeVendor = isFakeVendor;
        return this;
    }

    public GlobalVendorBuilder setIsRequireGtinBarcodes(boolean isRequireGtinBarcodes) {
        this.isRequireGtinBarcodes = isRequireGtinBarcodes;
        return this;
    }

    public GlobalVendor build() {
        GlobalVendor globalVendor = new GlobalVendor();
        globalVendor.setId(id);
        globalVendor.setNames(WordUtil.defaultWords(name));
        globalVendor.setAliases(WordUtil.defaultWords(aliases));
        globalVendor.setPublished(published);
        globalVendor.setPictureUrl(pictureUrl);
        globalVendor.setFakeVendor(isFakeVendor);
        globalVendor.setRequireGtinBarcodes(isRequireGtinBarcodes);
        return globalVendor;
    }
}
