#!/usr/bin/python
# -*- coding: utf-8 -*-

import os
import unittest
import pytest

import market.pylibrary.yenv as yenv


CS_ELLIPTICS_SETTINGS = {
    yenv.TESTING: {
        'hostname': 'cs-elliptics01ft.yandex.ru',
        'port': 88,
    },
    yenv.PRODUCTION: {
        'hostname': 'cs-elliptics.yandex.ru',
        'port': 99,
    }
}


class TestYenv(unittest.TestCase):
    @staticmethod
    def _write_env_file(path, env_type):
        with open(path, 'w') as fn:
            fn.write(env_type)

    def test_resolve_settings(self):
        settings = yenv.resolve_settings(CS_ELLIPTICS_SETTINGS, yenv.TESTING)
        self.assertEqual(settings['hostname'], 'cs-elliptics01ft.yandex.ru')
        self.assertEqual(settings['port'], 88)

    def test_set_environment_type(self):
        yenv.set_environment_type(yenv.PRODUCTION)
        self.assertEqual(yenv.environment_type(), yenv.PRODUCTION)
        settings = yenv.resolve_settings(CS_ELLIPTICS_SETTINGS)
        self.assertEqual(settings['hostname'], 'cs-elliptics.yandex.ru')
        self.assertEqual(settings['port'], 99)

    def test_get_environment_type_from_file(self):
        env_filename = "env_filename"

        try:
            self._write_env_file(env_filename, yenv.TESTING)
            assert yenv._get_type(path=env_filename) == yenv.TESTING

            self._write_env_file(env_filename, "blablabla")
            assert yenv._get_type(path=env_filename) == yenv.PRODUCTION  # we got production for unkwnown env

        finally:
            if os.path.exists(env_filename):
                os.unlink(env_filename)

    def test_get_env_type_env_hardware(self):
        try:
            os.environ['ENV_TYPE'] = yenv.TESTING
            assert yenv._get_type("no_such_file") == yenv.TESTING

            os.environ['ENV_TYPE'] = yenv.PRODUCTION
            assert yenv._get_type("no_such_file") == yenv.PRODUCTION

        finally:
            os.environ.pop('ENV_TYPE', None)

    def test_get_env_type_env_rtc(self):
        try:
            os.environ['BSCONFIG_ITAGS'] = "a_shard_0 a_itype_marketreport a_line_iva-2 " \
                                           "cgset_memory_recharge_on_pgfault_1 a_tier_MarketMiniClusterTier0" \
                                           "IVA_MARKET_TEST_REPORT_GENERAL_MARKET itag_replica_7" \
                                           "a_geo_msk a_metaprj_market a_prj_report-general-market a_ctype_testing " \
                                           "a_dc_iva use_hq_spec enable_hq_report enable_hq_poll;BSCONFIG_SHARDDIR=./"

            assert yenv._get_type("no_such_file") == yenv.TESTING

            os.environ['BSCONFIG_ITAGS'] = "blablabla bld lba=bla"
            assert yenv._get_type("no_such_file") == yenv.PRODUCTION

        finally:
            os.environ.pop('BSCONFIG_ITAGS', None)


@pytest.mark.parametrize('stage_id, expected', [
    ('testing_market_datacamp-parser', yenv.TESTING),
    ('production_market_datacamp-parser', yenv.PRODUCTION),
    ('', yenv.PRODUCTION),
    ('strangeenv_market_datacamp-parser', yenv.PRODUCTION),
], ids=['testing', 'production', 'fallback', 'strange_env'])
def test_gen_env_type_deploy(stage_id, expected):
    try:
        os.environ['DEPLOY_STAGE_ID'] = stage_id
        assert yenv._get_type(None) == expected
    finally:
        os.environ.pop('DEPLOY_STAGE_ID', None)


@pytest.mark.parametrize('mitype, expected', [
    (yenv.GIBSON, True),
    (yenv.STRATOCASTER, True),
    (yenv.PLANESHIFT_GIBSON, False),
    (yenv.TURBO_GIBSON, False),
    (yenv.FRESH_GIBSON, False),
])
def test_is_main(mitype, expected):
    assert yenv.is_main(mitype) == expected


@pytest.mark.parametrize('mitype, expected', [
    (yenv.GIBSON, False),
    (yenv.PLANESHIFT_GIBSON, True),
    (yenv.PLANESHIFT_STRATOCASTER, True),
    (yenv.TURBO_GIBSON, False),
    (yenv.FRESH_GIBSON, False),
])
def test_is_planeshift(mitype, expected):
    assert yenv.is_planeshift(mitype) == expected


@pytest.mark.parametrize('mitype, expected', [
    (yenv.GIBSON, False),
    (yenv.PLANESHIFT_GIBSON, False),
    (yenv.TURBO_GIBSON, True),
    (yenv.TURBO_STRATOCASTER, True),
    (yenv.FRESH_GIBSON, False),
])
def test_is_turbo(mitype, expected):
    assert yenv.is_turbo(mitype) == expected


@pytest.mark.parametrize('mitype, expected', [
    (yenv.GIBSON, False),
    (yenv.PLANESHIFT_GIBSON, False),
    (yenv.TURBO_GIBSON, False),
    (yenv.FRESH_GIBSON, True),
    (yenv.FRESH_STRATOCASTER, True),
])
def test_is_fresh(mitype, expected):
    assert yenv.is_fresh(mitype) == expected


@pytest.mark.parametrize('mitype, expected', [
    (yenv.GIBSON, yenv.MAIN),
    (yenv.PLANESHIFT_GIBSON, yenv.PLANESHIFT),
    (yenv.TURBO_GIBSON, yenv.TURBO),
    (yenv.FRESH_GIBSON, yenv.FRESH),
    ('unknown', None)
])
def test_idx_type(mitype, expected):
    assert yenv.idx_type(mitype) == expected


if __name__ == '__main__':
    unittest.main()
