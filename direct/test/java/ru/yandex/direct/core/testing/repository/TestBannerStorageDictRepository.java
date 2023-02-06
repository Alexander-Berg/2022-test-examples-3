package ru.yandex.direct.core.testing.repository;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.creative.model.BannerStorageDictLayoutItem;
import ru.yandex.direct.core.entity.creative.model.BannerStorageDictThemeItem;
import ru.yandex.direct.dbschema.ppcdict.enums.BannerStorageDictType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.utils.JsonUtils;

import static ru.yandex.direct.dbschema.ppcdict.tables.BannerStorageDict.BANNER_STORAGE_DICT;

public class TestBannerStorageDictRepository {
    @Autowired
    private DslContextProvider dslContextProvider;

    /**
     * Добавляет запись о layout'е в словарь {@code BANNER_STORAGE_DICT}.
     */
    public void addLayout(BannerStorageDictLayoutItem layout) {
        String jsonLayout = JsonUtils.toJson(layout);
        dslContextProvider.ppcdict()
                .insertInto(BANNER_STORAGE_DICT,
                        BANNER_STORAGE_DICT.ID,
                        BANNER_STORAGE_DICT.TYPE,
                        BANNER_STORAGE_DICT.JSON_CONTENT)
                .values(
                        layout.getId(),
                        BannerStorageDictType.layout,
                        jsonLayout
                )
                .onDuplicateKeyUpdate()
                .set(BANNER_STORAGE_DICT.JSON_CONTENT, jsonLayout)
                .execute();
    }

    /**
     * Добавляет запись о теме в словарь {@code BANNER_STORAGE_DICT}.
     */
    public void addTheme(BannerStorageDictThemeItem theme) {
        String jsonTheme = JsonUtils.toJson(theme);
        dslContextProvider.ppcdict()
                .insertInto(BANNER_STORAGE_DICT,
                        BANNER_STORAGE_DICT.ID,
                        BANNER_STORAGE_DICT.TYPE,
                        BANNER_STORAGE_DICT.JSON_CONTENT)
                .values(
                        theme.getId(),
                        BannerStorageDictType.theme,
                        jsonTheme
                )
                .onDuplicateKeyUpdate()
                .set(BANNER_STORAGE_DICT.JSON_CONTENT, jsonTheme)
                .execute();
    }

}
