package ru.yandex.market.mbo.util;

import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboVendors;

import java.util.ArrayList;
import java.util.List;

/**
 * @author commince
 * @date 08.05.2018
 */
public class ProtoUtils {
    private static final int LANG = 225;

    private ProtoUtils() {
    }

    public static MboParameters.Word word(String name) {
        return MboParameters.Word.newBuilder()
                .setLangId(LANG)
                .setName(name)
                .build();
    }

    public static List<MboParameters.Word> words(String... names) {
        List<MboParameters.Word> result = new ArrayList<>();
        for (String name : names) {
            result.add(word(name));
        }
        return result;
    }

    public static MboParameters.EnumAlias enumAlias(String alias) {
        return MboParameters.EnumAlias.newBuilder()
                .setAlias(word(alias))
                .setType(MboParameters.EnumAlias.Type.GENERAL)
                .build();
    }

    public static MboParameters.Option option(Long id, Long localVendorId, String name, String... aliases) {
        MboParameters.Option.Builder o = MboParameters.Option.newBuilder()
                .setId(id)
                .setActive(true)
                .addName(word(name));

        if (localVendorId != null) {
            o.setLocalVendorId(localVendorId);
        }

        for (String alias : aliases) {
            o.addAlias(enumAlias(alias));
        }

        return o.build();
    }

    public static MboParameters.Option option(Long id, String name, String... aliases) {
        return option(id, null, name, aliases);
    }

    public static MboVendors.GlobalVendor vendor(Long id, String name, String... aliases) {
        MboVendors.GlobalVendor.Builder v = MboVendors.GlobalVendor.newBuilder()
                .setId(id)
                .addName(word(name));

        for (String alias : aliases) {
            v.addAlias(word(alias));
        }

        return v.build();
    }
}
