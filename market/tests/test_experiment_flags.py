# -*- coding: utf-8 -*-

import json
import os.path
import shutil
import unittest

from market.pylibrary.experiment_flags import (
    HostInfo,
    JsonKeys,
    ExperimentFlagsReader,
    EmergencyFlagsReader,
    _parse_bool_flag,
    _parse_int_flag,
    _parse_str_flag,
)

ROOTDIR = "tmp"
ITS_FILE_PATH = os.path.join(ROOTDIR, "its_file.json")
PV_FAKE_FILE_FOR_TEST = os.path.join(ROOTDIR, "fake_pv_file")


def _write_flags_to_file(flags):
    # type: (dict) -> None
    with open(ITS_FILE_PATH, "w") as f:
        f.write(json.dumps(flags))


def _makedirs(dirname):
    if not os.path.exists(dirname):
        os.makedirs(dirname)


def _touch(filepath):
    dirname = os.path.dirname(filepath)
    _makedirs(dirname)
    with open(filepath, 'w'):
        pass


class BlackTests(unittest.TestCase):

    def test_smoke(self):
        flag = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                        "or (IS_SAS and IS_PREP and IS_API) "
                                        "or (IS_VLA and IS_PREP and IS_INT)"
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="parallel", role="market-report",
                                                        loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                        loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report", loc="sas",
                                                        cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report", loc="vla",
                                                        cluster=0, host=0)))

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                         loc="sas", cluster=0, host=0)))

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="priemka", subrole="shadow", role="market-report",
                                                         loc="sas", cluster=0, host=0)))

    def test_int_flag_no_default(self):
        flag = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "42",
                    JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                        "or (IS_SAS and IS_PREP and IS_API) "
                                        "or (IS_VLA and IS_PREP and IS_INT)"
                }
            ]
        }

        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="parallel", role="market-report",
                                                            loc="man", cluster=0, host=0), -1))
        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                            loc="man", cluster=0, host=0), -1))
        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                            loc="sas", cluster=0, host=0), -1))
        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                            loc="vla", cluster=0, host=0), -1))

        self.assertEqual(-1, _parse_int_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                            loc="man", cluster=0, host=0), -1))
        self.assertEqual(-1, _parse_int_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                            loc="man", cluster=0, host=0), -1))
        self.assertEqual(None, _parse_int_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                              loc="sas", cluster=0, host=0), None))
        self.assertEqual(None, _parse_int_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                              loc="sas", cluster=0, host=0), None))

    def test_int_flag_default(self):
        flag = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "42",
                    JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                        "or (IS_SAS and IS_PREP and IS_API) "
                                        "or (IS_VLA and IS_PREP and IS_INT)"
                }
            ],
            JsonKeys.DEFAULT_VALUE: "35"
        }

        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="parallel", role="market-report",
                                                            loc="man", cluster=0, host=0)))
        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                            loc="man", cluster=0, host=0)))
        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                            loc="sas", cluster=0, host=0)))
        self.assertEqual(42, _parse_int_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                            loc="vla", cluster=0, host=0)))

        self.assertEqual(35, _parse_int_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                            loc="man", cluster=0, host=0)))
        self.assertEqual(35, _parse_int_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                            loc="man", cluster=0, host=0)))
        self.assertEqual(35, _parse_int_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                            loc="sas", cluster=0, host=0)))
        self.assertEqual(35, _parse_int_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                            loc="sas", cluster=0, host=0)))

    def test_str_flag(self):
        flag = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "IS_MAN and IS_PREP"
                }
            ]
        }
        self.assertEqual("1", _parse_str_flag(flag, HostInfo(
            env="prestable", subrole="parallel", role="market-report", loc="man", cluster=0, host=0)))

    def test_format_error(self):
        flag1 = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                        "or (IS_SAS and IS_PREP and IS_API) "
                                        "or (IS_VLA and IS_PREP and IS_INT"
                }
            ]
        }
        self.assertRaises(SyntaxError, lambda: _parse_bool_flag(flag1, HostInfo("prestable", "int", "market-report",
                                                                                "sas", 0, 0)))

        flag2 = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                        "or (IS_SAS and IS_PREP and IS_UNDEFINED_VARIABLE_HERE_) "
                                        "or (IS_VLA and IS_PREP and IS_INT)"
                }
            ]
        }
        self.assertRaises(NameError, lambda: _parse_bool_flag(flag2, HostInfo("prestable", "int", "market-report",
                                                                              "sas", 0, 0)))

        no_such_keyword_but_lazy_evaluation_so_no_exception = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " (IS_MAN and IS_PREP and (IS_MAIN or IS_NO_SUCH_KEYWORD)) "
                }
            ]
        }
        self.assertFalse(_parse_bool_flag(no_such_keyword_but_lazy_evaluation_so_no_exception,
                                          HostInfo("production", "int", "market-report", "man", 0, 0)))

    def test_default_value(self):
        flag = {
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "0",
                    JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                        "or (IS_SAS and IS_PREP and IS_API) "
                                        "or (IS_VLA and IS_PREP and IS_INT)"
                }
            ],
            JsonKeys.DEFAULT_VALUE: "1"
        }

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="parallel", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report",
                                                         loc="vla", cluster=0, host=0)))

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report", loc="man",
                                                        cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report", loc="man",
                                                        cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                        loc="sas", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="int", role="market-report", loc="sas",
                                                        cluster=0, host=0)))

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="priemka", subrole="shadow", role="market-report",
                                                        loc="sas", cluster=0, host=0)))

    def test_no_condition(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0"
        }

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="parallel", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="priemka", subrole="shadow", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="", subrole="", role="", loc="", cluster=0, host=0)))

    def test_complex_expressions(self):
        flags = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "( IS_PREP and ((IS_MAN and (IS_PARALLEL or IS_MAIN)) or "
                                        "  (IS_SAS and IS_API) or (IS_VLA and IS_INT)) ) or "
                                        "(IS_SHADOW and IS_SAS) or IS_MARKET_KRAKEN"
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="market", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="parallel", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="int", role="market-report",
                                                         loc="vla", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flags, HostInfo(env="priemka", subrole="shadow", role="market-report",
                                                         loc="sas", cluster=0, host=0)))

        self.assertFalse(_parse_bool_flag(flags, HostInfo(env="production", subrole="market", role="market-report",
                                                          loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="api", role="market-report",
                                                          loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="api", role="market-report",
                                                          loc="vla", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flags, HostInfo(env="prestable", subrole="api", role="market-report",
                                                          loc="vla", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flags, HostInfo(env="priemka", subrole="shadow", role="market-report",
                                                          loc="man", cluster=0, host=0)))

    def test_cluster(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "IS_API and IS_SAS and CLUSTER in [1, 2, 3]"
                }
            ]
        }

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report", loc="sas",
                                                        cluster=1, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                        loc="sas", cluster=2, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report", loc="sas",
                                                        cluster=3, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                         loc="sas", cluster=4, host=0)))

    def test_geo(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " IS_MAIN and (GEO == \"sas\" or GEO == \"vla\") "
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                        loc="sas", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="market", role="market-report",
                                                        loc="vla", cluster=1, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="market", role="market-report",
                                                         loc="man", cluster=2, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="market", role="market-report",
                                                         loc="iva", cluster=3, host=0)))

    def test_env(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " ENV in [ \"prestable\", \"testing\"] and IS_API and IS_SAS"
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report", loc="sas",
                                                        cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="testing", subrole="api", role="market-report", loc="sas",
                                                        cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="perf-testing", subrole="api", role="market-report",
                                                         loc="sas", cluster=0, host=0)))

    def test_subrole(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: "SUBROLE in [\"turbo\", \"api\", \"planeshift\", \"bk\"] and IS_MAN and IS_PROD"
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="turbo", role="market-report",
                                                        loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="api", role="market-report",
                                                        loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="planeshift", role="market-report",
                                                        loc="man", cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="bk", role="market-report", loc="man",
                                                        cluster=0, host=0)))

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="parallel", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="mbo", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="shadow", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="blue-shadow", role="market-report",
                                                         loc="man", cluster=0, host=0)))

    def test_role(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " IS_SHADOW and IS_SAS and ROLE == \"market-report\" "
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="shadow", role="market-report",
                                                        loc="sas", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="shadow",
                                                         role="market-snippet-report", loc="sas", cluster=0, host=0)))

    def test_lots_of_conditions(self):
        flag = {
            JsonKeys.DEFAULT_VALUE: "0",
            JsonKeys.CONDITIONS: [
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " IS_PREP and IS_API and IS_SAS "
                },
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " IS_PROD and IS_MAIN and IS_MAN and CLUSTER == 42"
                },
                {
                    JsonKeys.VALUE: "1",
                    JsonKeys.CONDITION: " IS_TEST and IS_INT and IS_VLA and CLUSTER in [0, 1]"
                }
            ]
        }

        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report", loc="sas",
                                                        cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="production", subrole="market", role="market-report",
                                                        loc="man", cluster=42, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="testing", subrole="int", role="market-report", loc="vla",
                                                        cluster=0, host=0)))
        self.assertTrue(_parse_bool_flag(flag, HostInfo(env="testing", subrole="int", role="market-report", loc="vla",
                                                        cluster=1, host=0)))

        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="prestable", subrole="api", role="market-report",
                                                         loc="man", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="production", subrole="market", role="market-report",
                                                         loc="man", cluster=43, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="perf-testing", subrole="int", role="market-report",
                                                         loc="vla", cluster=0, host=0)))
        self.assertFalse(_parse_bool_flag(flag, HostInfo(env="testing", subrole="int", role="market-report", loc="sas",
                                                         cluster=1, host=0)))


