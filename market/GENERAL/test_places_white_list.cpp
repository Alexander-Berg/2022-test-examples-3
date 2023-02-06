#include <market/report/library/cgi/params.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/common/env.h>

#include <util/generic/hash_set.h>
#include <util/generic/yexception.h>
#include <util/stream/file.h>

THashSet<TString> PlacesWhiteList() {
    TFsPath whiteList = TFsPath(ArcadiaSourceRoot()) / "market/report/places_white_list/places_white_list";
    THashSet<TString> ret;

    {
        TFileInput f(whiteList);
        TString place;
        while (f.ReadLine(place)) {
            if (place) {
                ret.insert(place);
            }
        }
    }

    return ret;
}

TEST(ReportPlaces, PlacesInWhiteList) {
    const auto& placesWhiteList = PlacesWhiteList();
    const auto& placesActual = NMarketReport::KnownPlaces();

    for (const auto& place : placesActual) {
        if (!placesWhiteList.contains(place)) {
            ythrow yexception() << "Place '" << place << "' is not in places white list!\n"
                                                << "See https://wiki.yandex-team.ru/market/report/reglament-rasshirenija/"
                                                << " and https://a.yandex-team.ru/arc_vcs/market/report/places_white_list"
                                                << " for details!";
        }
    }
}
