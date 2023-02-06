# -*- coding: utf-8 -*-


from yandex_iznanka_geo_json_parser import YandexIznankaGeoJSONParser
from test_utils import TestParser
from yandex_iznanka_geo_json_parser_test import TestYandexIznankaGeoJSONParser as CheckYandexIznankaGeoJSONParser
# If TestYandexIznankaGeoJSONParser were to be imported as TestYandexIznankaGeoJSONParser,
# pytest's discovery magic would run the TestYandexIznankaGeoJSONParser's test methods here as well (in addition
# to their normal launch). For one it would pollute the test output.


def test_get_parser_class_name():
    assert "Parser" == TestParser._get_parser_class_name()


def test_get_parser_test_module_name():
    assert "test_utils" == TestParser._get_parser_test_module_name()
    assert "yandex_iznanka_geo_json_parser_test" == CheckYandexIznankaGeoJSONParser._get_parser_test_module_name()


def test_get_parser_module_name():
    assert "yandex_iznanka_geo_json_parser" == CheckYandexIznankaGeoJSONParser._get_parser_module_name()


def test_get_parser_test_data_directory_name():
    assert "yandex_iznanka_geo_json_parser_data" == CheckYandexIznankaGeoJSONParser._get_parser_test_data_directory_name()


def test_get_parser_class():
    assert None is CheckYandexIznankaGeoJSONParser._parser_class
    assert YandexIznankaGeoJSONParser == CheckYandexIznankaGeoJSONParser._get_parser_class()
    assert YandexIznankaGeoJSONParser is CheckYandexIznankaGeoJSONParser._parser_class


def test_read_file():
    assert u"Шоколадные места" in CheckYandexIznankaGeoJSONParser._read_file("pushkin.json")