class PublicInterfaceTest(unittest.TestCase):
    def setUp(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)
        os.makedirs(ROOTDIR)

    def tearDown(self):
        shutil.rmtree(ROOTDIR, ignore_errors=True)

    def test_simple(self):
        flags = {
            JsonKeys.ENABLE_NOCACHE: {
                JsonKeys.CONDITIONS: [
                    {
                        JsonKeys.VALUE: "1",
                        JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                            "or (IS_SAS and IS_PREP and IS_API) "
                    }
                ]
            },
            JsonKeys.ENABLE_NEW_INDEX_UNPACKER: {
                JsonKeys.CONDITIONS: [
                    {
                        JsonKeys.VALUE: "1",
                        JsonKeys.CONDITION: "(IS_SAS and IS_PREP and IS_API) or (IS_SHADOW and IS_SAS)"
                    }
                ]
            },
            JsonKeys.SATA_WRITE_LIMIT: {
                JsonKeys.CONDITIONS: [
                    {
                        JsonKeys.VALUE: "42",
                        JsonKeys.CONDITION: "(IS_SAS and IS_PREP and IS_API) or (IS_SHADOW and IS_SAS)"
                    }
                ]
            },
            JsonKeys.META_ENABLE_CROSS_DC_BASE_SEARCH: {
                JsonKeys.CONDITIONS: [
                    {
                        JsonKeys.VALUE: "1",
                        JsonKeys.CONDITION: "(IS_SAS and IS_PREP and IS_API)"
                    }
                ]
            },
        }

        _write_flags_to_file(flags)

        parsed_flags1 = ExperimentFlagsReader(ITS_FILE_PATH, "prestable", "api", "market-report", "sas", 0, 0).read_flags()
        self.assertTrue(parsed_flags1 is not None)
        self.assertTrue(parsed_flags1.enable_nocache)
        self.assertTrue(parsed_flags1.enable_new_index_unpacker)
        self.assertTrue(parsed_flags1.enable_write_speed_limit)
        self.assertTrue(parsed_flags1.meta_enable_cross_dc_base_search)

        parsed_flags2 = ExperimentFlagsReader(ITS_FILE_PATH, "prestable", "parallel", "market-report", "man", 0, 0).read_flags()
        self.assertTrue(parsed_flags2 is not None)
        self.assertTrue(parsed_flags2.enable_nocache)
        self.assertFalse(parsed_flags2.enable_new_index_unpacker)
        self.assertFalse(parsed_flags2.enable_write_speed_limit)
        self.assertFalse(parsed_flags2.meta_enable_cross_dc_base_search)

        parsed_flags3 = ExperimentFlagsReader(ITS_FILE_PATH, "production", "shadow", "market-report", "sas", 0, 0).read_flags()
        self.assertTrue(parsed_flags3 is not None)
        self.assertFalse(parsed_flags3.enable_nocache)
        self.assertTrue(parsed_flags3.enable_new_index_unpacker)
        self.assertFalse(parsed_flags3.meta_enable_cross_dc_base_search)

        parsed_flags4 = ExperimentFlagsReader(ITS_FILE_PATH, "prestable", "int", "market-report", "man", 0, 0).read_flags()
        self.assertTrue(parsed_flags4 is not None)
        self.assertFalse(parsed_flags4.enable_nocache)
        self.assertFalse(parsed_flags4.enable_new_index_unpacker)
        self.assertFalse(parsed_flags4.enable_write_speed_limit)
        self.assertFalse(parsed_flags4.meta_enable_cross_dc_base_search)

    def test_not_all_flags_in_file(self):
        just_one_flag = {
            JsonKeys.ENABLE_NOCACHE: {
                JsonKeys.CONDITIONS: [
                    {
                        JsonKeys.VALUE: "1",
                        JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                            "or (IS_SAS and IS_PREP and IS_API) "
                    }
                ]
            }
        }
        _write_flags_to_file(just_one_flag)
        parsed_flags1 = ExperimentFlagsReader(ITS_FILE_PATH, "prestable", "api", "market-report", "sas", 0, 0).read_flags()
        self.assertTrue(parsed_flags1 is not None)
        self.assertTrue(parsed_flags1.enable_nocache)
        self.assertFalse(parsed_flags1.enable_new_index_unpacker)
        self.assertFalse(parsed_flags1.enable_write_speed_limit)

        no_flags = {}
        _write_flags_to_file(no_flags)
        parsed_flags2 = ExperimentFlagsReader(ITS_FILE_PATH, "prestable", "api", "market-report", "sas", 0, 0).read_flags()
        self.assertTrue(parsed_flags2 is not None)
        self.assertFalse(parsed_flags2.enable_nocache)
        self.assertFalse(parsed_flags2.enable_new_index_unpacker)
        self.assertFalse(parsed_flags2.enable_write_speed_limit)

    def test_emergency_flags(self):
        flags = {
            JsonKeys.METASEARCH_PARALLEL_PARSING: {
                JsonKeys.CONDITIONS: [
                    {
                        JsonKeys.VALUE: "1",
                        JsonKeys.CONDITION: "(IS_MAN and IS_PREP and (IS_MAIN or IS_PARALLEL)) "
                                            "or (IS_VLA and IS_PROD and IS_MAIN)"
                                            "or (IS_SAS and IS_TEST and IS_API) "
                    }
                ]
            },
        }

        _write_flags_to_file(flags)

        parsed_flags1 = EmergencyFlagsReader(ITS_FILE_PATH, "testing", "api", "market-report", "sas", 0, 0).read_flags()
        self.assertTrue(parsed_flags1 is not None)
        self.assertTrue(parsed_flags1.metasearch_parallel_parsing)

        parsed_flags2 = EmergencyFlagsReader(ITS_FILE_PATH, "prestable", "parallel", "market-report", "man", 0, 0).read_flags()
        self.assertTrue(parsed_flags2 is not None)
        self.assertTrue(parsed_flags2.metasearch_parallel_parsing)

        parsed_flags3 = EmergencyFlagsReader(ITS_FILE_PATH, "production", "market", "market-report", "vla", 0, 0).read_flags()
        self.assertTrue(parsed_flags3 is not None)
        self.assertTrue(parsed_flags3.metasearch_parallel_parsing)

        parsed_flags4 = EmergencyFlagsReader(ITS_FILE_PATH, "prestable", "int", "market-report", "man", 0, 0).read_flags()
        self.assertTrue(parsed_flags4 is not None)
        self.assertFalse(parsed_flags4.metasearch_parallel_parsing)

    def test_bad_flags_file(self):
        def test_flags(path):
            flags = ExperimentFlagsReader(path, "production", "market", "market-report", "vla", 0, 0).read_flags()
            self.assertTrue(flags is not None)
            self.assertTrue(flags.enable_nocache is not None)
            self.assertFalse(flags.enable_nocache)
            flags = EmergencyFlagsReader(path, "production", "market", "market-report", "vla", 0, 0).read_flags()
            self.assertTrue(flags is not None)
            self.assertTrue(flags.metasearch_parallel_parsing is not None)
            self.assertFalse(flags.metasearch_parallel_parsing)

        # flags not set
        test_flags(None)
        # absent file
        flags_file_path = os.path.join(ROOTDIR, 'flags')
        test_flags(flags_file_path)
        # empty file
        _touch(flags_file_path)
        test_flags(flags_file_path)
        # corrupt json
        with open(ITS_FILE_PATH, 'w') as f:
            f.write("{{}")
        test_flags(flags_file_path)


if __name__ == '__main__':
    unittest.main()
