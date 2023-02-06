package ru.yandex.market.ir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

import ru.yandex.market.ir.http.ReportAliases;

public class ReportAliasesGenerator {

    private static final String PATH = "/home/marbok/arc/arcadia/market/ir/formalizer/src/test/resources" +
            "/reportAliases.pb";

    private ReportAliasesGenerator() {
    }

    public static void main(String[] args) throws IOException {
        final ReportAliases.ReportAliasesValues values = ReportAliases.ReportAliasesValues.newBuilder()
                .addValues(
                        createValue(4922804, 16826520,
                                createAlias("без кнопочное"),
                                createAlias("касательное", "A"),
                                createAlias("трогательное", "Б")))
                .build();
        values.writeTo(new FileOutputStream(PATH));
    }

    private static ReportAliases.Value createValue(long paramId, long valueId, ReportAliases.Alias... aliases) {
        return ReportAliases.Value.newBuilder()
                .setParamId(paramId)
                .setValueId(valueId)
                .addAllAliases(Arrays.asList(aliases))
                .build();
    }


    public static ReportAliases.Alias createAlias(String word) {
        return createAlias(word, null);
    }

    public static ReportAliases.Alias createAlias(String word, String layer) {
        ReportAliases.Alias.Builder builder = ReportAliases.Alias.newBuilder()
                .setWord(word);
        if (layer != null) {
            builder.setLayer(layer);
        }
        return builder.build();
    }
}
