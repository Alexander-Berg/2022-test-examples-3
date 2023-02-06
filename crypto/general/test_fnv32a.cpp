#include <crypta/ext_fp/matcher/lib/matchers/rostelecom_matcher/mc_domain/fnv32a.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <util/generic/vector.h>

using namespace NCrypta::NExtFp::NMatcher;

TEST(NMcDomain, Fnv32a) {
    const TVector<std::tuple<TString, ui32>> testCases = {
        {"", 0x811c9dc5},
        // https://a.yandex-team.ru/arc/trunk/arcadia/metrika/frontend/watch/test/e2e/proxy/rostelecom/rostelecom.spec.js?rev=r7899911#L49
        // https://a.yandex-team.ru/arc/trunk/arcadia/metrika/frontend/watch/test/e2e/proxy/rostelecom/rostelecom.hbs?rev=r7899911#L12-13
        {"11111", 2724587256},
        // https://a.yandex-team.ru/arc/trunk/arcadia/metrika/frontend/watch/src/utils/fnv32a/__tests__/fnv32a.spec.ts?rev=r7178391
        {"www.googletagservices.com/tag/js/gpt.js", 1882689622},
        {"www.googleadservices.com/pagead/conversion.js", 2318205080},
        {"www.googletagmanager.com/gtm.js", 3115871109},
        {"yastatic.net/metrika-static-watch/watch.js", 3604875100},
        {"cdn.jsdelivr.net/npm/yandex-metrica-watch/tag.js", 339366994},
        {"cdn.jsdelivr.net/npm/yandex-metrica-watch/tag_ua.js", 2890452365},
        {"cdn.jsdelivr.net/npm/yandex-metrica-watch/watch.js", 849340123},
        {"cdn.jsdelivr.net/npm/yandex-metrica-watch/watch_ua.js", 173872646},
        {"mc.yandex.ru/metrika/watch.js", 2343947156},
        {"mc.yandex.ru/metrika/tag.js", 655012937},
        {"mc.yandex.ru/metrika/tag_turbo.js", 3724710748},
        {"mc.yandex.ru/metrika/tag_jet_beta.js", 3364370932},
        {"stats.g.doubleclick.net/dc.js", 1996539654},
        {"www.google-analytics.com/ga.js", 2065498185},
        {"ssl.google-analytics.com/ga.js", 823651274},
        {"www.google-analytics.com/analytics.js", 12282461},
        {"ssl.google-analytics.com/analytics.js", 1555719328},
        {"counter.yadro.ru/hit", 1417229093},
        {"an.yandex.ru/system/context.js", 138396985},
    };

    for (const auto& [value, refHash] : testCases) {
        EXPECT_EQ(refHash, NMcDomain::Fnv32a(value));
    }
}
