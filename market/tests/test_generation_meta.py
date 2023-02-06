# -*- coding: utf-8 -*-
import pytest

from hamcrest import assert_that, equal_to, contains, has_properties

from async_publishing.generation_meta import (
    GenerationMeta,
    PackageMeta,
    DistGenerationMeta,
)


def test_parse_full_generation():
    meta = GenerationMeta.from_str('''{
      "name": "20180101_1010",
      "torrent_server_host": "mi01h.market.yandex.net",
      "torrent_server_port": 80
    }''')

    expected_url = 'http://mi01h.market.yandex.net:80/torrent-server/torrents/search-stats-20180101_1010.torrent'
    skynet_url = 'http://mi01h.market.yandex.net/marketindexer/dist_info.py?generation=20180101_1010&dist_name=search-stats'
    assert_that(
        (
            meta,
            meta.dist_url('search-stats'),
            meta.dist_url('search-stats', make_skynet_http_url=True),
            meta.dist_url('search-stats', 'sas')
        ),
        contains(
            has_properties({'name': '20180101_1010', '_not_for_publishing': False, 'reload_phase': None}),
            equal_to(expected_url),
            equal_to(skynet_url),
            equal_to(expected_url)
        )
    )


def test_parse_full_generation_with_reload_phase():
    meta = GenerationMeta.from_str('''{
      "name": "20180101_1010",
      "torrent_server_host": "mi01h.market.yandex.net",
      "torrent_server_port": 80,
      "not_for_publishing": true,
      "override_skynet_http_url": true,
      "reload_phase": 2
    }''')

    assert_that(meta, has_properties({'name': '20180101_1010', '_not_for_publishing': True, '_override_skynet_http_url': True, 'reload_phase': 2}))


def test_parse_full_generation_with_dcs():
    meta = GenerationMeta.from_str('''{
      "name": "20180101_1010",
      "torrent_server_host": "mi01h.market.yandex.net",
      "torrent_server_port": 80,
      "available_datacenters": ["sas"]
    }''')

    expected_unknown_dc_url = 'http://mi01h.market.yandex.net:80/torrent-server/torrents/search-stats-20180101_1010.torrent'
    expected_sas_url = 'http://mi01h.market.yandex.net:80/torrent-server/torrents/search-stats-20180101_1010-sas.torrent'

    assert_that(
        (
            meta.name,
            meta.dist_url('search-stats'),
            meta.dist_url('search-stats', 'man'),
            meta.dist_url('search-stats', 'sas')
        ),
        contains(
            equal_to('20180101_1010'),
            equal_to(expected_unknown_dc_url),
            equal_to(expected_unknown_dc_url),
            equal_to(expected_sas_url)
        )
    )


def test_serialize_full_generation():
    meta = GenerationMeta(
        "20180101_1010"
    )
    assert_that(meta, equal_to(GenerationMeta.from_str(str(meta))))


def test_parse_package():
    meta = PackageMeta.from_str('''{
          "name": "report",
          "torrent_server_host": "mi01h.market.yandex.net",
          "torrent_server_port": 80,
          "version": "2018.03.24.1",
          "rbtorrent":  "rbtorrent:cbe5556e235e898ebb2b1e521910c6514e7ab8a5"
        }''')

    assert_that(meta, has_properties({'name': "report",
                                      'version': "2018.03.24.1",
                                      'rbtorrent': "rbtorrent:cbe5556e235e898ebb2b1e521910c6514e7ab8a5"}))


def test_serialize_package():
    meta = PackageMeta(
        name="report",
        version="2018.04.24.1",
        torrent_server_host='somehost.market.yandex.net',
        torrent_server_port=80,
        rbtorrent="rbtorrent:cbe5556e235e898ebb2b1e521910c6514e7ab8a5"
    )

    assert_that(meta, equal_to(PackageMeta.from_str(str(meta))))


def test_parse_dist_generation():
    meta = DistGenerationMeta.from_str('''{
      "name": "20180101_1010",
      "dist_name": "search-part-1",
      "service_name": "marketsearch3",
      "torrent_server_host": "mi01h.market.yandex.net",
      "torrent_server_port": 80
    }''')

    expected_unknown_dc_url = 'http://mi01h.market.yandex.net:80/torrent-server/torrents/search-part-1-20180101_1010.torrent'

    assert_that(
        (
            meta,
            meta.has_torrent(),
            meta.has_skynet(),
            meta.dist_url(),
            meta.dist_url('man'),
            meta.dist_url('sas')
        ),
        contains(
            has_properties({'generation': '20180101_1010', 'dist_name': 'search-part-1', 'service_name': 'marketsearch3'}),
            equal_to(True),
            equal_to(False),
            equal_to(expected_unknown_dc_url),
            equal_to(expected_unknown_dc_url),
            equal_to(expected_unknown_dc_url)
        )
    )


def test_parse_dist_generation_with_dcs():
    meta = DistGenerationMeta.from_str('''{
      "name": "20180101_1010",
      "dist_name": "search-part-1",
      "service_name": "marketsearch3",
      "torrent_server_host": "mi01h.market.yandex.net",
      "torrent_server_port": 80,
      "available_datacenters": ["iva", "sas"]
    }''')

    expected_unknown_dc_url = 'http://mi01h.market.yandex.net:80/torrent-server/torrents/search-part-1-20180101_1010.torrent'
    expected_sas_url = 'http://mi01h.market.yandex.net:80/torrent-server/torrents/search-part-1-20180101_1010-sas.torrent'

    assert_that(
        (
            meta,
            meta.has_torrent(),
            meta.has_skynet(),
            meta.dist_url(),
            meta.dist_url('man'),
            meta.dist_url('sas')
        ),
        contains(
            has_properties({'generation': '20180101_1010', 'dist_name': 'search-part-1', 'service_name': 'marketsearch3'}),
            equal_to(True),
            equal_to(False),
            equal_to(expected_unknown_dc_url),
            equal_to(expected_unknown_dc_url),
            equal_to(expected_sas_url)
        )
    )


def test_parse_dist_with_skynet():
    meta = DistGenerationMeta.from_str('''{
      "name": "20180101_1010",
      "dist_name": "search-part-1",
      "service_name": "marketsearch3",
      "torrent_server_host": "mi01h.market.yandex.net",
      "torrent_server_port": 80,
      "rbtorrent": "rbtorrent:whatever"
    }''')

    assert_that(
        (
            meta,
            meta.has_torrent(),
            meta.has_skynet()
        ),
        contains(
            has_properties({"generation": "20180101_1010", "dist_name": "search-part-1", "service_name": "marketsearch3"}),
            equal_to(True),
            equal_to(True)
        ))


def test_compare_equal_full_generations():
    meta1 = GenerationMeta("20180101_1010")
    meta2 = GenerationMeta("20180101_1010")

    assert meta1 == meta2
    assert not meta1 != meta2


@pytest.mark.parametrize("a,b", [
    (GenerationMeta("20180101_1010"), GenerationMeta("20180101_1210")),
    (GenerationMeta("20180101_1010", not_for_publishing=True), GenerationMeta("20180101_1010")),
    (GenerationMeta("20180101_1010", not_for_publishing=False), GenerationMeta("20180101_1010", not_for_publishing=False, reload_phase=1)),
    (GenerationMeta("20180101_1010", reload_phase=2), GenerationMeta("20180101_1010",  reload_phase=1)),
    (GenerationMeta("20180101_1010", reload_phase=2), GenerationMeta("20180101_1010",  reload_phase=None)),
])
def test_compare_non_equal_full_generations(a, b):
    assert a != b
    assert not a == b
