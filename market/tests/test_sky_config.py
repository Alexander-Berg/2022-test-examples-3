# -*- coding: utf-8 -*-
from async_publishing.sky_config import SkyConfig


def test_set_functions():
    sky_config = SkyConfig.create_default()
    sky_config.set_global_get_dists(['search-part-base-0', 'search-part-base-1'])
    sky_config.set_group_config('report_market@atlantis', {1: ['search-part-base-1', 'search-part-base-2']})

    assert (
        sky_config.can_sky_get('report_market@fake', '1', 'search-part-base-0') and
        sky_config.can_sky_get('report_market@fake', 1, 'search-part-base-0') and
        sky_config.can_sky_get('report_market@atlantis', '1', 'search-part-base-2')
    )


def test_parse_sky_config():
    sky_config = SkyConfig.from_str('''{
        "global_get_dists": ["search-part-base-0", "search-part-base-1"],
        "sky_group_configs": {"report_market@atlantis": {"1": ["search-part-base-1", "search-part-base-2"]}}
    }''')

    assert (
        sky_config.can_sky_get('report_market@fake', '1', 'search-part-base-0') and
        sky_config.can_sky_get('report_market@atlantis', '1', 'search-part-base-2')
    )


def test_sky_get_permissions():
    sky_config = SkyConfig.create_default()
    sky_config.set_global_get_dists(['search-part-base-0'])
    sky_config.set_group_config('report_market@atlantis', {'*': ['search-part-base-1']})

    assert (
        # 'search-part-base-0' может скачать любая группа хостов, 'search-part-base-1' только 'report_market@atlantis'
        sky_config.can_sky_get('report_market@atlantis', '12', 'search-part-base-1') and
        sky_config.can_sky_get('report_market@atlantis', '12', 'search-part-base-0') and
        sky_config.can_sky_get('report_market@atlantis', 12, 'search-part-base-0') and
        not sky_config.can_sky_get('report_market@fake', '13', 'search-part-base-1') and
        not sky_config.can_sky_get('report_market@fake', 13, 'search-part-base-1')
    )


def test_serialization_to_str():
    sky_config = SkyConfig.create_default()
    sky_config.set_global_get_dists(['search-part-base-2'])
    sky_config.set_group_config('report_market@atlantis', {1: ['search-part-base-1', 'search-part-base-2']})

    expected_config = '{"global_get_dists": ["search-part-base-2"], "sky_group_configs": {"report_market@atlantis": {"1": ["search-part-base-1", "search-part-base-2"]}}}'

    assert (
        str(sky_config) == expected_config
    )
