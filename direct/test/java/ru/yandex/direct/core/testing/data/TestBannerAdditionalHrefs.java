package ru.yandex.direct.core.testing.data;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.banner.model.BannerAdditionalHref;

import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@ParametersAreNonnullByDefault
public class TestBannerAdditionalHrefs {

    public static final String HREF_1 = "http://www.ya.ru/additional_href_1";
    public static final String HREF_2 = "http://www.ya.ru/additional_href_2";
    public static final String HREF_3 = "http://www.ya.ru/additional_href_3";
    public static final String HREF_4 = "http://www.ya.ru/additional_href_4";

    public static List<BannerAdditionalHref> toNewBannerAdditionalHrefs(List<String> additionalHrefs) {
        return mapList(additionalHrefs, additionalHref -> new BannerAdditionalHref().withHref(additionalHref));
    }
}
