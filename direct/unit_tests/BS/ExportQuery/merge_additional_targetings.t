#!/usr/bin/perl

use Direct::Modern;

use Yandex::Test::UTF8Builder;
use Test::More;
use Test::Deep;

use utf8;

use Yandex::DBUnitTest qw/init_test_dataset/;

use BS::ExportQuery;
use Settings;

my %db = (
    bad_domains_titles => {
        original_db => PPCDICT,
        rows => [],
    },
    products => {
        original_db => PPCDICT,
        rows => [],
    },
    crypta_goals => {
        original_db => PPCDICT,
        rows => [],
    },
);

init_test_dataset(\%db);

# [$test_name, $global_variables, $context, $targeting, $expected_context]
my @tests = (
    [
        '1. Заполненный CONTEXT + без TargetingExpression',
        {},
        {
            SomeKey => "somevalue"
        },
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[123]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]]]),
            SomeKey => "somevalue"
        }

    ],
    [
        '2. Пустой CONTEXT',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[123]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]]]),
        }

    ],
    [
        '3. CONTEXT с пустым TargetingExpression',
        {},
        {
            TargetingExpression => BS::ExportQuery::_nosoap([])
        },
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[123]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]]]),
        }

    ],
    [
        '4. yandexuids, одно значение, filtering',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[123]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "not like", "123"]]]),
        }

    ],
    [
        '5. yandexuids, несколько значений, filtering, any',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[123, 456]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "not like", "123"], ["uniq-id", "not like", "456"]]]),
        }

    ],
    [
        '6. yandexuids, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => "[123, 456]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "not like", "123"]], [["uniq-id", "not like", "456"]]]),
        }

    ],
    [
        '7. yandexuids, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[123, 456]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"], ["uniq-id", "like", "456"]]]),
        }

    ],
    [
        '8. yandexuids, несколько значений, targeting, all',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "all",
            value => "[123, 456]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]], [["uniq-id", "like", "456"]]]),
        }

    ],
    [
        '9. device_names, targeting',
        {},
        {},
        {
            targeting_type => "device_names",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uatraits-device-name", "icase match", "name"]]]),
        }

    ],
    [
        '10. device_names, filtering',
        {},
        {},
        {
            targeting_type => "device_names",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uatraits-device-name", "icase not match", "name"]]]),
        }

    ],
    [
        '11. interface_langs, targeting',
        {},
        {},
        {
            targeting_type => "interface_langs",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["lang", "icase match", "name"]]]),
        }

    ],
    [
        '12. interface_langs, filtering',
        {},
        {},
        {
            targeting_type => "interface_langs",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["lang", "icase not match", "name"]]]),
        }

    ],
    [
        '13. desktop_installed_apps, targeting',
        {},
        {},
        {
            targeting_type => "desktop_installed_apps",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["installed-yasoft", "equal", "name"]]]),
        }

    ],
    [
        '14. desktop_installed_apps, filtering',
        {},
        {},
        {
            targeting_type => "desktop_installed_apps",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["installed-yasoft", "not equal", "name"]]]),
        }

    ],
    [
        '15. query_referers, targeting',
        {},
        {},
        {
            targeting_type => "query_referers",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["referer", "like", "name"]]]),
        }

    ],
    [
        '16. query_referers, filtering',
        {},
        {},
        {
            targeting_type => "query_referers",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["referer", "not like", "name"]]]),
        }

    ],
    [
        '17. user_agent, targeting',
        {},
        {},
        {
            targeting_type => "user_agent",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["user-agent", "like", "name"]]]),
        }

    ],
    [
        '18. user_agent, filtering',
        {},
        {},
        {
            targeting_type => "user_agent",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["user-agent", "not like", "name"]]]),
        }

    ],
    [
        '19. show_dates, targeting',
        {},
        {},
        {
            targeting_type => "show_dates",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["timestamp", "like", "name"]]]),
        }

    ],
    [
        '20. show_dates, filtering',
        {},
        {},
        {
            targeting_type => "show_dates",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["timestamp", "not like", "name"]]]),
        }

    ],
    [
        '21. query_options, targeting',
        {},
        {},
        {
            targeting_type => "query_options",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["options", "icase match", "name"]]]),
        }

    ],
    [
        '22. query_options, filtering',
        {},
        {},
        {
            targeting_type => "query_options",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["options", "icase not match", "name"]]]),
        }

    ],
    [
        '23. clids, targeting',
        {},
        {},
        {
            targeting_type => "clids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["stat-id", "equal", "name"]]]),
        }

    ],
    [
        '24. clids, filtering',
        {},
        {},
        {
            targeting_type => "clids",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["stat-id", "not equal", "name"]]]),
        }

    ],
    [
        '25. clid_types, targeting',
        {},
        {},
        {
            targeting_type => "clid_types",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["clid-type", "match", "name"]]]),
        }

    ],
    [
        '26. clid_types, filtering',
        {},
        {},
        {
            targeting_type => "clid_types",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["clid-type", "not match", "name"]]]),
        }

    ],
    [
        '27. test_ids, targeting',
        {},
        {},
        {
            targeting_type => "test_ids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["test-ids", "equal", "name"]]]),
        }

    ],
    [
        '28. test_ids, filtering',
        {},
        {},
        {
            targeting_type => "test_ids",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["test-ids", "not equal", "name"]]]),
        }

    ],
    [
        '29. ys_cookies, targeting',
        {},
        {},
        {
            targeting_type => "ys_cookies",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-ys", "like", "name"]]]),
        }

    ],
    [
        '30. ys_cookies, filtering',
        {},
        {},
        {
            targeting_type => "ys_cookies",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-ys", "not like", "name"]]]),
        }

    ],
    [
        '31. yandexuids, в CONTEXT уже есть таргетинги, all',
        {},
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-ys", "not like", "name"]]]),
        },
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "all",
            value => "[123, 456]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-ys", "not like", "name"]], [["uniq-id", "like", "123"]], [["uniq-id", "like", "456"]]]),
        }

    ],
    [
        '32. yandexuids, в CONTEXT уже есть таргетинги, any',
        {},
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-ys", "not like", "name"]]]),
        },
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[123, 456]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-ys", "not like", "name"]], [["uniq-id", "like", "123"], ["uniq-id", "like", "456"]]]),
        }

    ],
    [
        '33. Отсутствует сортировка внешнего массива. Сортируется позже',
        {},
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]], [["uniq-id", "like", "456"]]]),
        },
        {
            targeting_type => "ys_cookies",
            targeting_mode => "filtering",
            value_join_type => "any",
            value => "[\"name\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]], [["uniq-id", "like", "456"]], [["cookie-ys", "not like", "name"]]]),
        }

    ],
    [
        '34. сортировка внутреннего массива, any',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[456, 123]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"], ["uniq-id", "like", "456"]]]),
        }

    ],
    [
        '35. сортировка внутреннего массива, all',
        {},
        {},
        {
            targeting_type => "yandexuids",
            targeting_mode => "targeting",
            value_join_type => "all",
            value => "[456, 123]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["uniq-id", "like", "123"]], [["uniq-id", "like", "456"]]]),
        }
    ],
    [
        '36. internal_network, targeting',
        {},
        {},
        {
            targeting_type => "internal_network",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["network-id", "equal", "2"]]]),
        }

    ],
    [
        '37. internal_network, filtering',
        {},
        {},
        {
            targeting_type => "internal_network",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["network-id", "not equal", "2"]]]),
        }

    ],
    [
        '38. is_mobile, targeting',
        {},
        {},
        {
            targeting_type => "is_mobile",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-mobile", "equal", "1"]]]),
        }

    ],
    [
        '39. is_mobile, filtering',
        {},
        {},
        {
            targeting_type => "is_mobile",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-mobile", "equal", "0"]]]),
        }

    ],
    [
        '40. has_l_cookie, targeting',
        {},
        {},
        {
            targeting_type => "has_l_cookie",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-l", "not equal", "0"]]]),
        }

    ],
    [
        '41. has_l_cookie, filtering',
        {},
        {},
        {
            targeting_type => "has_l_cookie",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["cookie-l", "equal", "0"]]]),
        }

    ],
    [
        '42. has_passport_id, targeting',
        {},
        {},
        {
            targeting_type => "has_passport_id",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["passport-id", "greater", "0"]]]),
        }

    ],
    [
        '43. has_passport_id, filtering',
        {},
        {},
        {
            targeting_type => "has_passport_id",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["passport-id", "equal", "0"]]]),
        }

    ],
    [
        '44. is_pp_logged_in, targeting',
        {},
        {},
        {
            targeting_type => "is_pp_logged_in",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["authorized", "equal", "1"]]]),
        }

    ],
    [
        '45. is_pp_logged_in, filtering',
        {},
        {},
        {
            targeting_type => "is_pp_logged_in",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["authorized", "equal", "0"]]]),
        }

    ],
    [
        '46. is_tablet, targeting',
        {},
        {},
        {
            targeting_type => "is_tablet",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-tablet", "equal", "1"]]]),
        }

    ],
    [
        '47. is_tablet, filtering',
        {},
        {},
        {
            targeting_type => "is_tablet",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-tablet", "equal", "0"]]]),
        }

    ],
    [
        '48. is_touch, targeting',
        {},
        {},
        {
            targeting_type => "is_touch",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-touch", "equal", "1"]]]),
        }

    ],
    [
        '49. is_touch, filtering',
        {},
        {},
        {
            targeting_type => "is_touch",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-touch", "equal", "0"]]]),
        }

    ],
    [
        '50. is_virused, targeting',
        {},
        {},
        {
            targeting_type => "is_virused",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["virus-mark", "equal", "1"]]]),
        }

    ],
    [
        '51. is_virused, filtering',
        {},
        {},
        {
            targeting_type => "is_virused",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["virus-mark", "equal", "0"]]]),
        }

    ],
    [
        '52. is_yandex_plus, targeting',
        {},
        {},
        {
            targeting_type => "is_yandex_plus",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["yandex-plus-enabled", "equal", "1"]]]),
        }

    ],
    [
        '53. is_yandex_plus, filtering',
        {},
        {},
        {
            targeting_type => "is_yandex_plus",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["yandex-plus-enabled", "equal", "0"]]]),
        }

    ],
    [
        '54. is_default_yandex_search, targeting',
        {},
        {},
        {
            targeting_type => "is_default_yandex_search",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["search-antitargeting", "equal", "1"]]]),
        }

    ],
    [
        '55. is_default_yandex_search, filtering',
        {},
        {},
        {
            targeting_type => "is_default_yandex_search",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["search-antitargeting", "equal", "0"]]]),
        }

    ],
    [
        '56. is_tv, targeting',
        {},
        {},
        {
            targeting_type => "is_tv",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-tv", "equal", "1"]]]),
        }

    ],
    [
        '57. is_tv, filtering',
        {},
        {},
        {
            targeting_type => "is_tv",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => undef
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["device-is-tv", "equal", "0"]]]),
        }

    ],
    [
        '58. sids, targeting',
        {},
        {},
        {
            targeting_type => "sids",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[55, 47]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["sids", "equal", "47"], ["sids", "equal", "55"]]]),
        }

    ],
    [
        '59. sids, filtering',
        {},
        {},
        {
            targeting_type => "sids",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => "[55, 47]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["sids", "not equal", "47"]], [["sids", "not equal", "55"]]]),
        }
    ],
    [
        '60. uuid, targeting',
        {},
        {},
        {
            targeting_type => "uuid",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"bb91c6e3d1bcb785bc8ffab48bed03e5\", \"c4e6538d6081e38922f2b971b6a69f29\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["uuid", "icase match", "bb91c6e3d1bcb785bc8ffab48bed03e5"], ["uuid", "icase match", "c4e6538d6081e38922f2b971b6a69f29"]]
            ]),
        }
    ],
    [
        '61. uuid, filtering',
        {},
        {},
        {
            targeting_type => "uuid",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => "[\"bb91c6e3d1bcb785bc8ffab48bed03e5\", \"c4e6538d6081e38922f2b971b6a69f29\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["uuid", "icase not match", "bb91c6e3d1bcb785bc8ffab48bed03e5"]],
                [["uuid", "icase not match", "c4e6538d6081e38922f2b971b6a69f29"]]
            ]),
        }
    ],
    [
        '62. device_id, targeting',
        {},
        {},
        {
            targeting_type => "device_id",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"1A987627-2F60-4AC2-9061-06DCFA0E42AC\", \"c85406d70b8553116de4a8ade48b04fd\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["device-id", "icase match", "1A987627-2F60-4AC2-9061-06DCFA0E42AC"],
                    ["device-id", "icase match", "c85406d70b8553116de4a8ade48b04fd"]]
            ]),
        }

    ],
    [
        '63. device_id, filtering',
        {},
        {},
        {
            targeting_type => "device_id",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => "[\"1A987627-2F60-4AC2-9061-06DCFA0E42AC\", \"c85406d70b8553116de4a8ade48b04fd\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["device-id", "icase not match", "1A987627-2F60-4AC2-9061-06DCFA0E42AC"]],
                [["device-id", "icase not match", "c85406d70b8553116de4a8ade48b04fd"]]
            ]),
        }
    ],
    [
        '64. plus_user_segments, targeting',
        {},
        {},
        {
            targeting_type => "plus_user_segments",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[2, 3]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["plus-user-segments", "equal", "2"], ["plus-user-segments", "equal", "3"]]
            ]),
        }

    ],
    [
        '65. plus_user_segments, filtering',
        {},
        {},
        {
            targeting_type => "plus_user_segments",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => "[2, 3]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["plus-user-segments", "not equal", "2"]],
                [["plus-user-segments", "not equal", "3"]]
            ]),
        }
    ],
    [
        '66. search_text, targeting',
        {},
        {},
        {
            targeting_type => "search_text",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => "[\"коронавирус\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["search-text", "icase match", "коронавирус"]]]),
        }

    ],
    [
        '67. search_text, filtering',
        {},
        {},
        {
            targeting_type => "search_text",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => "[\"коронавирус\"]"
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([[["search-text", "icase not match", "коронавирус"]]]),
        }
    ],
    [
        '68. browser_names, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "browser_names",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"maxVersion": 3000, "minVersion": 1000, "targetingValueEntryId": 1}, {"maxVersion": 7000, "minVersion": 5000, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["browser-name-and-version", "not match name and version range", "1:1000:3000"]],
                [["browser-name-and-version", "not match name and version range", "2:5000:7000"]],
            ]),
        }
    ],
    [
        '69. browser_names, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "browser_names",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"maxVersion": 4321, "minVersion": 1234, "targetingValueEntryId": 1}, {"maxVersion": 7000, "minVersion": 5000, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "browser-name-and-version", "match name and version range", "1:1234:4321" ],
                    [ "browser-name-and-version", "match name and version range", "2:5000:7000" ],
                ]
            ]),
        }
    ],
    [
        '70. browser_names, несколько значений, версии не везде, targeting, any',
        {},
        {},
        {
            targeting_type => "browser_names",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"maxVersion": 3000, "minVersion": null, "targetingValueEntryId": 1}, {"maxVersion": null, "minVersion": 5000, "targetingValueEntryId": 2}, {"maxVersion": null, "minVersion": null, "targetingValueEntryId": 3}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "browser-name-and-version", "match name and version range", "1::3000" ],
                    [ "browser-name-and-version", "match name and version range", "2:5000:" ],
                    [ "browser-name-and-version", "match name and version range", "3::" ],
                ]
            ]),
        }
    ],
    [
        '71. browser_names, несколько значений, filtering, all + targeting',
        {},
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "browser-name-and-version", "match name and version range", "3:1234:4321" ],
                    [ "browser-name-and-version", "match name and version range", "4:5000:7000" ],
                ]
            ])
        },
        {
            targeting_type => "browser_names",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"maxVersion": 3000, "minVersion": 1000, "targetingValueEntryId": 1}, {"maxVersion": 4321, "minVersion": 1, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    ["browser-name-and-version", "match name and version range", "3:1234:4321"],
                    ["browser-name-and-version", "match name and version range", "4:5000:7000"],
                ],
                [["browser-name-and-version", "not match name and version range", "1:1000:3000"]],
                [["browser-name-and-version", "not match name and version range", "2:1:4321"]],
            ]),
        }
    ],
    [
        '72. browser_engines, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "browser_engines",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"maxVersion": 3000, "minVersion": 1000, "targetingValueEntryId": 1}, {"maxVersion": 7000, "minVersion": 5000, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["browser-engine-name-and-version", "not match name and version range", "1:1000:3000"]],
                [["browser-engine-name-and-version", "not match name and version range", "2:5000:7000"]],
            ]),
        }
    ],
    [
        '73. browser_engines, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "browser_engines",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"maxVersion": 4321, "minVersion": 1234, "targetingValueEntryId": 1}, {"maxVersion": 7000, "minVersion": 5000, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "browser-engine-name-and-version", "match name and version range", "1:1234:4321" ],
                    [ "browser-engine-name-and-version", "match name and version range", "2:5000:7000" ],
                ]
            ]),
        }
    ],
    [
        '74. browser_engines, несколько значений, версии не везде, targeting, any',
        {},
        {},
        {
            targeting_type => "browser_engines",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"maxVersion": 3000, "minVersion": null, "targetingValueEntryId": 1}, {"maxVersion": null, "minVersion": 5000, "targetingValueEntryId": 2}, {"maxVersion": null, "minVersion": null, "targetingValueEntryId": 3}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "browser-engine-name-and-version", "match name and version range", "1::3000" ],
                    [ "browser-engine-name-and-version", "match name and version range", "2:5000:" ],
                    [ "browser-engine-name-and-version", "match name and version range", "3::" ],
                ]
            ]),
        }
    ],
    [
        '75. os_families, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "os_families",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"maxVersion": 3000, "minVersion": 1000, "targetingValueEntryId": 1}, {"maxVersion": 7000, "minVersion": 5000, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [["os-family-and-version", "not match name and version range", "1:1000:3000"]],
                [["os-family-and-version", "not match name and version range", "2:5000:7000"]],
            ]),
        }
    ],
    [
        '76. os_families, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "os_families",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"maxVersion": 4321, "minVersion": 1234, "targetingValueEntryId": 1}, {"maxVersion": 7000, "minVersion": 5000, "targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "os-family-and-version", "match name and version range", "1:1234:4321" ],
                    [ "os-family-and-version", "match name and version range", "2:5000:7000" ],
                ]
            ]),
        }
    ],
    [
        '77. os_families, несколько значений, версии не везде, targeting, any',
        {},
        {},
        {
            targeting_type => "os_families",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"maxVersion": 3000, "minVersion": null, "targetingValueEntryId": 1}, {"maxVersion": null, "minVersion": 5000, "targetingValueEntryId": 2}, {"maxVersion": null, "minVersion": null, "targetingValueEntryId": 3}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "os-family-and-version", "match name and version range", "1::3000" ],
                    [ "os-family-and-version", "match name and version range", "2:5000:" ],
                    [ "os-family-and-version", "match name and version range", "3::" ],
                ]
            ]),
        }
    ],
    [
        '78. os_names, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "os_names",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"targetingValueEntryId": 1}, {"targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [[ "os-name", "not equal", "1" ]],
                [[ "os-name", "not equal", "2" ]],
            ]),
        }
    ],
    [
        '79. os_names, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "os_names",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"targetingValueEntryId": 11}, {"targetingValueEntryId": 22}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "os-name", "equal", "11" ],
                    [ "os-name", "equal", "22" ],
                ]
            ]),
        }
    ],
    [
        '80. device_vendors, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "device_vendors",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"targetingValueEntryId": 1}, {"targetingValueEntryId": 2}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [[ "device-vendor", "not equal", "1" ]],
                [[ "device-vendor", "not equal", "2" ]],
            ]),
        }
    ],
    [
        '81. device_vendors, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "device_vendors",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"targetingValueEntryId": 11}, {"targetingValueEntryId": 22}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "device-vendor", "equal", "11" ],
                    [ "device-vendor", "equal", "22" ],
                ]
            ]),
        }
    ],
    [
        '82. yp_cookies, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "yp_cookies",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '["AAAAA", "BBBBB"]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [[ "cookie-yp", "not like", "AAAAA" ]],
                [[ "cookie-yp", "not like", "BBBBB" ]],
            ]),
        }
    ],
    [
        '83. yp_cookies, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "yp_cookies",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '["abc", "sakfjASFjksal"]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "cookie-yp", "like", "abc" ],
                    [ "cookie-yp", "like", "sakfjASFjksal" ],
                ]
            ]),
        }
    ],
    [
        '84. mobile_installed_apps, несколько значений, filtering, all',
        {
            mobile_content => {
                329701 => {
                    os_type          => "Android",
                    store_content_id => "com.rovio.angrybirds",
                    bundle_id        => "",
                },
                329711 => {
                    os_type          => "iOS",
                    store_content_id => "id1346190318",
                    bundle_id        => "com.omnigroup.OmniFocus3.iOS",
                }
            }
        },
        {},
        {
            targeting_type => "mobile_installed_apps",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '[{"storeUrl": "https://apps.apple.com/ru/app/omnifocus-3/id1346190318", "mobileContentId": 329711}, {"storeUrl": "https://play.google.com/store/apps/details?id=com.rovio.angrybirds&hl=ru", "mobileContentId": "329701"}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [[ "except-apps-on-cpi", "not equal", "1792509008" ]],
                [[ "except-apps-on-cpi", "not equal", "3941433497" ]],
            ]),
        }
    ],
    [
        '85. mobile_installed_apps, несколько значений, targeting, any',
        {
            mobile_content => {
                329701 => {
                    os_type          => "Android",
                    store_content_id => "com.rovio.angrybirds",
                    bundle_id        => "",
                },
                329711 => {
                    os_type          => "iOS",
                    store_content_id => "id1346190318",
                    bundle_id        => "com.omnigroup.OmniFocus3.iOS",
                }
            }
        },
        {},
        {
            targeting_type => "mobile_installed_apps",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"storeUrl": "https://play.google.com/store/apps/details?id=com.rovio.angrybirds&hl=ru", "mobileContentId": 329701}, {"storeUrl": "https://apps.apple.com/ru/app/omnifocus-3/id1346190318", "mobileContentId": "329711"}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "except-apps-on-cpi", "equal", "1792509008" ],
                    [ "except-apps-on-cpi", "equal", "3941433497" ],
                ]
            ]),
        }
    ],
    [
        '86. mobile_installed_apps, пустой store_content_id и bundle_id',
        {
            mobile_content => {
                329701 => {
                    os_type          => "Android",
                    store_content_id => "",
                    bundle_id        => undef,
                },
                329711 => {
                    os_type          => "iOS",
                    store_content_id => "",
                    bundle_id        => undef,
                }
            }
        },
        {},
        {
            targeting_type => "mobile_installed_apps",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '[{"storeUrl": "https://play.google.com/store/apps/details?id=com.rovio.angrybirds&hl=ru", "mobileContentId": 329701}, {"storeUrl": "https://apps.apple.com/ru/app/omnifocus-3/id1346190318", "mobileContentId": "329711"}]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "except-apps-on-cpi", "equal", "2428552969" ],
                    [ "except-apps-on-cpi", "equal", "3892132942" ],
                ]
            ]),
        }
    ],
    [
        '87. features_in_pp, несколько значений, filtering, all',
        {},
        {},
        {
            targeting_type => "features_in_pp",
            targeting_mode => "filtering",
            value_join_type => "all",
            value => '["AAAAA", "BBBBB"]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [[ "enabled_features", "icase not match", "AAAAA" ]],
                [[ "enabled_features", "icase not match", "BBBBB" ]],
            ]),
        }
    ],
    [
        '88. features_in_pp, несколько значений, targeting, any',
        {},
        {},
        {
            targeting_type => "features_in_pp",
            targeting_mode => "targeting",
            value_join_type => "any",
            value => '["abc", "sakfjASFjksal"]'
        },
        {
            TargetingExpression => BS::ExportQuery::_nosoap([
                [
                    [ "enabled_features", "icase match", "abc" ],
                    [ "enabled_features", "icase match", "sakfjASFjksal" ],
                ]
            ]),
        }
    ],
);

Test::More::plan(tests => scalar(@tests));

foreach my $test (@tests) {
    my ($test_name, $global_variables, $context, $targeting, $expected_context) = @$test;
    BS::ExportQuery::set_global_variables($global_variables);
    BS::ExportQuery::_merge_additional_targetings($context, $targeting);
    cmp_deeply($context, $expected_context, $test_name);
}
