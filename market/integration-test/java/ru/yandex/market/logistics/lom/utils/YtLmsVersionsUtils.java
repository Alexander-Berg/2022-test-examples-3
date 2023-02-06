package ru.yandex.market.logistics.lom.utils;

import javax.annotation.ParametersAreNonnullByDefault;

import lombok.experimental.UtilityClass;

import ru.yandex.inside.yt.kosher.tables.YtTables;
import ru.yandex.market.logistics.lom.configuration.properties.LmsYtProperties;

@UtilityClass
@ParametersAreNonnullByDefault
@SuppressWarnings("HideUtilityClassConstructor")
public class YtLmsVersionsUtils {
    public void verifyYtVersionTableInteractions(
        YtTables ytTables,
        LmsYtProperties lmsYtProperties,
        int times
    ) {
        YtUtils.verifySelectRowsInteractions(
            ytTables,
            String.format("version FROM [%s] ORDER BY created_at DESC LIMIT 1", lmsYtProperties.getVersionPath()),
            times
        );
    }

    public void verifyYtVersionTableInteractions(
        YtTables ytTables,
        LmsYtProperties lmsYtProperties
    ) {
        verifyYtVersionTableInteractions(ytTables, lmsYtProperties, 1);
    }

    public void mockYtVersionTable(
        YtTables ytTables,
        LmsYtProperties lmsYtProperties,
        String version
    ) {
        YtUtils.mockSelectRowsFromYt(
            ytTables,
            version,
            String.format("version FROM [%s] ORDER BY created_at DESC LIMIT 1", lmsYtProperties.getVersionPath())
        );
    }
}
